package com.wikiforge.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.common.filesystem.PathSafety;
import com.wikiforge.core.application.dto.CreatePersonalRecordRequest;
import com.wikiforge.core.application.dto.PersonalRecordObsidianNoteResponse;
import com.wikiforge.core.application.dto.PersonalRecordPageResponse;
import com.wikiforge.core.application.dto.PersonalRecordResponse;
import com.wikiforge.core.application.dto.PersonalRecordSummaryResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
public class PersonalRecordService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String PERSONAL_RECORD_DIRECTORY = "00_Inbox_收集箱/Personal_个人记录";
    private static final Set<String> RECORD_TYPES = Set.of(
            "expense",
            "bill",
            "email",
            "relationship",
            "event",
            "note"
    );
    private static final Set<String> SENSITIVITY_LEVELS = Set.of("low", "medium", "high");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final CoreRuntimeProperties runtimeProperties;

    public PersonalRecordService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            CoreRuntimeProperties runtimeProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.runtimeProperties = runtimeProperties;
    }

    @Transactional
    public PersonalRecordResponse createRecord(CreatePersonalRecordRequest request) {
        String recordType = normalizeRequired(request.recordType(), "recordType");
        if (!RECORD_TYPES.contains(recordType)) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_TYPE);
        }
        String title = normalizeRequired(request.title(), "title");
        String rawContent = normalizeRequired(request.rawContent(), "rawContent");
        if (title.length() > 512 || rawContent.length() > 100000) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_INPUT);
        }
        String sourceChannel = defaultText(request.sourceChannel(), "manual");
        String sourceRef = normalizeOptional(request.sourceRef());
        String sensitivityLevel = defaultText(request.sensitivityLevel(), "medium");
        if (!SENSITIVITY_LEVELS.contains(sensitivityLevel)) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_INPUT, "sensitivityLevel is invalid");
        }
        if (sourceChannel.length() > 128 || (sourceRef != null && sourceRef.length() > 2048)) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_INPUT);
        }
        String createdBy = defaultText(request.createdBy(), "web-ui");
        if (createdBy.length() > 128) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_INPUT);
        }
        String structuredJson = request.structured() == null ? null : toJson(request.structured());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime occurredAt = parseOptionalDateTime(request.occurredAt());
        String recordUid = nextUid("record");

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
                structuredJson,
                sensitivityLevel,
                createdBy,
                now,
                now
        );
        return getRecord(recordUid);
    }

    @Transactional(readOnly = true)
    public PersonalRecordPageResponse listRecords(
            String recordType,
            String status,
            String sourceChannel,
            int page,
            int pageSize
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        QueryParts queryParts = queryParts(recordType, status, sourceChannel, null);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personal_records" + queryParts.where(),
                Long.class,
                queryParts.params().toArray()
        );
        List<Object> params = new ArrayList<>(queryParts.params());
        params.add(normalizedPageSize);
        params.add((normalizedPage - 1) * normalizedPageSize);
        List<PersonalRecordResponse> items = jdbcTemplate.query(
                baseSelect()
                        + queryParts.where()
                        + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                this::mapRecord,
                params.toArray()
        );
        return new PersonalRecordPageResponse(items, normalizedPage, normalizedPageSize, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public PersonalRecordResponse getRecord(String recordUid) {
        List<PersonalRecordResponse> records = jdbcTemplate.query(
                baseSelect() + " WHERE record_uid = ? LIMIT 1",
                this::mapRecord,
                recordUid
        );
        if (records.isEmpty()) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_NOT_FOUND);
        }
        return records.get(0);
    }

    @Transactional(readOnly = true)
    public PersonalRecordSummaryResponse summary(String period) {
        String normalizedPeriod = normalizePeriod(period);
        LocalDateTime startAt = periodStart(normalizedPeriod);
        QueryParts queryParts = queryParts(null, null, null, startAt);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personal_records" + queryParts.where(),
                Long.class,
                queryParts.params().toArray()
        );
        Map<String, Long> byType = countBy("record_type", queryParts);
        Map<String, Long> byStatus = countBy("status", queryParts);
        List<Object> recentParams = new ArrayList<>(queryParts.params());
        recentParams.add(5);
        List<PersonalRecordResponse> recentItems = jdbcTemplate.query(
                baseSelect()
                        + queryParts.where()
                        + " ORDER BY created_at DESC, id DESC LIMIT ?",
                this::mapRecord,
                recentParams.toArray()
        );
        return new PersonalRecordSummaryResponse(
                normalizedPeriod,
                total == null ? 0 : total,
                byType,
                byStatus,
                recentItems
        );
    }

    @Transactional
    public PersonalRecordObsidianNoteResponse writeObsidianNote(String recordUid) {
        PersonalRecordResponse record = getRecord(recordUid);
        Path vaultRoot = vaultRoot();
        LocalDateTime now = LocalDateTime.now();
        String vaultPath = recordVaultPath(record, now);
        Path target = resolveVaultPath(vaultRoot, vaultPath);
        String markdown = markdown(record, vaultPath, now);
        writeAtomically(target, markdown);
        String obsidianUri = obsidianUri(runtimeProperties.obsidianVaultName(), vaultPath);
        jdbcTemplate.update("""
                UPDATE personal_records
                SET status = 'archived', obsidian_vault_path = ?, obsidian_uri = ?, archived_at = ?, updated_at = ?
                WHERE record_uid = ?
                """, vaultPath, obsidianUri, now, now, recordUid);
        return new PersonalRecordObsidianNoteResponse(
                recordUid,
                record.title(),
                runtimeProperties.obsidianVaultName(),
                vaultPath,
                obsidianUri,
                "archived",
                toOffset(now)
        );
    }

    private QueryParts queryParts(String recordType, String status, String sourceChannel, LocalDateTime startAt) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (hasText(recordType)) {
            where.append(" AND record_type = ?");
            params.add(recordType);
        }
        if (hasText(status)) {
            where.append(" AND status = ?");
            params.add(status);
        }
        if (hasText(sourceChannel)) {
            where.append(" AND source_channel = ?");
            params.add(sourceChannel);
        }
        if (startAt != null) {
            where.append(" AND created_at >= ?");
            params.add(startAt);
        }
        return new QueryParts(where.toString(), params);
    }

    private Map<String, Long> countBy(String column, QueryParts queryParts) {
        List<Map.Entry<String, Long>> rows = jdbcTemplate.query(
                "SELECT " + column + " AS item_key, COUNT(*) AS item_count FROM personal_records"
                        + queryParts.where()
                        + " GROUP BY " + column
                        + " ORDER BY item_count DESC",
                (rs, rowNum) -> Map.entry(rs.getString("item_key"), rs.getLong("item_count")),
                queryParts.params().toArray()
        );
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> row : rows) {
            result.put(row.getKey(), row.getValue());
        }
        return result;
    }

    private String baseSelect() {
        return """
                SELECT record_uid, record_type, title, occurred_at, source_channel, source_ref, raw_content,
                    structured_json, status, sensitivity_level, created_by, obsidian_vault_path, obsidian_uri,
                    archived_at, created_at, updated_at
                FROM personal_records
                """;
    }

    private PersonalRecordResponse mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return new PersonalRecordResponse(
                rs.getString("record_uid"),
                rs.getString("record_type"),
                rs.getString("title"),
                toOffset(rs.getTimestamp("occurred_at")),
                rs.getString("source_channel"),
                rs.getString("source_ref"),
                rs.getString("raw_content"),
                rs.getString("structured_json"),
                rs.getString("status"),
                rs.getString("sensitivity_level"),
                rs.getString("created_by"),
                rs.getString("obsidian_vault_path"),
                rs.getString("obsidian_uri"),
                toOffset(rs.getTimestamp("archived_at")),
                toOffset(rs.getTimestamp("created_at")),
                toOffset(rs.getTimestamp("updated_at"))
        );
    }

    private String recordVaultPath(PersonalRecordResponse record, LocalDateTime now) {
        String month = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return PERSONAL_RECORD_DIRECTORY
                + "/"
                + record.recordType()
                + "/"
                + month
                + "/"
                + safeFileName(record.title())
                + "-"
                + record.recordUid()
                + ".md";
    }

    private String markdown(PersonalRecordResponse record, String vaultPath, LocalDateTime archivedAt) {
        return """
                ---
                id: %s
                type: personal_record
                record_type: %s
                title: %s
                source_channel: %s
                source_ref: %s
                sensitivity_level: %s
                occurred_at: %s
                archived_at: %s
                vault_path: %s
                status: archived
                tags:
                  - wikiforge/personal-record
                  - personal/%s
                ---
                # %s

                ## 原始记录 Raw Record

                %s

                %s

                ## 后续处理 Next Actions

                - [ ] 判断是否需要进入知识卡片或 Wiki 编译
                - [ ] 如涉及人物、项目或账单，后续补充关联主题
                """.formatted(
                yamlScalar(record.recordUid()),
                yamlScalar(record.recordType()),
                yamlValue(record.title()),
                yamlScalar(record.sourceChannel()),
                yamlValue(record.sourceRef()),
                yamlScalar(record.sensitivityLevel()),
                yamlScalar(record.occurredAt() == null ? "" : record.occurredAt().toString()),
                yamlScalar(toOffset(archivedAt).toString()),
                yamlValue(vaultPath),
                tagValue(record.recordType()),
                record.title(),
                value(record.rawContent()),
                structuredSection(record.structuredJson())
        );
    }

    private String structuredSection(String structuredJson) {
        if (structuredJson == null || structuredJson.isBlank()) {
            return "";
        }
        return "## 结构化数据 Structured Data\n\n```json\n" + prettyJson(structuredJson) + "\n```";
    }

    private String prettyJson(String structuredJson) {
        try {
            Object value = objectMapper.readValue(structuredJson, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception exception) {
            return structuredJson;
        }
    }

    private Path vaultRoot() {
        String configuredPath = runtimeProperties.obsidianVaultPath();
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian vault path is not configured");
        }
        Path root = PathSafety.normalizeAbsolute(Path.of(configuredPath));
        try {
            Files.createDirectories(root);
            return root.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian vault path cannot be created");
        }
    }

    private Path resolveVaultPath(Path vaultRoot, String vaultPath) {
        Path relativePath = Path.of(vaultPath);
        if (relativePath.isAbsolute()) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian note path must be relative");
        }
        Path resolved = vaultRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(vaultRoot)) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian note path escapes vault");
        }
        return resolved;
    }

    private void writeAtomically(Path target, String markdown) {
        try {
            Files.createDirectories(target.getParent());
            Path temp = target.resolveSibling(target.getFileName() + ".wf.tmp");
            Files.writeString(temp, markdown, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian note cannot be written");
        }
    }

    private String obsidianUri(String vaultName, String vaultPath) {
        return "obsidian://open?vault=" + encode(vaultName) + "&file=" + encode(vaultPath);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private LocalDateTime periodStart(String period) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "daily" -> today.atStartOfDay();
            case "weekly" -> today.with(DayOfWeek.MONDAY).atStartOfDay();
            case "monthly" -> today.withDayOfMonth(1).atStartOfDay();
            default -> null;
        };
    }

    private String normalizePeriod(String period) {
        String normalized = defaultText(period, "all").toLowerCase(Locale.ROOT);
        if (!Set.of("all", "daily", "weekly", "monthly").contains(normalized)) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_INPUT, "period is invalid");
        }
        return normalized;
    }

    private LocalDateTime parseOptionalDateTime(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(normalized).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Try local date-time next.
        }
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
            // Try date-only next.
        }
        try {
            return LocalDate.parse(normalized).atStartOfDay();
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_INPUT, "occurredAt is invalid");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_INPUT, "structured json cannot be generated");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.PERSONAL_RECORD_INVALID_INPUT, fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String defaultText(String value, String defaultValue) {
        String normalized = normalizeOptional(value);
        return normalized == null ? defaultValue : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    private String nextUid(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return prefix + "_" + date + "_" + suffix;
    }

    private OffsetDateTime toOffset(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return toOffset(timestamp.toLocalDateTime());
    }

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String safeFileName(String value) {
        String safe = value == null ? "untitled" : value;
        safe = safe.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "-")
                .replaceAll("\\s+", " ")
                .trim();
        if (safe.isBlank()) {
            safe = "untitled";
        }
        return safe.length() > 80 ? safe.substring(0, 80).trim() : safe;
    }

    private String yamlValue(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    private String yamlScalar(String value) {
        return value == null ? "" : value;
    }

    private String tagValue(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record QueryParts(String where, List<Object> params) {
    }
}
