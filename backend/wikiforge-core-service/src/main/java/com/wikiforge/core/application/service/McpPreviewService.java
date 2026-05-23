package com.wikiforge.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.common.filesystem.PathSafety;
import com.wikiforge.core.application.dto.McpToolCallPageResponse;
import com.wikiforge.core.application.dto.McpToolCallResponse;
import com.wikiforge.core.application.dto.McpToolDefinition;
import com.wikiforge.core.application.dto.McpToolListResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class McpPreviewService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_MCP_CALL_PAGE_SIZE = 100;
    private static final Set<String> SOURCE_STATUSES = Set.of(
            "pending",
            "organized",
            "processing",
            "pending_review",
            "archived",
            "rejected",
            "failed"
    );
    private static final Set<String> SOURCE_TYPES = Set.of("text", "note", "link", "manual");
    private static final Set<String> PROCESSING_INTENTS = Set.of("organize_only", "extract_and_review");
    private static final Set<String> RECORD_TYPES = Set.of("expense", "bill", "email", "relationship", "event", "note");
    private static final Set<String> SENSITIVITY_LEVELS = Set.of("low", "medium", "high");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final CoreRuntimeProperties runtimeProperties;
    private final List<McpToolDefinition> tools;

    public McpPreviewService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            CoreRuntimeProperties runtimeProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.runtimeProperties = runtimeProperties;
        this.tools = List.of(
                tool(
                        "search_sources",
                        "Search organized WikiForge sources without exposing local filesystem paths.",
                        true,
                        searchSourcesInputSchema()
                ),
                tool(
                        "get_source",
                        "Get a sanitized WikiForge source summary by sourceUid or fileUid.",
                        true,
                        getSourceInputSchema()
                ),
                tool(
                        "create_source",
                        "Create a text source draft without filesystem writes or connector fetching.",
                        true,
                        createSourceInputSchema()
                ),
                tool(
                        "get_obsidian_note",
                        "Read a registered Obsidian note without exposing absolute paths.",
                        true,
                        getObsidianNoteInputSchema()
                ),
                tool(
                        "create_personal_record",
                        "Create a personal record draft through MCP.",
                        true,
                        createPersonalRecordInputSchema()
                )
        );
    }

    public McpToolListResponse listTools() {
        return new McpToolListResponse(tools);
    }

    public McpToolCallResponse callTool(String toolName, Map<String, Object> arguments, String callerType, String callerId) {
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        String callUid = nextUid("mcp_call");
        LocalDateTime createdAt = LocalDateTime.now();
        long start = System.nanoTime();
        String normalizedCallerType = normalizeCallerType(callerType);
        String normalizedCallerId = normalizeCallerId(callerId);
        try {
            McpToolDefinition tool = findTool(toolName);
            if (!tool.enabled()) {
                throw new BusinessException(ErrorCode.MCP_TOOL_DISABLED);
            }
            Object result = transactionTemplate.execute(status -> switch (toolName) {
                case "search_sources" -> searchSources(safeArguments);
                case "get_source" -> getSource(safeArguments);
                case "create_source" -> createSource(safeArguments);
                case "get_obsidian_note" -> getObsidianNote(safeArguments);
                case "create_personal_record" -> createPersonalRecord(safeArguments, normalizedCallerId);
                default -> throw new BusinessException(ErrorCode.MCP_TOOL_NOT_FOUND);
            });
            long durationMs = durationMs(start);
            saveCallLog(
                    callUid,
                    toolName,
                    normalizedCallerType,
                    normalizedCallerId,
                    sanitizedInput(toolName, safeArguments),
                    sanitizedOutput(result),
                    "completed",
                    null,
                    null,
                    durationMs,
                    createdAt
            );
            return new McpToolCallResponse(
                    callUid,
                    toolName,
                    "completed",
                    result,
                    null,
                    durationMs,
                    toOffset(createdAt)
            );
        } catch (BusinessException exception) {
            long durationMs = durationMs(start);
            saveCallLog(
                    callUid,
                    toolName,
                    normalizedCallerType,
                    normalizedCallerId,
                    sanitizedInput(toolName, safeArguments),
                    Map.of(),
                    "failed",
                    exception.getErrorCode().code(),
                    exception.getMessage(),
                    durationMs,
                    createdAt
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public McpToolCallPageResponse listCalls(String toolName, String status, String callerType, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizeMcpCallPageSize(pageSize);
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (hasText(toolName)) {
            where.append(" AND tool_name = ?");
            params.add(toolName);
        }
        if (hasText(status)) {
            where.append(" AND status = ?");
            params.add(status);
        }
        if (hasText(callerType)) {
            where.append(" AND caller_type = ?");
            params.add(callerType);
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mcp_tool_calls" + where,
                Long.class,
                params.toArray()
        );
        params.add(normalizedPageSize);
        params.add((normalizedPage - 1) * normalizedPageSize);
        List<Map<String, Object>> items = jdbcTemplate.query(
                "SELECT call_uid, tool_name, caller_type, caller_id, status, error_code, error_message, "
                        + "duration_ms, created_at FROM mcp_tool_calls"
                        + where
                        + " ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    put(item, "callUid", rs.getString("call_uid"));
                    put(item, "toolName", rs.getString("tool_name"));
                    put(item, "callerType", rs.getString("caller_type"));
                    put(item, "callerId", rs.getString("caller_id"));
                    put(item, "status", rs.getString("status"));
                    put(item, "errorCode", rs.getString("error_code"));
                    put(item, "errorMessage", rs.getString("error_message"));
                    put(item, "durationMs", rs.getLong("duration_ms"));
                    put(item, "createdAt", toOffset(rs.getTimestamp("created_at")));
                    return item;
                },
                params.toArray()
        );
        return new McpToolCallPageResponse(items, normalizedPage, normalizedPageSize, total == null ? 0 : total);
    }

    private Map<String, Object> searchSources(Map<String, Object> arguments) {
        String keyword = stringValue(arguments.get("keyword"));
        String status = stringValue(arguments.get("status"));
        if (keyword != null && keyword.length() > 200) {
            throw new BusinessException(ErrorCode.MCP_INVALID_INPUT);
        }
        int page = normalizePage(intValue(arguments.get("page"), 1));
        int pageSize = normalizePageSize(intValue(arguments.get("pageSize"), DEFAULT_PAGE_SIZE));

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (hasText(keyword)) {
            where.append(" AND (LOWER(s.title) LIKE ? OR LOWER(sf.file_name) LIKE ?)");
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            params.add(like);
            params.add(like);
        }
        if (hasText(status)) {
            requireOneOf(status, SOURCE_STATUSES, "status");
            where.append(" AND s.status = ?");
            params.add(status);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sources s LEFT JOIN source_files sf ON sf.source_id = s.id" + where,
                Long.class,
                params.toArray()
        );
        params.add(pageSize);
        params.add((page - 1) * pageSize);
        List<Map<String, Object>> items = jdbcTemplate.query(
                "SELECT s.source_uid, s.title, s.source_type, s.source_platform, s.status, s.created_at, "
                        + "sf.file_uid, sf.file_name, sf.file_ext, sf.file_size, sf.mime_type, sf.parse_status, "
                        + "sf.organize_status, n.note_uid, n.title AS note_title "
                        + "FROM sources s "
                        + "LEFT JOIN source_files sf ON sf.source_id = s.id "
                        + "LEFT JOIN obsidian_notes n ON n.source_file_id = sf.id "
                        + where
                        + " ORDER BY s.created_at DESC, s.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    put(item, "sourceUid", rs.getString("source_uid"));
                    put(item, "fileUid", rs.getString("file_uid"));
                    put(item, "title", rs.getString("title"));
                    put(item, "sourceType", rs.getString("source_type"));
                    put(item, "sourcePlatform", rs.getString("source_platform"));
                    put(item, "status", rs.getString("status"));
                    put(item, "fileName", rs.getString("file_name"));
                    put(item, "fileExt", rs.getString("file_ext"));
                    put(item, "mimeType", rs.getString("mime_type"));
                    put(item, "fileSize", nullableLong(rs, "file_size"));
                    put(item, "parseStatus", rs.getString("parse_status"));
                    put(item, "organizeStatus", rs.getString("organize_status"));
                    put(item, "obsidianNoteUid", rs.getString("note_uid"));
                    put(item, "obsidianNoteTitle", rs.getString("note_title"));
                    put(item, "createdAt", toOffset(rs.getTimestamp("created_at")));
                    return item;
                },
                params.toArray()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "items", items);
        put(result, "page", page);
        put(result, "pageSize", pageSize);
        put(result, "total", total == null ? 0 : total);
        return result;
    }

    private Map<String, Object> getSource(Map<String, Object> arguments) {
        String sourceUid = stringValue(arguments.get("sourceUid"));
        String fileUid = stringValue(arguments.get("fileUid"));
        if (!hasText(sourceUid) && !hasText(fileUid)) {
            throw new BusinessException(ErrorCode.MCP_INVALID_INPUT, "sourceUid or fileUid is required");
        }
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT s.id AS source_id, s.source_uid, s.title, s.source_type, s.source_platform, s.status, "
                        + "s.created_at AS source_created_at, sf.id AS source_file_id, sf.file_uid, sf.file_name, "
                        + "sf.file_ext, sf.file_size, sf.mime_type, sf.parse_status, sf.organize_status "
                        + "FROM sources s LEFT JOIN source_files sf ON sf.source_id = s.id "
                        + "WHERE (? IS NOT NULL AND s.source_uid = ?) OR (? IS NOT NULL AND sf.file_uid = ?) "
                        + "ORDER BY sf.id ASC LIMIT 1",
                (rs, rowNum) -> sourceRow(rs),
                hasText(sourceUid) ? sourceUid : null,
                sourceUid,
                hasText(fileUid) ? fileUid : null,
                fileUid
        );
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.MCP_SOURCE_NOT_FOUND);
        }
        Map<String, Object> row = rows.get(0);
        Long sourceFileId = (Long) row.get("sourceFileId");
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "source", safeSource(row));
        put(result, "sourceFile", safeSourceFile(row));
        put(result, "content", sourceFileId == null ? null : contentSummary(sourceFileId));
        put(result, "obsidianNote", sourceFileId == null ? null : obsidianNoteSummary(sourceFileId));
        return result;
    }

    private Map<String, Object> createSource(Map<String, Object> arguments) {
        String title = requiredString(arguments, "title");
        String rawContent = requiredString(arguments, "rawContent");
        if (title.length() > 512 || rawContent.length() > 100000) {
            throw new BusinessException(ErrorCode.MCP_INVALID_INPUT);
        }
        String sourceType = defaultString(arguments.get("sourceType"), "text");
        String sourcePlatform = defaultString(arguments.get("sourcePlatform"), "manual");
        String processingIntent = defaultString(arguments.get("processingIntent"), "organize_only");
        String sourceUrl = stringValue(arguments.get("sourceUrl"));
        requireOneOf(sourceType, SOURCE_TYPES, "sourceType");
        requireOneOf(processingIntent, PROCESSING_INTENTS, "processingIntent");
        if (sourcePlatform.length() > 128 || (sourceUrl != null && sourceUrl.length() > 2048)) {
            throw new BusinessException(ErrorCode.MCP_INVALID_INPUT);
        }
        String sourceUid = nextUid("src");
        String fileUid = nextUid("file");
        String contentUid = nextUid("content");
        String jobUid = nextUid("job");
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO import_jobs (
                    job_uid, import_type, input_url, status, total_count, success_count, skipped_count,
                    failed_count, created_at, updated_at
                ) VALUES (?, 'mcp_text', ?, 'completed', 1, 1, 0, 0, ?, ?)
                """, jobUid, sourceUrl, now, now);
        Long importJobId = jdbcTemplate.queryForObject(
                "SELECT id FROM import_jobs WHERE job_uid = ?",
                Long.class,
                jobUid
        );
        jdbcTemplate.update("""
                INSERT INTO sources (
                    source_uid, title, source_type, source_platform, source_url, raw_organize_status,
                    processing_intent, status, collected_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 'pending', ?, 'pending', ?, ?, ?)
                """, sourceUid, title, sourceType, sourcePlatform, sourceUrl, processingIntent, now, now, now);
        Long sourceId = jdbcTemplate.queryForObject(
                "SELECT id FROM sources WHERE source_uid = ?",
                Long.class,
                sourceUid
        );
        jdbcTemplate.update("""
                INSERT INTO source_files (
                    file_uid, source_id, import_job_id, file_name, file_ext, original_path, managed_path,
                    file_size, mime_type, content_hash, parser_name, parse_status, organize_status, created_at
                ) VALUES (?, ?, ?, ?, 'txt', ?, NULL, ?, 'text/plain', ?, 'mcp-preview', 'success', 'pending', ?)
                """,
                fileUid,
                sourceId,
                importJobId,
                title + ".txt",
                "mcp://source/" + sourceUid,
                (long) rawContent.length(),
                sha256(rawContent),
                now
        );
        Long sourceFileId = jdbcTemplate.queryForObject(
                "SELECT id FROM source_files WHERE file_uid = ?",
                Long.class,
                fileUid
        );
        jdbcTemplate.update("""
                INSERT INTO source_contents (
                    content_uid, source_id, source_file_id, parser_name, content_type, raw_text, text_hash,
                    char_count, raw_text_saved, parse_status, created_at, updated_at
                ) VALUES (?, ?, ?, 'mcp-preview', 'plain_text', ?, ?, ?, TRUE, 'success', ?, ?)
                """, contentUid, sourceId, sourceFileId, rawContent, sha256(rawContent), rawContent.length(), now, now);
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "sourceUid", sourceUid);
        put(result, "fileUid", fileUid);
        put(result, "status", "pending");
        put(result, "sourceType", sourceType);
        put(result, "sourcePlatform", sourcePlatform);
        put(result, "createdAt", toOffset(now));
        return result;
    }

    private Map<String, Object> getObsidianNote(Map<String, Object> arguments) {
        String noteUid = requiredString(arguments, "noteUid");
        boolean includeMarkdown = boolValue(arguments.get("includeMarkdown"), true);
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT n.note_uid, n.title, n.vault_name, n.vault_path, n.obsidian_uri, n.status, "
                        + "s.source_uid, sf.file_uid "
                        + "FROM obsidian_notes n "
                        + "JOIN sources s ON s.id = n.source_id "
                        + "LEFT JOIN source_files sf ON sf.id = n.source_file_id "
                        + "WHERE n.note_uid = ? LIMIT 1",
                (rs, rowNum) -> {
                    Map<String, Object> note = new LinkedHashMap<>();
                    put(note, "noteUid", rs.getString("note_uid"));
                    put(note, "sourceUid", rs.getString("source_uid"));
                    put(note, "fileUid", rs.getString("file_uid"));
                    put(note, "title", rs.getString("title"));
                    put(note, "vaultName", rs.getString("vault_name"));
                    put(note, "vaultPath", rs.getString("vault_path"));
                    put(note, "obsidianUri", rs.getString("obsidian_uri"));
                    put(note, "status", rs.getString("status"));
                    return note;
                },
                noteUid
        );
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.MCP_OBSIDIAN_NOTE_NOT_FOUND);
        }
        Map<String, Object> note = rows.get(0);
        String vaultPath = safeVaultPathForOutput(stringValue(note.get("vaultPath")));
        put(note, "vaultPath", vaultPath);
        if (includeMarkdown) {
            put(note, "markdown", readRegisteredNoteMarkdown(vaultPath));
        }
        return note;
    }

    private Map<String, Object> createPersonalRecord(Map<String, Object> arguments, String callerId) {
        String recordType = requiredString(arguments, "recordType");
        if (!RECORD_TYPES.contains(recordType)) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_TYPE);
        }
        String title = requiredString(arguments, "title");
        String rawContent = requiredString(arguments, "rawContent");
        if (title.length() > 512 || rawContent.length() > 100000) {
            throw new BusinessException(ErrorCode.MCP_INVALID_INPUT);
        }
        String sourceChannel = defaultString(arguments.get("sourceChannel"), "mcp");
        String sourceRef = stringValue(arguments.get("sourceRef"));
        String sensitivityLevel = defaultString(arguments.get("sensitivityLevel"), "medium");
        requireOneOf(sensitivityLevel, SENSITIVITY_LEVELS, "sensitivityLevel");
        if (sourceChannel.length() > 128 || (sourceRef != null && sourceRef.length() > 2048)) {
            throw new BusinessException(ErrorCode.MCP_INVALID_INPUT);
        }
        Object structured = arguments.get("structured");
        String recordUid = nextUid("record");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime occurredAt = parseOptionalDateTime(arguments.get("occurredAt"));
        jdbcTemplate.update("""
                INSERT INTO personal_records (
                    record_uid, record_type, title, occurred_at, source_channel, source_ref, raw_content,
                    structured_json, status, sensitivity_level, created_by, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?, ?, ?, ?)
                """,
                recordUid,
                recordType,
                title,
                occurredAt,
                sourceChannel,
                sourceRef,
                rawContent,
                structured == null ? null : toJson(structured),
                sensitivityLevel,
                callerId,
                now,
                now
        );
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "recordUid", recordUid);
        put(result, "recordType", recordType);
        put(result, "status", "pending");
        put(result, "createdAt", toOffset(now));
        return result;
    }

    private McpToolDefinition findTool(String toolName) {
        return tools.stream()
                .filter(tool -> tool.name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MCP_TOOL_NOT_FOUND));
    }

    private McpToolDefinition tool(String name, String description, boolean enabled, Map<String, Object> inputSchema) {
        return new McpToolDefinition(
                name,
                description,
                enabled,
                inputSchema,
                Map.of("type", "object")
        );
    }

    private Map<String, Object> searchSourcesInputSchema() {
        return objectSchema(
                properties(
                        prop("keyword", Map.of("type", "string", "maxLength", 200)),
                        prop("status", Map.of("type", "string", "enum", List.copyOf(SOURCE_STATUSES))),
                        prop("page", Map.of("type", "integer", "minimum", 1, "default", 1)),
                        prop("pageSize", Map.of("type", "integer", "minimum", 1, "maximum", MAX_PAGE_SIZE, "default", DEFAULT_PAGE_SIZE))
                )
        );
    }

    private Map<String, Object> getSourceInputSchema() {
        Map<String, Object> schema = objectSchema(
                properties(
                        prop("sourceUid", Map.of("type", "string", "pattern", "^src_[A-Za-z0-9_\\-]+$")),
                        prop("fileUid", Map.of("type", "string", "pattern", "^file_[A-Za-z0-9_\\-]+$")),
                        prop("includeContentExcerpt", Map.of("type", "boolean", "default", true))
                )
        );
        put(schema, "oneOf", List.of(
                        Map.of("required", List.of("sourceUid")),
                        Map.of("required", List.of("fileUid"))
        ));
        return schema;
    }

    private Map<String, Object> createSourceInputSchema() {
        return objectSchema(
                properties(
                        prop("title", Map.of("type", "string", "minLength", 1, "maxLength", 512)),
                        prop("rawContent", Map.of("type", "string", "minLength", 1, "maxLength", 100000)),
                        prop("sourceType", Map.of("type", "string", "enum", List.copyOf(SOURCE_TYPES), "default", "text")),
                        prop("sourcePlatform", Map.of("type", "string", "maxLength", 128, "default", "manual")),
                        prop("sourceUrl", Map.of("type", "string", "maxLength", 2048)),
                        prop("processingIntent", Map.of("type", "string", "enum", List.copyOf(PROCESSING_INTENTS), "default", "organize_only"))
                ),
                List.of("title", "rawContent")
        );
    }

    private Map<String, Object> getObsidianNoteInputSchema() {
        return objectSchema(
                properties(
                        prop("noteUid", Map.of("type", "string", "pattern", "^note_[A-Za-z0-9_\\-]+$")),
                        prop("includeMarkdown", Map.of("type", "boolean", "default", true))
                ),
                List.of("noteUid")
        );
    }

    private Map<String, Object> createPersonalRecordInputSchema() {
        return objectSchema(
                properties(
                        prop("recordType", Map.of("type", "string", "enum", List.of("expense", "bill", "email", "relationship", "event", "note"))),
                        prop("title", Map.of("type", "string", "minLength", 1, "maxLength", 512)),
                        prop("occurredAt", Map.of("type", "string", "format", "date-time")),
                        prop("rawContent", Map.of("type", "string", "minLength", 1, "maxLength", 100000)),
                        prop("structured", Map.of("type", "object")),
                        prop("sourceChannel", Map.of("type", "string", "maxLength", 128, "default", "mcp")),
                        prop("sourceRef", Map.of("type", "string", "maxLength", 2048)),
                        prop("sensitivityLevel", Map.of("type", "string", "enum", List.of("low", "medium", "high"), "default", "medium"))
                ),
                List.of("recordType", "title", "rawContent")
        );
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        put(schema, "type", "object");
        put(schema, "properties", properties);
        put(schema, "additionalProperties", false);
        return schema;
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, Object required) {
        Map<String, Object> schema = objectSchema(properties);
        put(schema, "required", required);
        return schema;
    }

    @SafeVarargs
    private Map<String, Object> properties(Map.Entry<String, Object>... entries) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            put(properties, entry.getKey(), entry.getValue());
        }
        return properties;
    }

    private Map.Entry<String, Object> prop(String name, Object schema) {
        return Map.entry(name, schema);
    }

    private Map<String, Object> sourceRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        put(row, "sourceId", rs.getLong("source_id"));
        put(row, "sourceUid", rs.getString("source_uid"));
        put(row, "title", rs.getString("title"));
        put(row, "sourceType", rs.getString("source_type"));
        put(row, "sourcePlatform", rs.getString("source_platform"));
        put(row, "status", rs.getString("status"));
        put(row, "sourceCreatedAt", toOffset(rs.getTimestamp("source_created_at")));
        long sourceFileId = rs.getLong("source_file_id");
        put(row, "sourceFileId", rs.wasNull() ? null : sourceFileId);
        put(row, "fileUid", rs.getString("file_uid"));
        put(row, "fileName", rs.getString("file_name"));
        put(row, "fileExt", rs.getString("file_ext"));
        put(row, "fileSize", nullableLong(rs, "file_size"));
        put(row, "mimeType", rs.getString("mime_type"));
        put(row, "parseStatus", rs.getString("parse_status"));
        put(row, "organizeStatus", rs.getString("organize_status"));
        return row;
    }

    private Map<String, Object> safeSource(Map<String, Object> row) {
        Map<String, Object> source = new LinkedHashMap<>();
        put(source, "sourceUid", row.get("sourceUid"));
        put(source, "title", row.get("title"));
        put(source, "sourceType", row.get("sourceType"));
        put(source, "sourcePlatform", row.get("sourcePlatform"));
        put(source, "status", row.get("status"));
        put(source, "createdAt", row.get("sourceCreatedAt"));
        return source;
    }

    private Map<String, Object> safeSourceFile(Map<String, Object> row) {
        if (row.get("fileUid") == null) {
            return null;
        }
        Map<String, Object> sourceFile = new LinkedHashMap<>();
        put(sourceFile, "fileUid", row.get("fileUid"));
        put(sourceFile, "fileName", row.get("fileName"));
        put(sourceFile, "fileExt", row.get("fileExt"));
        put(sourceFile, "fileSize", row.get("fileSize"));
        put(sourceFile, "mimeType", row.get("mimeType"));
        put(sourceFile, "parseStatus", row.get("parseStatus"));
        put(sourceFile, "organizeStatus", row.get("organizeStatus"));
        return sourceFile;
    }

    private Map<String, Object> contentSummary(Long sourceFileId) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT content_uid, parse_status, char_count, raw_text FROM source_contents WHERE source_file_id = ? LIMIT 1",
                (rs, rowNum) -> {
                    Map<String, Object> content = new LinkedHashMap<>();
                    String rawText = rs.getString("raw_text");
                    put(content, "contentUid", rs.getString("content_uid"));
                    put(content, "parseStatus", rs.getString("parse_status"));
                    put(content, "charCount", rs.getInt("char_count"));
                    put(content, "excerpt", excerpt(rawText));
                    return content;
                },
                sourceFileId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> obsidianNoteSummary(Long sourceFileId) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT note_uid, title, vault_path, obsidian_uri, status FROM obsidian_notes WHERE source_file_id = ? "
                        + "ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> {
                    Map<String, Object> note = new LinkedHashMap<>();
                    put(note, "noteUid", rs.getString("note_uid"));
                    put(note, "title", rs.getString("title"));
                    put(note, "vaultPath", rs.getString("vault_path"));
                    put(note, "obsidianUri", rs.getString("obsidian_uri"));
                    put(note, "status", rs.getString("status"));
                    return note;
                },
                sourceFileId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> sanitizedInput(String toolName, Map<String, Object> arguments) {
        Map<String, Object> sanitized = new LinkedHashMap<>(arguments);
        Object rawContent = sanitized.remove("rawContent");
        if (rawContent instanceof String rawContentText) {
            put(sanitized, "rawContentLength", rawContentText.length());
            put(sanitized, "rawContentSha256", sha256(rawContentText));
        }
        Object structured = sanitized.remove("structured");
        if (structured != null) {
            put(sanitized, "structuredRedacted", true);
        }
        Object markdown = sanitized.remove("markdown");
        if (markdown instanceof String markdownText) {
            put(sanitized, "markdownLength", markdownText.length());
        }
        put(sanitized, "toolName", toolName);
        return sanitized;
    }

    private Object sanitizedOutput(Object result) {
        return sanitizeValueForLog(result);
    }

    private Object sanitizeValueForLog(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object entryValue = entry.getValue();
                if ("markdown".equals(key) && entryValue instanceof String markdownText) {
                    put(sanitized, "markdownLength", markdownText.length());
                    put(sanitized, "markdownSha256", sha256(markdownText));
                    continue;
                }
                if ("rawContent".equals(key) && entryValue instanceof String rawContentText) {
                    put(sanitized, "rawContentLength", rawContentText.length());
                    put(sanitized, "rawContentSha256", sha256(rawContentText));
                    continue;
                }
                if ("structured".equals(key)) {
                    put(sanitized, "structuredRedacted", true);
                    continue;
                }
                put(sanitized, key, sanitizeValueForLog(entryValue));
            }
            return sanitized;
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::sanitizeValueForLog)
                    .toList();
        }
        return value;
    }

    private void saveCallLog(
            String callUid,
            String toolName,
            String callerType,
            String callerId,
            Object input,
            Object output,
            String status,
            String errorCode,
            String errorMessage,
            long durationMs,
            LocalDateTime createdAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO mcp_tool_calls (
                    call_uid, tool_name, caller_type, caller_id, input_json, output_json, status,
                    error_code, error_message, duration_ms, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                callUid,
                toolName,
                callerType,
                callerId,
                toJson(input),
                toJson(output),
                status,
                errorCode,
                errorMessage,
                durationMs,
                createdAt
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.MCP_CALL_FAILED, "mcp json cannot be generated");
        }
    }

    private String requiredString(Map<String, Object> arguments, String key) {
        String value = stringValue(arguments.get(key));
        if (!hasText(value)) {
            throw new BusinessException(ErrorCode.MCP_INVALID_INPUT, key + " is required");
        }
        return value;
    }

    private String defaultString(Object value, String defaultValue) {
        String stringValue = stringValue(value);
        return hasText(stringValue) ? stringValue : defaultValue;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && hasText(text)) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException exception) {
                throw new BusinessException(ErrorCode.MCP_INVALID_INPUT);
            }
        }
        return defaultValue;
    }

    private boolean boolValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String text && hasText(text)) {
            return Boolean.parseBoolean(text);
        }
        return defaultValue;
    }

    private LocalDateTime parseOptionalDateTime(Object value) {
        String text = stringValue(value);
        if (!hasText(text)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(text);
            } catch (DateTimeParseException exception) {
                throw new BusinessException(ErrorCode.MCP_INVALID_INPUT, "occurredAt is invalid");
            }
        }
    }

    private void requireOneOf(String value, Set<String> allowedValues, String fieldName) {
        if (!allowedValues.contains(value)) {
            throw new BusinessException(ErrorCode.MCP_INVALID_INPUT, fieldName + " is invalid");
        }
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int normalizeMcpCallPageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_MCP_CALL_PAGE_SIZE);
    }

    private String normalizeCallerType(String callerType) {
        return hasText(callerType) ? callerType : "external_agent";
    }

    private String normalizeCallerId(String callerId) {
        return hasText(callerId) ? callerId : "local-mcp-preview";
    }

    private String excerpt(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 1000 ? text : text.substring(0, 1000);
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private OffsetDateTime toOffset(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return toOffset(timestamp.toLocalDateTime());
    }

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String readRegisteredNoteMarkdown(String vaultPath) {
        Path vaultRoot = vaultRoot();
        Path notePath = resolveVaultPath(vaultRoot, vaultPath);
        if (!Files.exists(notePath, LinkOption.NOFOLLOW_LINKS) && !Files.exists(notePath)) {
            throw new BusinessException(ErrorCode.MCP_OBSIDIAN_NOTE_NOT_FOUND);
        }
        try {
            Path realNotePath = notePath.toRealPath();
            if (!realNotePath.startsWith(vaultRoot) || !Files.isRegularFile(realNotePath, LinkOption.NOFOLLOW_LINKS)) {
                throw new BusinessException(ErrorCode.MCP_FORBIDDEN_PATH_EXPOSURE);
            }
            return Files.readString(realNotePath, StandardCharsets.UTF_8);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.MCP_CALL_FAILED, "obsidian note cannot be read");
        }
    }

    private Path vaultRoot() {
        String configuredPath = runtimeProperties.obsidianVaultPath();
        if (!hasText(configuredPath)) {
            throw new BusinessException(ErrorCode.MCP_CALL_FAILED, "obsidian vault path is not configured");
        }
        try {
            return PathSafety.normalizeAbsolute(Path.of(configuredPath)).toRealPath();
        } catch (BusinessException exception) {
            throw new BusinessException(ErrorCode.MCP_CALL_FAILED, "obsidian vault path is invalid");
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.MCP_CALL_FAILED, "obsidian vault path cannot be resolved");
        }
    }

    private String safeVaultPathForOutput(String vaultPath) {
        return relativeVaultPath(vaultPath).toString().replace('\\', '/');
    }

    private Path resolveVaultPath(Path vaultRoot, String vaultPath) {
        Path relativePath = relativeVaultPath(vaultPath);
        Path resolved = vaultRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(vaultRoot)) {
            throw new BusinessException(ErrorCode.MCP_FORBIDDEN_PATH_EXPOSURE);
        }
        return resolved;
    }

    private Path relativeVaultPath(String vaultPath) {
        if (!hasText(vaultPath)) {
            throw new BusinessException(ErrorCode.MCP_OBSIDIAN_NOTE_NOT_FOUND);
        }
        Path relativePath = Path.of(vaultPath);
        if (relativePath.isAbsolute()) {
            throw new BusinessException(ErrorCode.MCP_FORBIDDEN_PATH_EXPOSURE);
        }
        Path normalized = relativePath.normalize();
        if (normalized.startsWith("..")) {
            throw new BusinessException(ErrorCode.MCP_FORBIDDEN_PATH_EXPOSURE);
        }
        return normalized;
    }

    private long durationMs(long startNanos) {
        return Math.max(0, (System.nanoTime() - startNanos) / 1_000_000);
    }

    private void put(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nextUid(String prefix) {
        if ("job".equals(prefix)) {
            String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            return "job_" + date + "_" + randomSuffix;
        }
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.MCP_CALL_FAILED, "sha-256 is not available");
        }
    }
}
