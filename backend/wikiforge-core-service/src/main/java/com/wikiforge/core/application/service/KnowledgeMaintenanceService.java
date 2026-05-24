package com.wikiforge.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.core.application.dto.CreateKnowledgeMaintenanceRunRequest;
import com.wikiforge.core.application.dto.KnowledgeMaintenanceItemPageResponse;
import com.wikiforge.core.application.dto.KnowledgeMaintenanceItemResponse;
import com.wikiforge.core.application.dto.KnowledgeMaintenanceRunPageResponse;
import com.wikiforge.core.application.dto.KnowledgeMaintenanceRunResponse;
import com.wikiforge.core.application.dto.UpdateKnowledgeMaintenanceItemStatusRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
public class KnowledgeMaintenanceService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_STALE_DAYS = 7;
    private static final int MAX_STALE_DAYS = 365;
    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 10000;
    private static final Set<String> ITEM_STATUSES = Set.of("open", "resolved", "ignored");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgeMaintenanceService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public KnowledgeMaintenanceRunResponse createRun(CreateKnowledgeMaintenanceRunRequest request) {
        int staleDays = normalizeStaleDays(request == null ? null : request.staleDays());
        int limit = normalizeLimit(request == null ? null : request.limit());
        String runUid = nextUid("maint");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(staleDays);

        List<MaintenanceItemRow> items = new ArrayList<>();
        addMissingSourceContentIssues(items, runUid, limit, now);
        addDuplicateSourceContentIssues(items, runUid, limit, now);
        addUnarchivedPersonalRecordIssues(items, runUid, cutoff, limit, now);
        addEmptyVectorExportIssues(items, runUid, limit, now);
        addStaleVectorChunkIssues(items, runUid, cutoff, limit, now);

        jdbcTemplate.update("""
                INSERT INTO knowledge_maintenance_runs (
                    run_uid, run_type, status, stale_days, total_count, issue_count,
                    started_at, finished_at, created_at
                ) VALUES (?, 'manual', 'completed', ?, ?, ?, ?, ?, ?)
                """,
                runUid,
                staleDays,
                items.size(),
                items.size(),
                now,
                now,
                now
        );

        for (MaintenanceItemRow item : items) {
            jdbcTemplate.update("""
                    INSERT INTO knowledge_maintenance_items (
                        item_uid, run_uid, issue_type, severity, content_type,
                        source_uid, file_uid, record_uid, chunk_uid, export_uid,
                        title, summary, evidence_json, status, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'open', ?, ?)
                    """,
                    item.itemUid(),
                    item.runUid(),
                    item.issueType(),
                    item.severity(),
                    item.contentType(),
                    item.sourceUid(),
                    item.fileUid(),
                    item.recordUid(),
                    item.chunkUid(),
                    item.exportUid(),
                    item.title(),
                    item.summary(),
                    item.evidenceJson(),
                    now,
                    now
            );
        }

        return getRun(runUid);
    }

    @Transactional(readOnly = true)
    public KnowledgeMaintenanceRunPageResponse listRuns(String status, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        QueryParts queryParts = runQueryParts(status);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_maintenance_runs" + queryParts.where(),
                Long.class,
                queryParts.params().toArray()
        );
        List<Object> params = new ArrayList<>(queryParts.params());
        params.add(normalizedPageSize);
        params.add((normalizedPage - 1) * normalizedPageSize);
        List<KnowledgeMaintenanceRunResponse> items = jdbcTemplate.query(
                baseRunSelect()
                        + queryParts.where()
                        + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                this::mapRun,
                params.toArray()
        );
        return new KnowledgeMaintenanceRunPageResponse(items, normalizedPage, normalizedPageSize, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public KnowledgeMaintenanceItemPageResponse listItems(
            String runUid,
            String issueType,
            String severity,
            String status,
            int page,
            int pageSize
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        QueryParts queryParts = itemQueryParts(runUid, issueType, severity, status);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_maintenance_items" + queryParts.where(),
                Long.class,
                queryParts.params().toArray()
        );
        List<Object> params = new ArrayList<>(queryParts.params());
        params.add(normalizedPageSize);
        params.add((normalizedPage - 1) * normalizedPageSize);
        List<KnowledgeMaintenanceItemResponse> items = jdbcTemplate.query(
                baseItemSelect()
                        + queryParts.where()
                        + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                this::mapItem,
                params.toArray()
        );
        return new KnowledgeMaintenanceItemPageResponse(items, normalizedPage, normalizedPageSize, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public KnowledgeMaintenanceRunResponse getRun(String runUid) {
        List<KnowledgeMaintenanceRunResponse> runs = jdbcTemplate.query(
                baseRunSelect() + " WHERE run_uid = ? LIMIT 1",
                this::mapRun,
                runUid
        );
        if (runs.isEmpty()) {
            throw new BusinessException(ErrorCode.MAINTENANCE_RUN_NOT_FOUND);
        }
        return runs.get(0);
    }

    @Transactional
    public KnowledgeMaintenanceItemResponse updateItemStatus(
            String itemUid,
            UpdateKnowledgeMaintenanceItemStatusRequest request
    ) {
        String normalizedItemUid = normalizeRequired(itemUid, "itemUid is required");
        String status = normalizeRequired(request == null ? null : request.status(), "status is required")
                .toLowerCase(Locale.ROOT);
        if (!ITEM_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.MAINTENANCE_INVALID_INPUT, "status must be open, resolved or ignored");
        }

        LocalDateTime now = LocalDateTime.now();
        int updatedRows;
        if ("open".equals(status)) {
            updatedRows = jdbcTemplate.update("""
                    UPDATE knowledge_maintenance_items
                    SET status = 'open',
                        resolution_note = NULL,
                        resolved_by = NULL,
                        resolved_at = NULL,
                        updated_at = ?
                    WHERE item_uid = ?
                    """,
                    now,
                    normalizedItemUid
            );
        } else {
            String resolutionNote = normalizeOptional(request.resolutionNote());
            String resolvedBy = firstText(request.resolvedBy(), "web-ui");
            updatedRows = jdbcTemplate.update("""
                    UPDATE knowledge_maintenance_items
                    SET status = ?,
                        resolution_note = ?,
                        resolved_by = ?,
                        resolved_at = ?,
                        updated_at = ?
                    WHERE item_uid = ?
                    """,
                    status,
                    resolutionNote,
                    resolvedBy,
                    now,
                    now,
                    normalizedItemUid
            );
        }
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.MAINTENANCE_ITEM_NOT_FOUND);
        }
        return getItem(normalizedItemUid);
    }

    @Transactional(readOnly = true)
    public KnowledgeMaintenanceItemResponse getItem(String itemUid) {
        List<KnowledgeMaintenanceItemResponse> items = jdbcTemplate.query(
                baseItemSelect() + " WHERE item_uid = ? LIMIT 1",
                this::mapItem,
                itemUid
        );
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.MAINTENANCE_ITEM_NOT_FOUND);
        }
        return items.get(0);
    }

    private void addMissingSourceContentIssues(
            List<MaintenanceItemRow> items,
            String runUid,
            int limit,
            LocalDateTime now
    ) {
        List<SourceContentCandidate> candidates = jdbcTemplate.query("""
                SELECT sc.content_uid, sc.raw_text, s.source_uid, sf.file_uid,
                    COALESCE(s.title, sf.file_name) AS title
                FROM source_contents sc
                JOIN sources s ON s.id = sc.source_id
                JOIN source_files sf ON sf.id = sc.source_file_id
                ORDER BY sc.created_at DESC, sc.id DESC
                LIMIT ?
                """,
                this::mapSourceContentCandidate,
                limit
        );
        for (SourceContentCandidate candidate : candidates) {
            if (normalizeOptional(candidate.rawText()) != null) {
                continue;
            }
            items.add(newItem(
                    runUid,
                    "missing_source_content",
                    "high",
                    "source_content",
                    candidate.sourceUid(),
                    candidate.fileUid(),
                    null,
                    null,
                    null,
                    candidate.title(),
                    "Source Content 正文为空，后续摘要、Wiki 编译和向量导出会缺少材料。",
                    Map.of("contentUid", candidate.contentUid()),
                    now
            ));
        }
    }

    private void addDuplicateSourceContentIssues(
            List<MaintenanceItemRow> items,
            String runUid,
            int limit,
            LocalDateTime now
    ) {
        List<DuplicateSourceContentCandidate> candidates = jdbcTemplate.query("""
                SELECT sc.text_hash, COUNT(*) AS duplicate_count,
                    MIN(sc.content_uid) AS sample_content_uid,
                    MIN(s.source_uid) AS sample_source_uid,
                    MIN(sf.file_uid) AS sample_file_uid,
                    MIN(COALESCE(s.title, sf.file_name)) AS sample_title
                FROM source_contents sc
                JOIN sources s ON s.id = sc.source_id
                JOIN source_files sf ON sf.id = sc.source_file_id
                WHERE sc.text_hash IS NOT NULL AND sc.text_hash <> ''
                GROUP BY sc.text_hash
                HAVING COUNT(*) > 1
                ORDER BY COUNT(*) DESC, sc.text_hash
                LIMIT ?
                """,
                this::mapDuplicateSourceContentCandidate,
                limit
        );
        for (DuplicateSourceContentCandidate candidate : candidates) {
            items.add(newItem(
                    runUid,
                    "duplicate_source_content",
                    "medium",
                    "source_content",
                    candidate.sampleSourceUid(),
                    candidate.sampleFileUid(),
                    null,
                    null,
                    null,
                    firstText(candidate.sampleTitle(), "重复 Source Content"),
                    "发现相同 text_hash 的 Source Content，可后续人工合并或标记重复。",
                    Map.of(
                            "textHash", candidate.textHash(),
                            "duplicateCount", candidate.duplicateCount(),
                            "sampleContentUid", candidate.sampleContentUid()
                    ),
                    now
            ));
        }
    }

    private void addUnarchivedPersonalRecordIssues(
            List<MaintenanceItemRow> items,
            String runUid,
            LocalDateTime cutoff,
            int limit,
            LocalDateTime now
    ) {
        List<PersonalRecordCandidate> candidates = jdbcTemplate.query("""
                SELECT record_uid, record_type, title, created_at
                FROM personal_records
                WHERE archived_at IS NULL AND created_at < ?
                ORDER BY created_at ASC, id ASC
                LIMIT ?
                """,
                this::mapPersonalRecordCandidate,
                cutoff,
                limit
        );
        for (PersonalRecordCandidate candidate : candidates) {
            items.add(newItem(
                    runUid,
                    "unarchived_personal_record",
                    "medium",
                    "personal_record",
                    null,
                    null,
                    candidate.recordUid(),
                    null,
                    null,
                    candidate.title(),
                    "Personal Record 尚未归档到 Obsidian，可能还停留在收集层。",
                    Map.of(
                            "recordType", candidate.recordType(),
                            "createdAt", candidate.createdAt() == null ? "" : candidate.createdAt().toString()
                    ),
                    now
            ));
        }
    }

    private void addEmptyVectorExportIssues(
            List<MaintenanceItemRow> items,
            String runUid,
            int limit,
            LocalDateTime now
    ) {
        List<VectorExportCandidate> candidates = jdbcTemplate.query("""
                SELECT export_uid, scope, target_collection, created_at
                FROM vector_export_jobs
                WHERE status = 'completed' AND total_count = 0
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """,
                this::mapVectorExportCandidate,
                limit
        );
        for (VectorExportCandidate candidate : candidates) {
            items.add(newItem(
                    runUid,
                    "empty_vector_export",
                    "low",
                    "vector_export",
                    null,
                    null,
                    null,
                    null,
                    candidate.exportUid(),
                    candidate.targetCollection(),
                    "Vector Export 已完成但 chunk 数为 0，可能没有可导出的正文。",
                    Map.of(
                            "scope", candidate.scope(),
                            "targetCollection", candidate.targetCollection(),
                            "createdAt", candidate.createdAt() == null ? "" : candidate.createdAt().toString()
                    ),
                    now
            ));
        }
    }

    private void addStaleVectorChunkIssues(
            List<MaintenanceItemRow> items,
            String runUid,
            LocalDateTime cutoff,
            int limit,
            LocalDateTime now
    ) {
        List<VectorChunkCandidate> candidates = jdbcTemplate.query("""
                SELECT chunk_uid, export_uid, content_type, source_uid, file_uid, record_uid,
                    title, target_collection, created_at
                FROM content_chunks
                WHERE embedding_status = 'pending' AND created_at < ?
                ORDER BY created_at ASC, id ASC
                LIMIT ?
                """,
                this::mapVectorChunkCandidate,
                cutoff,
                limit
        );
        for (VectorChunkCandidate candidate : candidates) {
            items.add(newItem(
                    runUid,
                    "stale_vector_chunk",
                    "medium",
                    candidate.contentType(),
                    candidate.sourceUid(),
                    candidate.fileUid(),
                    candidate.recordUid(),
                    candidate.chunkUid(),
                    candidate.exportUid(),
                    candidate.title(),
                    "Content Chunk 长时间处于 pending，后续接入向量库后需要补偿导入。",
                    Map.of(
                            "targetCollection", candidate.targetCollection(),
                            "createdAt", candidate.createdAt() == null ? "" : candidate.createdAt().toString()
                    ),
                    now
            ));
        }
    }

    private QueryParts runQueryParts(String status) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        String normalizedStatus = normalizeOptional(status);
        if (normalizedStatus != null) {
            where.append(" AND status = ?");
            params.add(normalizedStatus);
        }
        return new QueryParts(where.toString(), params);
    }

    private QueryParts itemQueryParts(String runUid, String issueType, String severity, String status) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        addEquals(where, params, "run_uid", runUid);
        addEquals(where, params, "issue_type", issueType);
        addEquals(where, params, "severity", severity);
        addEquals(where, params, "status", status);
        return new QueryParts(where.toString(), params);
    }

    private void addEquals(StringBuilder where, List<Object> params, String column, String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return;
        }
        where.append(" AND ").append(column).append(" = ?");
        params.add(normalized);
    }

    private MaintenanceItemRow newItem(
            String runUid,
            String issueType,
            String severity,
            String contentType,
            String sourceUid,
            String fileUid,
            String recordUid,
            String chunkUid,
            String exportUid,
            String title,
            String summary,
            Map<String, Object> evidence,
            LocalDateTime now
    ) {
        return new MaintenanceItemRow(
                nextUid("maint_item"),
                runUid,
                issueType,
                severity,
                contentType,
                sourceUid,
                fileUid,
                recordUid,
                chunkUid,
                exportUid,
                title,
                summary,
                toJson(evidence)
        );
    }

    private String baseRunSelect() {
        return """
                SELECT run_uid, run_type, status, stale_days, total_count, issue_count,
                    started_at, finished_at, created_at, error_message
                FROM knowledge_maintenance_runs
                """;
    }

    private String baseItemSelect() {
        return """
                SELECT item_uid, run_uid, issue_type, severity, content_type,
                    source_uid, file_uid, record_uid, chunk_uid, export_uid,
                    title, summary, evidence_json, status,
                    resolution_note, resolved_by, resolved_at,
                    created_at, updated_at
                FROM knowledge_maintenance_items
                """;
    }

    private KnowledgeMaintenanceRunResponse mapRun(ResultSet rs, int rowNum) throws SQLException {
        return new KnowledgeMaintenanceRunResponse(
                rs.getString("run_uid"),
                rs.getString("run_type"),
                rs.getString("status"),
                rs.getInt("stale_days"),
                rs.getInt("total_count"),
                rs.getInt("issue_count"),
                toOffset(rs.getTimestamp("started_at")),
                toOffset(rs.getTimestamp("finished_at")),
                toOffset(rs.getTimestamp("created_at")),
                rs.getString("error_message")
        );
    }

    private KnowledgeMaintenanceItemResponse mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new KnowledgeMaintenanceItemResponse(
                rs.getString("item_uid"),
                rs.getString("run_uid"),
                rs.getString("issue_type"),
                rs.getString("severity"),
                rs.getString("content_type"),
                rs.getString("source_uid"),
                rs.getString("file_uid"),
                rs.getString("record_uid"),
                rs.getString("chunk_uid"),
                rs.getString("export_uid"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("evidence_json"),
                rs.getString("status"),
                rs.getString("resolution_note"),
                rs.getString("resolved_by"),
                toOffset(rs.getTimestamp("resolved_at")),
                toOffset(rs.getTimestamp("created_at")),
                toOffset(rs.getTimestamp("updated_at"))
        );
    }

    private SourceContentCandidate mapSourceContentCandidate(ResultSet rs, int rowNum) throws SQLException {
        return new SourceContentCandidate(
                rs.getString("content_uid"),
                rs.getString("raw_text"),
                rs.getString("source_uid"),
                rs.getString("file_uid"),
                rs.getString("title")
        );
    }

    private DuplicateSourceContentCandidate mapDuplicateSourceContentCandidate(ResultSet rs, int rowNum) throws SQLException {
        return new DuplicateSourceContentCandidate(
                rs.getString("text_hash"),
                rs.getInt("duplicate_count"),
                rs.getString("sample_content_uid"),
                rs.getString("sample_source_uid"),
                rs.getString("sample_file_uid"),
                rs.getString("sample_title")
        );
    }

    private PersonalRecordCandidate mapPersonalRecordCandidate(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new PersonalRecordCandidate(
                rs.getString("record_uid"),
                rs.getString("record_type"),
                rs.getString("title"),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }

    private VectorExportCandidate mapVectorExportCandidate(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new VectorExportCandidate(
                rs.getString("export_uid"),
                rs.getString("scope"),
                rs.getString("target_collection"),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }

    private VectorChunkCandidate mapVectorChunkCandidate(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new VectorChunkCandidate(
                rs.getString("chunk_uid"),
                rs.getString("export_uid"),
                rs.getString("content_type"),
                rs.getString("source_uid"),
                rs.getString("file_uid"),
                rs.getString("record_uid"),
                rs.getString("title"),
                rs.getString("target_collection"),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }

    private int normalizeStaleDays(Integer value) {
        if (value == null || value <= 0) {
            return DEFAULT_STALE_DAYS;
        }
        if (value > MAX_STALE_DAYS) {
            throw new BusinessException(ErrorCode.MAINTENANCE_INVALID_INPUT, "staleDays must be 1-365");
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.MAINTENANCE_FAILED, "maintenance evidence cannot be serialized");
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

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.MAINTENANCE_INVALID_INPUT, message);
        }
        return normalized;
    }

    private OffsetDateTime toOffset(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private record QueryParts(String where, List<Object> params) {
    }

    private record MaintenanceItemRow(
            String itemUid,
            String runUid,
            String issueType,
            String severity,
            String contentType,
            String sourceUid,
            String fileUid,
            String recordUid,
            String chunkUid,
            String exportUid,
            String title,
            String summary,
            String evidenceJson
    ) {
    }

    private record SourceContentCandidate(
            String contentUid,
            String rawText,
            String sourceUid,
            String fileUid,
            String title
    ) {
    }

    private record DuplicateSourceContentCandidate(
            String textHash,
            int duplicateCount,
            String sampleContentUid,
            String sampleSourceUid,
            String sampleFileUid,
            String sampleTitle
    ) {
    }

    private record PersonalRecordCandidate(
            String recordUid,
            String recordType,
            String title,
            LocalDateTime createdAt
    ) {
    }

    private record VectorExportCandidate(
            String exportUid,
            String scope,
            String targetCollection,
            LocalDateTime createdAt
    ) {
    }

    private record VectorChunkCandidate(
            String chunkUid,
            String exportUid,
            String contentType,
            String sourceUid,
            String fileUid,
            String recordUid,
            String title,
            String targetCollection,
            LocalDateTime createdAt
    ) {
    }
}
