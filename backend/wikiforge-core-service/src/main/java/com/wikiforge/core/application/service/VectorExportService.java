package com.wikiforge.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.core.application.dto.CreateVectorExportRequest;
import com.wikiforge.core.application.dto.VectorExportJobResponse;
import com.wikiforge.core.application.dto.VectorExportPageResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

@Service
public class VectorExportService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_CHUNK_CHARS = 1600;
    private static final int MIN_CHUNK_CHARS = 200;
    private static final int MAX_CHUNK_CHARS = 8000;
    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 10000;
    private static final String EXPORT_FORMAT = "jsonl";
    private static final Set<String> SCOPES = Set.of("all", "sources", "personal_records");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final CoreRuntimeProperties runtimeProperties;

    public VectorExportService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            CoreRuntimeProperties runtimeProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.runtimeProperties = runtimeProperties;
    }

    @Transactional
    public VectorExportJobResponse createExport(CreateVectorExportRequest request) {
        String scope = normalizeScope(request == null ? null : request.scope());
        String targetCollection = normalizeTargetCollection(request == null ? null : request.targetCollection());
        int maxChunkChars = normalizeMaxChunkChars(request == null ? null : request.maxChunkChars());
        int limit = normalizeLimit(request == null ? null : request.limit());
        String exportUid = nextUid("vexp");
        LocalDateTime now = LocalDateTime.now();

        List<ChunkRow> chunks = new ArrayList<>();
        if ("all".equals(scope) || "sources".equals(scope)) {
            addSourceContentChunks(chunks, exportUid, targetCollection, maxChunkChars, limit);
        }
        if ("all".equals(scope) || "personal_records".equals(scope)) {
            addPersonalRecordChunks(chunks, exportUid, targetCollection, maxChunkChars, limit);
        }

        String dateDirectory = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String exportFileName = exportUid + "." + EXPORT_FORMAT;
        String exportRelativePath = dateDirectory + "/" + exportFileName;
        writeJsonl(exportRelativePath, chunks);

        jdbcTemplate.update("""
                INSERT INTO vector_export_jobs (
                    export_uid, scope, target_collection, export_format, status, total_count,
                    export_file_name, export_relative_path, created_at, finished_at
                ) VALUES (?, ?, ?, ?, 'completed', ?, ?, ?, ?, ?)
                """,
                exportUid,
                scope,
                targetCollection,
                EXPORT_FORMAT,
                chunks.size(),
                exportFileName,
                exportRelativePath,
                now,
                now
        );

        for (ChunkRow chunk : chunks) {
            jdbcTemplate.update("""
                    INSERT INTO content_chunks (
                        chunk_uid, export_uid, content_type, source_uid, file_uid, record_uid,
                        title, chunk_index, chunk_text, text_hash, char_count, token_estimate,
                        metadata_json, embedding_status, target_collection, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?, ?, ?)
                    """,
                    chunk.chunkUid(),
                    chunk.exportUid(),
                    chunk.contentType(),
                    chunk.sourceUid(),
                    chunk.fileUid(),
                    chunk.recordUid(),
                    chunk.title(),
                    chunk.chunkIndex(),
                    chunk.chunkText(),
                    chunk.textHash(),
                    chunk.charCount(),
                    chunk.tokenEstimate(),
                    toJson(chunk.metadata()),
                    chunk.targetCollection(),
                    now,
                    now
            );
        }

        return getExport(exportUid);
    }

    @Transactional(readOnly = true)
    public VectorExportPageResponse listExports(String status, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        QueryParts queryParts = exportQueryParts(status);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_export_jobs" + queryParts.where(),
                Long.class,
                queryParts.params().toArray()
        );
        List<Object> params = new ArrayList<>(queryParts.params());
        params.add(normalizedPageSize);
        params.add((normalizedPage - 1) * normalizedPageSize);
        List<VectorExportJobResponse> items = jdbcTemplate.query(
                baseExportSelect()
                        + queryParts.where()
                        + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                this::mapExportJob,
                params.toArray()
        );
        return new VectorExportPageResponse(items, normalizedPage, normalizedPageSize, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public VectorExportJobResponse getExport(String exportUid) {
        List<VectorExportJobResponse> exports = jdbcTemplate.query(
                baseExportSelect() + " WHERE export_uid = ? LIMIT 1",
                this::mapExportJob,
                exportUid
        );
        if (exports.isEmpty()) {
            throw new BusinessException(ErrorCode.VECTOR_EXPORT_JOB_NOT_FOUND);
        }
        return exports.get(0);
    }

    private void addSourceContentChunks(
            List<ChunkRow> chunks,
            String exportUid,
            String targetCollection,
            int maxChunkChars,
            int limit
    ) {
        List<SourceDocument> documents = jdbcTemplate.query("""
                SELECT sc.content_uid, sc.raw_text, sc.text_hash, s.source_uid, s.title,
                    s.source_type, s.source_platform, s.source_url, sf.file_uid, sf.file_name
                FROM source_contents sc
                JOIN sources s ON s.id = sc.source_id
                JOIN source_files sf ON sf.id = sc.source_file_id
                WHERE sc.raw_text IS NOT NULL
                ORDER BY sc.created_at DESC, sc.id DESC
                LIMIT ?
                """,
                this::mapSourceDocument,
                limit
        );
        for (SourceDocument document : documents) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("contentUid", document.contentUid());
            metadata.put("sourceUid", document.sourceUid());
            metadata.put("fileUid", document.fileUid());
            metadata.put("sourceType", document.sourceType());
            metadata.put("sourcePlatform", document.sourcePlatform());
            metadata.put("sourceUrl", document.sourceUrl());
            metadata.put("fileName", document.fileName());
            addChunks(
                    chunks,
                    exportUid,
                    targetCollection,
                    "source_content",
                    document.sourceUid(),
                    document.fileUid(),
                    null,
                    firstText(document.title(), document.fileName(), document.contentUid()),
                    document.rawText(),
                    document.textHash(),
                    metadata,
                    maxChunkChars
            );
        }
    }

    private void addPersonalRecordChunks(
            List<ChunkRow> chunks,
            String exportUid,
            String targetCollection,
            int maxChunkChars,
            int limit
    ) {
        List<PersonalRecordDocument> documents = jdbcTemplate.query("""
                SELECT record_uid, record_type, title, occurred_at, source_channel,
                    source_ref, raw_content, structured_json, status
                FROM personal_records
                WHERE raw_content IS NOT NULL
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """,
                this::mapPersonalRecordDocument,
                limit
        );
        for (PersonalRecordDocument document : documents) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("recordUid", document.recordUid());
            metadata.put("recordType", document.recordType());
            metadata.put("occurredAt", document.occurredAt() == null ? null : document.occurredAt().toString());
            metadata.put("sourceChannel", document.sourceChannel());
            metadata.put("sourceRef", document.sourceRef());
            metadata.put("status", document.status());
            if (document.structuredJson() != null && !document.structuredJson().isBlank()) {
                metadata.put("structuredJson", document.structuredJson());
            }
            addChunks(
                    chunks,
                    exportUid,
                    targetCollection,
                    "personal_record",
                    null,
                    null,
                    document.recordUid(),
                    document.title(),
                    document.rawContent(),
                    null,
                    metadata,
                    maxChunkChars
            );
        }
    }

    private void addChunks(
            List<ChunkRow> chunks,
            String exportUid,
            String targetCollection,
            String contentType,
            String sourceUid,
            String fileUid,
            String recordUid,
            String title,
            String text,
            String sourceTextHash,
            Map<String, Object> metadata,
            int maxChunkChars
    ) {
        String normalizedText = normalizeOptional(text);
        if (normalizedText == null) {
            return;
        }
        int chunkIndex = 0;
        for (int start = 0; start < normalizedText.length(); start += maxChunkChars) {
            int end = Math.min(start + maxChunkChars, normalizedText.length());
            String chunkText = normalizedText.substring(start, end).trim();
            if (chunkText.isBlank()) {
                continue;
            }
            Map<String, Object> chunkMetadata = new LinkedHashMap<>(metadata);
            chunkMetadata.put("chunkStart", start);
            chunkMetadata.put("chunkEnd", end);
            chunkMetadata.put("sourceTextHash", sourceTextHash);
            chunks.add(new ChunkRow(
                    nextUid("chunk"),
                    exportUid,
                    contentType,
                    sourceUid,
                    fileUid,
                    recordUid,
                    title,
                    chunkIndex,
                    chunkText,
                    sha256(chunkText),
                    chunkText.length(),
                    estimateTokens(chunkText),
                    chunkMetadata,
                    targetCollection
            ));
            chunkIndex += 1;
        }
    }

    private void writeJsonl(String exportRelativePath, List<ChunkRow> chunks) {
        Path root = Path.of(runtimeProperties.vectorExportRoot()).toAbsolutePath().normalize();
        Path target = root.resolve(exportRelativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.VECTOR_EXPORT_INVALID_INPUT, "vector export path escapes root");
        }
        try {
            Files.createDirectories(target.getParent());
            StringBuilder builder = new StringBuilder();
            for (ChunkRow chunk : chunks) {
                builder.append(toJson(chunk.toJsonLine())).append('\n');
            }
            Path temp = target.resolveSibling(target.getFileName() + ".wf.tmp");
            Files.writeString(temp, builder.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.VECTOR_EXPORT_FAILED, "vector export file cannot be written");
        }
    }

    private QueryParts exportQueryParts(String status) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        String normalizedStatus = normalizeOptional(status);
        if (normalizedStatus != null) {
            where.append(" AND status = ?");
            params.add(normalizedStatus);
        }
        return new QueryParts(where.toString(), params);
    }

    private String baseExportSelect() {
        return """
                SELECT export_uid, scope, target_collection, export_format, status, total_count,
                    export_file_name, export_relative_path, created_at, finished_at, error_message
                FROM vector_export_jobs
                """;
    }

    private VectorExportJobResponse mapExportJob(ResultSet rs, int rowNum) throws SQLException {
        return new VectorExportJobResponse(
                rs.getString("export_uid"),
                rs.getString("scope"),
                rs.getString("target_collection"),
                rs.getString("export_format"),
                rs.getString("status"),
                rs.getInt("total_count"),
                rs.getString("export_file_name"),
                rs.getString("export_relative_path"),
                toOffset(rs.getTimestamp("created_at")),
                toOffset(rs.getTimestamp("finished_at")),
                rs.getString("error_message")
        );
    }

    private SourceDocument mapSourceDocument(ResultSet rs, int rowNum) throws SQLException {
        return new SourceDocument(
                rs.getString("content_uid"),
                rs.getString("raw_text"),
                rs.getString("text_hash"),
                rs.getString("source_uid"),
                rs.getString("title"),
                rs.getString("source_type"),
                rs.getString("source_platform"),
                rs.getString("source_url"),
                rs.getString("file_uid"),
                rs.getString("file_name")
        );
    }

    private PersonalRecordDocument mapPersonalRecordDocument(ResultSet rs, int rowNum) throws SQLException {
        Timestamp occurredAt = rs.getTimestamp("occurred_at");
        return new PersonalRecordDocument(
                rs.getString("record_uid"),
                rs.getString("record_type"),
                rs.getString("title"),
                occurredAt == null ? null : occurredAt.toLocalDateTime(),
                rs.getString("source_channel"),
                rs.getString("source_ref"),
                rs.getString("raw_content"),
                rs.getString("structured_json"),
                rs.getString("status")
        );
    }

    private String normalizeScope(String scope) {
        String normalized = firstText(scope, "all").toLowerCase(Locale.ROOT);
        if (!SCOPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.VECTOR_EXPORT_INVALID_INPUT, "scope is invalid");
        }
        return normalized;
    }

    private String normalizeTargetCollection(String targetCollection) {
        String normalized = firstText(targetCollection, "wikiforge_default");
        if (normalized.length() > 128 || !normalized.matches("[A-Za-z0-9_.-]+")) {
            throw new BusinessException(ErrorCode.VECTOR_EXPORT_INVALID_INPUT, "targetCollection is invalid");
        }
        return normalized;
    }

    private int normalizeMaxChunkChars(Integer value) {
        if (value == null || value <= 0) {
            return DEFAULT_CHUNK_CHARS;
        }
        if (value < MIN_CHUNK_CHARS || value > MAX_CHUNK_CHARS) {
            throw new BusinessException(ErrorCode.VECTOR_EXPORT_INVALID_INPUT, "maxChunkChars must be 200-8000");
        }
        return value;
    }

    private int normalizeLimit(Integer value) {
        if (value == null || value <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, MAX_LIMIT);
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int estimateTokens(String text) {
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.VECTOR_EXPORT_FAILED, "json cannot be generated");
        }
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.VECTOR_EXPORT_FAILED, "sha-256 is not available");
        }
    }

    private String nextUid(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return prefix + "_" + date + "_" + suffix;
    }

    private String firstText(String... values) {
        for (String value : values) {
            String normalized = normalizeOptional(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private OffsetDateTime toOffset(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private record QueryParts(String where, List<Object> params) {
    }

    private record SourceDocument(
            String contentUid,
            String rawText,
            String textHash,
            String sourceUid,
            String title,
            String sourceType,
            String sourcePlatform,
            String sourceUrl,
            String fileUid,
            String fileName
    ) {
    }

    private record PersonalRecordDocument(
            String recordUid,
            String recordType,
            String title,
            LocalDateTime occurredAt,
            String sourceChannel,
            String sourceRef,
            String rawContent,
            String structuredJson,
            String status
    ) {
    }

    private record ChunkRow(
            String chunkUid,
            String exportUid,
            String contentType,
            String sourceUid,
            String fileUid,
            String recordUid,
            String title,
            int chunkIndex,
            String chunkText,
            String textHash,
            int charCount,
            int tokenEstimate,
            Map<String, Object> metadata,
            String targetCollection
    ) {
        private Map<String, Object> toJsonLine() {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("chunkUid", chunkUid);
            line.put("exportUid", exportUid);
            line.put("contentType", contentType);
            line.put("sourceUid", sourceUid);
            line.put("fileUid", fileUid);
            line.put("recordUid", recordUid);
            line.put("title", title);
            line.put("chunkIndex", chunkIndex);
            line.put("text", chunkText);
            line.put("textHash", textHash);
            line.put("charCount", charCount);
            line.put("tokenEstimate", tokenEstimate);
            line.put("targetCollection", targetCollection);
            line.put("metadata", metadata);
            return line;
        }
    }
}
