package com.wikiforge.core.application.service;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.common.filesystem.PathSafety;
import com.wikiforge.core.application.dto.CreateWikiCompileRunRequest;
import com.wikiforge.core.application.dto.CreateWikiPageRequest;
import com.wikiforge.core.application.dto.WikiCompileRunResponse;
import com.wikiforge.core.application.dto.WikiIntegrationDecisionRequest;
import com.wikiforge.core.application.dto.WikiIntegrationPageResponse;
import com.wikiforge.core.application.dto.WikiIntegrationResponse;
import com.wikiforge.core.application.dto.WikiPagePageResponse;
import com.wikiforge.core.application.dto.WikiPageResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiCompileService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String PIPELINE_VERSION = "wiki-compile-v1";
    private static final BigDecimal AUTO_CONFIDENCE = new BigDecimal("0.75");
    private static final Set<String> PAGE_TYPES = Set.of("topic", "project");
    private static final Set<String> PAGE_STATUSES = Set.of("active", "paused", "archived");
    private static final Set<String> RISK_LEVELS = Set.of("low", "medium", "high");

    private final JdbcTemplate jdbcTemplate;
    private final CoreRuntimeProperties runtimeProperties;

    public WikiCompileService(JdbcTemplate jdbcTemplate, CoreRuntimeProperties runtimeProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.runtimeProperties = runtimeProperties;
    }

    @Transactional
    public WikiPageResponse createPage(CreateWikiPageRequest request) {
        String pageType = normalizePageType(request == null ? null : request.pageType());
        String title = required(request == null ? null : request.title(), "title");
        String slug = normalizeSlug(request == null ? null : request.slug(), title);
        String vaultPath = normalizeVaultPath(
                request == null ? null : request.vaultPath(),
                pageType,
                slug
        );
        String status = normalizePageStatus(request == null ? null : request.status());
        String pageUid = newUid("wiki");
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("""
                INSERT INTO wiki_pages (
                    page_uid, page_type, title, slug, vault_path, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, pageUid, pageType, title, slug, vaultPath, status, now, now);
        ensureWikiPageFile(pageUid, pageType, title, vaultPath);
        return getPage(pageUid);
    }

    @Transactional(readOnly = true)
    public WikiPagePageResponse listPages(String type, String status, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        QueryParts queryParts = pageQueryParts(type, status);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wiki_pages" + queryParts.where(),
                Long.class,
                queryParts.params().toArray()
        );
        List<Object> params = new ArrayList<>(queryParts.params());
        params.add(normalizedPageSize);
        params.add((normalizedPage - 1) * normalizedPageSize);
        List<WikiPageResponse> items = jdbcTemplate.query(
                basePageSelect() + queryParts.where() + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                this::mapPage,
                params.toArray()
        );
        return new WikiPagePageResponse(items, normalizedPage, normalizedPageSize, total == null ? 0 : total);
    }

    @Transactional
    public WikiCompileRunResponse createCompileRun(String fileUid, CreateWikiCompileRunRequest request) {
        SourceRow source = findSourceByFileUid(fileUid);
        WikiPageRow targetPage = findPageByUid(normalizeOptional(request == null ? null : request.targetPageUid()));
        String riskLevel = normalizeRiskLevel(request == null ? null : request.riskLevel());
        BigDecimal confidence = normalizeConfidence(request == null ? null : request.confidenceScore());
        String changeSummary = firstText(
                request == null ? null : request.changeSummary(),
                "补充 " + firstText(source.title(), source.fileName(), source.sourceUid()) + " 的知识更新"
        );
        String proposedMarkdown = firstText(
                request == null ? null : request.proposedMarkdown(),
                proposedMarkdown(source, changeSummary, confidence)
        );
        boolean autoApply = canAutoApply(source, targetPage, riskLevel, confidence);
        String status = autoApply ? "auto_applied" : "pending_review";
        String finalDecision = autoApply ? "auto_applied" : "need_review";
        LocalDateTime now = LocalDateTime.now();
        String runUid = newUid("run");

        jdbcTemplate.update("""
                INSERT INTO agent_runs (
                    run_uid, source_id, source_file_id, run_type, pipeline_version, status,
                    current_step, started_at, finished_at, final_decision, created_at, updated_at
                ) VALUES (?, ?, ?, 'wiki_compile', ?, 'completed', 'integrate', ?, ?, ?, ?, ?)
                """,
                runUid,
                source.sourceId(),
                source.sourceFileId(),
                PIPELINE_VERSION,
                now,
                now,
                finalDecision,
                now,
                now
        );
        Long runId = jdbcTemplate.queryForObject(
                "SELECT id FROM agent_runs WHERE run_uid = ?",
                Long.class,
                runUid
        );

        LocalDateTime appliedAt = null;
        if (autoApply) {
            appendToWikiPage(targetPage, proposedMarkdown);
            appliedAt = now;
        }

        String integrationUid = newUid("wint");
        jdbcTemplate.update("""
                INSERT INTO wiki_integrations (
                    integration_uid, source_id, source_file_id, wiki_page_id, run_id, status,
                    risk_level, confidence_score, change_summary, proposed_markdown, applied_at,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                integrationUid,
                source.sourceId(),
                source.sourceFileId(),
                targetPage == null ? null : targetPage.id(),
                runId,
                status,
                riskLevel,
                confidence,
                changeSummary,
                proposedMarkdown,
                appliedAt,
                now,
                now
        );

        return new WikiCompileRunResponse(runUid, integrationUid, status, finalDecision);
    }

    @Transactional(readOnly = true)
    public WikiIntegrationPageResponse listIntegrations(
            String status,
            String pageUid,
            String sourceUid,
            int page,
            int pageSize
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        QueryParts queryParts = integrationQueryParts(status, pageUid, sourceUid);
        Long total = jdbcTemplate.queryForObject(
                integrationCountSelect() + queryParts.where(),
                Long.class,
                queryParts.params().toArray()
        );
        List<Object> params = new ArrayList<>(queryParts.params());
        params.add(normalizedPageSize);
        params.add((normalizedPage - 1) * normalizedPageSize);
        List<WikiIntegrationResponse> items = jdbcTemplate.query(
                baseIntegrationSelect() + queryParts.where() + " ORDER BY wi.created_at DESC, wi.id DESC LIMIT ? OFFSET ?",
                this::mapIntegration,
                params.toArray()
        );
        return new WikiIntegrationPageResponse(items, normalizedPage, normalizedPageSize, total == null ? 0 : total);
    }

    @Transactional
    public WikiIntegrationResponse approveIntegration(String integrationUid, WikiIntegrationDecisionRequest request) {
        IntegrationRow integration = findIntegration(integrationUid);
        if (!"pending_review".equals(integration.status())) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, "wiki integration is not pending review");
        }
        if (integration.pageUid() == null) {
            throw new BusinessException(ErrorCode.WIKI_PAGE_NOT_FOUND, "target wiki page is required before approval");
        }
        appendToWikiPage(integration.toPageRow(), integration.proposedMarkdown());
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                UPDATE wiki_integrations
                SET status = 'approved', applied_at = ?, updated_at = ?
                WHERE integration_uid = ?
                """, now, now, integrationUid);
        return findIntegrationResponse(integrationUid);
    }

    @Transactional
    public WikiIntegrationResponse rejectIntegration(String integrationUid, WikiIntegrationDecisionRequest request) {
        IntegrationRow integration = findIntegration(integrationUid);
        if (!"pending_review".equals(integration.status())) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, "wiki integration is not pending review");
        }
        jdbcTemplate.update("""
                UPDATE wiki_integrations
                SET status = 'rejected', updated_at = ?
                WHERE integration_uid = ?
                """, LocalDateTime.now(), integrationUid);
        return findIntegrationResponse(integrationUid);
    }

    private WikiPageResponse getPage(String pageUid) {
        List<WikiPageResponse> pages = jdbcTemplate.query(
                basePageSelect() + " WHERE page_uid = ? LIMIT 1",
                this::mapPage,
                pageUid
        );
        if (pages.isEmpty()) {
            throw new BusinessException(ErrorCode.WIKI_PAGE_NOT_FOUND);
        }
        return pages.get(0);
    }

    private SourceRow findSourceByFileUid(String fileUid) {
        List<SourceRow> rows = jdbcTemplate.query("""
                SELECT s.id AS source_id, s.source_uid, s.title, sf.id AS source_file_id,
                    sf.file_uid, sf.file_name, sf.duplicate_of_file_id, sc.raw_text
                FROM source_files sf
                JOIN sources s ON s.id = sf.source_id
                LEFT JOIN source_contents sc ON sc.source_file_id = sf.id
                WHERE sf.file_uid = ?
                LIMIT 1
                """,
                this::mapSourceRow,
                fileUid
        );
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.SOURCE_FILE_NOT_FOUND);
        }
        return rows.get(0);
    }

    private WikiPageRow findPageByUid(String pageUid) {
        if (pageUid == null) {
            return null;
        }
        List<WikiPageRow> rows = jdbcTemplate.query(
                "SELECT id, page_uid, page_type, title, slug, vault_path, status FROM wiki_pages WHERE page_uid = ? LIMIT 1",
                this::mapPageRow,
                pageUid
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private IntegrationRow findIntegration(String integrationUid) {
        List<IntegrationRow> rows = jdbcTemplate.query(
                baseIntegrationSelect() + " WHERE wi.integration_uid = ? LIMIT 1",
                this::mapIntegrationRow,
                integrationUid
        );
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.WIKI_INTEGRATION_NOT_FOUND);
        }
        return rows.get(0);
    }

    private WikiIntegrationResponse findIntegrationResponse(String integrationUid) {
        List<WikiIntegrationResponse> integrations = jdbcTemplate.query(
                baseIntegrationSelect() + " WHERE wi.integration_uid = ? LIMIT 1",
                this::mapIntegration,
                integrationUid
        );
        if (integrations.isEmpty()) {
            throw new BusinessException(ErrorCode.WIKI_INTEGRATION_NOT_FOUND);
        }
        return integrations.get(0);
    }

    private void ensureWikiPageFile(String pageUid, String pageType, String title, String vaultPath) {
        Path target = resolveVaultPath(vaultPath);
        if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        String markdown = """
                ---
                type: %s
                wikiforge_page_uid: %s
                status: active
                ---
                # %s

                ## WikiForge Updates
                """.formatted(pageType, pageUid, title);
        writeAtomically(target, markdown);
    }

    private void appendToWikiPage(WikiPageRow page, String proposedMarkdown) {
        Path target = resolveVaultPath(page.vaultPath());
        String existing = "";
        if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            try {
                existing = Files.readString(target, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "wiki page cannot be read");
            }
        }
        if (existing.isBlank()) {
            existing = "# " + page.title() + "\n";
        }
        if (!existing.contains("## WikiForge Updates")) {
            existing = existing.stripTrailing() + "\n\n## WikiForge Updates\n";
        }
        String updated = existing.stripTrailing() + "\n\n" + proposedMarkdown.strip() + "\n";
        writeAtomically(target, updated);
    }

    private Path resolveVaultPath(String vaultPath) {
        String rootValue = runtimeProperties.obsidianVaultPath();
        if (rootValue == null || rootValue.isBlank()) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian vault path is not configured");
        }
        Path root = PathSafety.normalizeAbsolute(Path.of(rootValue));
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "obsidian vault cannot be created");
        }
        Path relative = Path.of(vaultPath);
        if (relative.isAbsolute()) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "wiki page path must be relative");
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "wiki page path escapes vault");
        }
        return target;
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
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "wiki page cannot be written");
        }
    }

    private boolean canAutoApply(SourceRow source, WikiPageRow targetPage, String riskLevel, BigDecimal confidence) {
        return targetPage != null
                && "active".equals(targetPage.status())
                && "low".equals(riskLevel)
                && confidence.compareTo(AUTO_CONFIDENCE) >= 0
                && source.duplicateOfFileId() == null
                && normalizeOptional(source.rawText()) != null;
    }

    private String proposedMarkdown(SourceRow source, String changeSummary, BigDecimal confidence) {
        String title = firstText(source.title(), source.fileName(), source.sourceUid());
        String excerpt = truncate(firstText(source.rawText(), "该资料暂无可解析正文。"), 360);
        return """
                ### %s

                %s

                %s

                - Source UID: `%s`
                - Source File UID: `%s`
                - Confidence: `%s`
                """.formatted(
                title,
                changeSummary,
                excerpt,
                source.sourceUid(),
                source.fileUid(),
                confidence
        );
    }

    private QueryParts pageQueryParts(String type, String status) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        String normalizedType = normalizeOptional(type);
        if (normalizedType != null) {
            where.append(" AND page_type = ?");
            params.add(normalizedType);
        }
        String normalizedStatus = normalizeOptional(status);
        if (normalizedStatus != null) {
            where.append(" AND status = ?");
            params.add(normalizedStatus);
        }
        return new QueryParts(where.toString(), params);
    }

    private QueryParts integrationQueryParts(String status, String pageUid, String sourceUid) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        String normalizedStatus = normalizeOptional(status);
        if (normalizedStatus != null) {
            where.append(" AND wi.status = ?");
            params.add(normalizedStatus);
        }
        String normalizedPageUid = normalizeOptional(pageUid);
        if (normalizedPageUid != null) {
            where.append(" AND wp.page_uid = ?");
            params.add(normalizedPageUid);
        }
        String normalizedSourceUid = normalizeOptional(sourceUid);
        if (normalizedSourceUid != null) {
            where.append(" AND s.source_uid = ?");
            params.add(normalizedSourceUid);
        }
        return new QueryParts(where.toString(), params);
    }

    private String basePageSelect() {
        return "SELECT page_uid, page_type, title, slug, vault_path, status, created_at FROM wiki_pages";
    }

    private String integrationCountSelect() {
        return """
                SELECT COUNT(*)
                FROM wiki_integrations wi
                JOIN sources s ON s.id = wi.source_id
                LEFT JOIN source_files sf ON sf.id = wi.source_file_id
                LEFT JOIN wiki_pages wp ON wp.id = wi.wiki_page_id
                JOIN agent_runs ar ON ar.id = wi.run_id
                """;
    }

    private String baseIntegrationSelect() {
        return """
                SELECT wi.integration_uid, wp.page_uid, wp.title AS page_title, wp.page_type,
                    wp.vault_path, s.source_uid, sf.file_uid, ar.run_uid, wi.status,
                    wi.risk_level, wi.confidence_score, wi.change_summary, wi.proposed_markdown,
                    wi.applied_at, wi.created_at, wi.wiki_page_id
                FROM wiki_integrations wi
                JOIN sources s ON s.id = wi.source_id
                LEFT JOIN source_files sf ON sf.id = wi.source_file_id
                LEFT JOIN wiki_pages wp ON wp.id = wi.wiki_page_id
                JOIN agent_runs ar ON ar.id = wi.run_id
                """;
    }

    private WikiPageResponse mapPage(ResultSet rs, int rowNum) throws SQLException {
        return new WikiPageResponse(
                rs.getString("page_uid"),
                rs.getString("page_type"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getString("vault_path"),
                rs.getString("status"),
                toOffset(rs.getTimestamp("created_at"))
        );
    }

    private WikiPageRow mapPageRow(ResultSet rs, int rowNum) throws SQLException {
        return new WikiPageRow(
                rs.getLong("id"),
                rs.getString("page_uid"),
                rs.getString("page_type"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getString("vault_path"),
                rs.getString("status")
        );
    }

    private SourceRow mapSourceRow(ResultSet rs, int rowNum) throws SQLException {
        long duplicateId = rs.getLong("duplicate_of_file_id");
        boolean duplicateWasNull = rs.wasNull();
        return new SourceRow(
                rs.getLong("source_id"),
                rs.getString("source_uid"),
                rs.getString("title"),
                rs.getLong("source_file_id"),
                rs.getString("file_uid"),
                rs.getString("file_name"),
                duplicateWasNull ? null : duplicateId,
                rs.getString("raw_text")
        );
    }

    private WikiIntegrationResponse mapIntegration(ResultSet rs, int rowNum) throws SQLException {
        return new WikiIntegrationResponse(
                rs.getString("integration_uid"),
                rs.getString("page_uid"),
                rs.getString("page_title"),
                rs.getString("page_type"),
                rs.getString("vault_path"),
                rs.getString("source_uid"),
                rs.getString("file_uid"),
                rs.getString("run_uid"),
                rs.getString("status"),
                rs.getString("risk_level"),
                rs.getBigDecimal("confidence_score"),
                rs.getString("change_summary"),
                rs.getString("proposed_markdown"),
                toOffset(rs.getTimestamp("applied_at")),
                toOffset(rs.getTimestamp("created_at"))
        );
    }

    private IntegrationRow mapIntegrationRow(ResultSet rs, int rowNum) throws SQLException {
        return new IntegrationRow(
                rs.getString("integration_uid"),
                rs.getString("page_uid"),
                rs.getString("page_title"),
                rs.getString("page_type"),
                rs.getString("vault_path"),
                rs.getString("status"),
                rs.getString("proposed_markdown")
        );
    }

    private String normalizePageType(String value) {
        String normalized = firstText(value, "topic").toLowerCase(Locale.ROOT);
        if (!PAGE_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, "pageType is invalid");
        }
        return normalized;
    }

    private String normalizePageStatus(String value) {
        String normalized = firstText(value, "active").toLowerCase(Locale.ROOT);
        if (!PAGE_STATUSES.contains(normalized)) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, "status is invalid");
        }
        return normalized;
    }

    private String normalizeSlug(String value, String title) {
        String normalized = firstText(value, title)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5_.-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (normalized.isBlank() || normalized.length() > 255) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, "slug is invalid");
        }
        return normalized;
    }

    private String normalizeVaultPath(String value, String pageType, String slug) {
        String defaultDirectory = "project".equals(pageType) ? "20_Projects_项目" : "10_Wiki_主题库";
        String normalized = firstText(value, defaultDirectory + "/" + slug + ".md");
        if (!normalized.endsWith(".md") || normalized.length() > 1024) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, "vaultPath is invalid");
        }
        Path path = Path.of(normalized);
        if (path.isAbsolute() || normalized.contains("..")) {
            throw new BusinessException(ErrorCode.OBSIDIAN_INVALID_VAULT, "wiki page path must stay inside vault");
        }
        return normalized.replace("\\", "/");
    }

    private String normalizeRiskLevel(String value) {
        String normalized = firstText(value, "medium").toLowerCase(Locale.ROOT);
        if (!RISK_LEVELS.contains(normalized)) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, "riskLevel is invalid");
        }
        return normalized;
    }

    private BigDecimal normalizeConfidence(BigDecimal value) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.compareTo(BigDecimal.ZERO) < 0 || normalized.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, "confidenceScore must be 0-1");
        }
        return normalized.setScale(4, RoundingMode.HALF_UP);
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

    private String required(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, fieldName + " is required");
        }
        return normalized;
    }

    private String firstText(String... values) {
        for (String value : values) {
            String normalized = normalizeOptional(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return "";
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        String normalized = firstText(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength).trim() + "...";
    }

    private String newUid(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return prefix + "_" + date + "_" + suffix;
    }

    private OffsetDateTime toOffset(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private record QueryParts(String where, List<Object> params) {
    }

    private record SourceRow(
            Long sourceId,
            String sourceUid,
            String title,
            Long sourceFileId,
            String fileUid,
            String fileName,
            Long duplicateOfFileId,
            String rawText
    ) {
    }

    private record WikiPageRow(
            Long id,
            String pageUid,
            String pageType,
            String title,
            String slug,
            String vaultPath,
            String status
    ) {
    }

    private record IntegrationRow(
            String integrationUid,
            String pageUid,
            String pageTitle,
            String pageType,
            String vaultPath,
            String status,
            String proposedMarkdown
    ) {
        WikiPageRow toPageRow() {
            return new WikiPageRow(null, pageUid, pageType, pageTitle, null, vaultPath, "active");
        }
    }
}
