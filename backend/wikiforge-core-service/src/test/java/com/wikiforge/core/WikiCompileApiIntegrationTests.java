package com.wikiforge.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.wikiforge.core.application.port.WorkerImportJobClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = WikiForgeCoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WikiCompileApiIntegrationTests {

    private static final Path TEST_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "wikiforge-wiki-compile-test-" + UUID.randomUUID()
    ).toAbsolutePath().normalize();
    private static final Path RAW_SOURCES_ROOT = TEST_ROOT.resolve("raw-sources").normalize();
    private static final Path OBSIDIAN_VAULT = TEST_ROOT.resolve("WikiForgeVault").normalize();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private WorkerImportJobClient workerImportJobClient;

    @DynamicPropertySource
    static void coreProperties(DynamicPropertyRegistry registry) {
        registry.add("wikiforge.storage.raw-sources-root", RAW_SOURCES_ROOT::toString);
        registry.add("wikiforge.worker.base-url", () -> "http://test-worker:8081");
        registry.add("wikiforge.security.internal-api-token", () -> "test-token");
        registry.add("wikiforge.obsidian-vault-path", OBSIDIAN_VAULT::toString);
        registry.add("wikiforge.obsidian-vault-name", () -> "WikiForgeVault");
    }

    @BeforeEach
    void prepareSchema() throws Exception {
        deleteDirectory(OBSIDIAN_VAULT);
        Files.createDirectories(RAW_SOURCES_ROOT);
        Files.createDirectories(OBSIDIAN_VAULT);
        jdbcTemplate.execute("DROP TABLE IF EXISTS wiki_integrations");
        jdbcTemplate.execute("DROP TABLE IF EXISTS wiki_pages");
        jdbcTemplate.execute("DROP TABLE IF EXISTS obsidian_notes");
        jdbcTemplate.execute("DROP TABLE IF EXISTS agent_steps");
        jdbcTemplate.execute("DROP TABLE IF EXISTS agent_runs");
        jdbcTemplate.execute("DROP TABLE IF EXISTS source_contents");
        jdbcTemplate.execute("DROP TABLE IF EXISTS source_files");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sources");
        jdbcTemplate.execute("DROP TABLE IF EXISTS import_jobs");
        createSourceTables();
        createAgentTables();
        createWikiTables();
        seedSourceContent();
    }

    @Test
    void createAndListWikiPagesRegistersUserControlledTopicPage() throws Exception {
        ResponseEntity<JsonNode> createResponse = createWikiPage("topic", "知识管理", "knowledge-management");

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode page = createResponse.getBody().path("data");
        assertThat(page.path("pageUid").asText()).startsWith("wiki_");
        assertThat(page.path("pageType").asText()).isEqualTo("topic");
        assertThat(page.path("title").asText()).isEqualTo("知识管理");
        assertThat(page.path("slug").asText()).isEqualTo("knowledge-management");
        assertThat(page.path("vaultPath").asText()).isEqualTo("10_Wiki_主题库/knowledge-management.md");
        assertThat(page.path("status").asText()).isEqualTo("active");
        assertThat(Files.readString(OBSIDIAN_VAULT.resolve("10_Wiki_主题库/knowledge-management.md")))
                .contains("# 知识管理");

        ResponseEntity<JsonNode> listResponse = restTemplate.getForEntity(
                "/api/v1/wiki-pages?type=topic&status=active&page=1&pageSize=20",
                JsonNode.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode list = listResponse.getBody().path("data");
        assertThat(list.path("total").asInt()).isEqualTo(1);
        assertThat(list.path("items").get(0).path("pageUid").asText()).isEqualTo(page.path("pageUid").asText());
    }

    @Test
    void compileLowRiskSourceAutoAppendsToExistingWikiPage() throws Exception {
        String pageUid = createWikiPage("topic", "知识管理", "knowledge-management")
                .getBody().path("data").path("pageUid").asText();

        ResponseEntity<JsonNode> compileResponse = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/wiki-compile-runs",
                Map.of(
                        "targetPageUid", pageUid,
                        "riskLevel", "low",
                        "confidenceScore", 0.82,
                        "changeSummary", "补充 WikiForge 路线"
                ),
                JsonNode.class
        );

        assertThat(compileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = compileResponse.getBody().path("data");
        assertThat(data.path("runUid").asText()).startsWith("run_");
        assertThat(data.path("integrationUid").asText()).startsWith("wint_");
        assertThat(data.path("status").asText()).isEqualTo("auto_applied");
        assertThat(data.path("finalDecision").asText()).isEqualTo("auto_applied");

        Path pagePath = OBSIDIAN_VAULT.resolve("10_Wiki_主题库/knowledge-management.md");
        String markdown = Files.readString(pagePath);
        assertThat(markdown)
                .contains("## WikiForge Updates")
                .contains("### wikiforge.md")
                .contains("WikiForge 是一个本地优先知识系统")
                .contains("Source UID: `src_test`");

        ResponseEntity<JsonNode> listResponse = restTemplate.getForEntity(
                "/api/v1/wiki-integrations?status=auto_applied&page=1&pageSize=20",
                JsonNode.class
        );
        assertThat(listResponse.getBody().path("data").path("total").asInt()).isEqualTo(1);
        assertThat(listResponse.getBody().path("data").path("items").get(0).path("pageUid").asText()).isEqualTo(pageUid);
    }

    @Test
    void compileHighRiskOrMissingTargetStaysPendingReviewWithoutWritingWikiPage() throws Exception {
        String pageUid = createWikiPage("project", "WikiForge", "wikiforge-project")
                .getBody().path("data").path("pageUid").asText();
        Path pagePath = OBSIDIAN_VAULT.resolve("20_Projects_项目/wikiforge-project.md");
        String before = Files.readString(pagePath);

        ResponseEntity<JsonNode> highRiskResponse = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/wiki-compile-runs",
                Map.of(
                        "targetPageUid", pageUid,
                        "riskLevel", "high",
                        "confidenceScore", 0.91,
                        "changeSummary", "高风险内容需要人工确认"
                ),
                JsonNode.class
        );
        ResponseEntity<JsonNode> missingTargetResponse = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/wiki-compile-runs",
                Map.of(
                        "riskLevel", "low",
                        "confidenceScore", 0.86,
                        "changeSummary", "缺少目标页需要人工选择"
                ),
                JsonNode.class
        );

        assertThat(highRiskResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(highRiskResponse.getBody().path("data").path("status").asText()).isEqualTo("pending_review");
        assertThat(missingTargetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(missingTargetResponse.getBody().path("data").path("status").asText()).isEqualTo("pending_review");
        assertThat(Files.readString(pagePath)).isEqualTo(before);
    }

    @Test
    void approvePendingIntegrationWritesWikiPageAndRejectLeavesItUntouched() throws Exception {
        String pageUid = createWikiPage("topic", "AI 工程", "ai-engineering")
                .getBody().path("data").path("pageUid").asText();
        String reviewUid = compilePending(pageUid, "待审核内容");
        String rejectUid = compilePending(pageUid, "拒绝内容不应写入");

        ResponseEntity<JsonNode> approveResponse = restTemplate.postForEntity(
                "/api/v1/wiki-integrations/{integrationUid}/approve",
                Map.of("decisionNote", "人工确认可写入"),
                JsonNode.class,
                reviewUid
        );
        ResponseEntity<JsonNode> rejectResponse = restTemplate.postForEntity(
                "/api/v1/wiki-integrations/{integrationUid}/reject",
                Map.of("decisionNote", "不写入主题页"),
                JsonNode.class,
                rejectUid
        );

        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approveResponse.getBody().path("data").path("status").asText()).isEqualTo("approved");
        assertThat(rejectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejectResponse.getBody().path("data").path("status").asText()).isEqualTo("rejected");

        String markdown = Files.readString(OBSIDIAN_VAULT.resolve("10_Wiki_主题库/ai-engineering.md"));
        assertThat(markdown)
                .contains("待审核内容")
                .doesNotContain("拒绝内容不应写入");
    }

    private String compilePending(String pageUid, String changeSummary) {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/wiki-compile-runs",
                Map.of(
                        "targetPageUid", pageUid,
                        "riskLevel", "medium",
                        "confidenceScore", 0.7,
                        "changeSummary", changeSummary
                ),
                JsonNode.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("data").path("integrationUid").asText();
    }

    private ResponseEntity<JsonNode> createWikiPage(String pageType, String title, String slug) {
        String directory = "project".equals(pageType) ? "20_Projects_项目" : "10_Wiki_主题库";
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/wiki-pages",
                Map.of(
                        "pageType", pageType,
                        "title", title,
                        "slug", slug,
                        "vaultPath", directory + "/" + slug + ".md"
                ),
                JsonNode.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response;
    }

    private void createSourceTables() {
        jdbcTemplate.execute("""
                CREATE TABLE import_jobs (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    job_uid VARCHAR(64) NOT NULL,
                    import_type VARCHAR(64) NOT NULL,
                    input_path CLOB NULL,
                    raw_sources_root CLOB NULL,
                    recursive_scan BOOLEAN NOT NULL DEFAULT TRUE,
                    organize_mode VARCHAR(64) NOT NULL DEFAULT 'copy',
                    max_copy_file_size_mb INT NOT NULL DEFAULT 100,
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    total_count INT NOT NULL DEFAULT 0,
                    success_count INT NOT NULL DEFAULT 0,
                    skipped_count INT NOT NULL DEFAULT 0,
                    failed_count INT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_import_jobs_job_uid (job_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sources (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    source_uid VARCHAR(64) NOT NULL,
                    title VARCHAR(512) NULL,
                    source_type VARCHAR(64) NOT NULL DEFAULT 'file',
                    source_platform VARCHAR(128) NOT NULL DEFAULT 'local',
                    raw_original_path CLOB NULL,
                    raw_managed_path CLOB NULL,
                    raw_organize_status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    processing_intent VARCHAR(64) NOT NULL DEFAULT 'organize_only',
                    content_hash VARCHAR(128) NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    collected_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_sources_source_uid (source_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE source_files (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    file_uid VARCHAR(64) NOT NULL,
                    source_id BIGINT NULL,
                    import_job_id BIGINT NOT NULL,
                    file_name VARCHAR(512) NOT NULL,
                    file_ext VARCHAR(32) NULL,
                    original_path CLOB NOT NULL,
                    managed_path CLOB NULL,
                    file_size BIGINT NOT NULL DEFAULT 0,
                    mime_type VARCHAR(128) NULL,
                    content_hash VARCHAR(128) NULL,
                    parser_name VARCHAR(128) NULL,
                    parse_status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    organize_status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    duplicate_of_file_id BIGINT NULL,
                    parse_error CLOB NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_source_files_file_uid (file_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE source_contents (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    content_uid VARCHAR(64) NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_file_id BIGINT NOT NULL,
                    parser_name VARCHAR(128) NULL,
                    content_type VARCHAR(64) NOT NULL DEFAULT 'plain_text',
                    raw_text CLOB NULL,
                    text_hash VARCHAR(128) NULL,
                    char_count INT NOT NULL DEFAULT 0,
                    raw_text_saved BOOLEAN NOT NULL DEFAULT FALSE,
                    parse_status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_source_contents_content_uid (content_uid),
                    UNIQUE KEY uk_source_contents_source_file (source_file_id)
                )
                """);
    }

    private void createAgentTables() {
        jdbcTemplate.execute("""
                CREATE TABLE agent_runs (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    run_uid VARCHAR(64) NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_file_id BIGINT NULL,
                    run_type VARCHAR(64) NOT NULL,
                    pipeline_version VARCHAR(64) NOT NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    current_step VARCHAR(64) NULL,
                    model_provider VARCHAR(128) NULL,
                    model_name VARCHAR(128) NULL,
                    started_at TIMESTAMP NULL,
                    finished_at TIMESTAMP NULL,
                    final_decision VARCHAR(64) NULL,
                    error_message CLOB NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_runs_run_uid (run_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE agent_steps (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    step_uid VARCHAR(64) NOT NULL,
                    run_id BIGINT NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_file_id BIGINT NULL,
                    step_name VARCHAR(64) NOT NULL,
                    agent_name VARCHAR(128) NOT NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    input_json CLOB NULL,
                    output_json CLOB NULL,
                    prompt_version VARCHAR(64) NULL,
                    started_at TIMESTAMP NULL,
                    finished_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_steps_step_uid (step_uid)
                )
                """);
    }

    private void createWikiTables() {
        jdbcTemplate.execute("""
                CREATE TABLE wiki_pages (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    page_uid VARCHAR(64) NOT NULL,
                    page_type VARCHAR(64) NOT NULL,
                    title VARCHAR(512) NOT NULL,
                    slug VARCHAR(255) NOT NULL,
                    vault_path VARCHAR(1024) NOT NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'active',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_wiki_pages_page_uid (page_uid),
                    UNIQUE KEY uk_wiki_pages_slug (slug)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE wiki_integrations (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    integration_uid VARCHAR(64) NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_file_id BIGINT NULL,
                    wiki_page_id BIGINT NULL,
                    run_id BIGINT NOT NULL,
                    status VARCHAR(64) NOT NULL,
                    risk_level VARCHAR(32) NOT NULL,
                    confidence_score DECIMAL(5,4) NOT NULL DEFAULT 0,
                    change_summary CLOB NULL,
                    proposed_markdown CLOB NULL,
                    applied_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_wiki_integrations_uid (integration_uid)
                )
                """);
    }

    private void seedSourceContent() {
        jdbcTemplate.update("""
                INSERT INTO import_jobs (
                    id, job_uid, import_type, input_path, raw_sources_root, recursive_scan,
                    organize_mode, max_copy_file_size_mb, status, total_count, success_count,
                    skipped_count, failed_count, created_at, updated_at
                ) VALUES (
                    10, 'job_test', 'path_scan', ?, ?, TRUE,
                    'copy', 100, 'completed', 1, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """, TEST_ROOT.resolve("input").toString(), RAW_SOURCES_ROOT.toString());
        jdbcTemplate.update("""
                INSERT INTO sources (
                    id, source_uid, title, source_type, source_platform, raw_original_path,
                    raw_managed_path, raw_organize_status, processing_intent, content_hash,
                    status, collected_at, created_at, updated_at
                ) VALUES (
                    100, 'src_test', 'wikiforge.md', 'md', 'local', ?,
                    ?, 'copied', 'extract_and_review', 'hash-test',
                    'organized', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """, TEST_ROOT.resolve("input/wikiforge.md").toString(), RAW_SOURCES_ROOT.resolve("wikiforge.md").toString());
        jdbcTemplate.update("""
                INSERT INTO source_files (
                    id, file_uid, source_id, import_job_id, file_name, file_ext, original_path,
                    managed_path, file_size, mime_type, content_hash, parse_status,
                    organize_status, created_at
                ) VALUES (
                    200, 'file_test', 100, 10, 'wikiforge.md', 'md', ?,
                    ?, 1024, 'text/markdown', 'hash-test', 'success',
                    'copied', CURRENT_TIMESTAMP
                )
                """, TEST_ROOT.resolve("input/wikiforge.md").toString(), RAW_SOURCES_ROOT.resolve("wikiforge.md").toString());
        jdbcTemplate.update("""
                INSERT INTO source_contents (
                    id, content_uid, source_id, source_file_id, parser_name, content_type,
                    raw_text, text_hash, char_count, raw_text_saved, parse_status,
                    created_at, updated_at
                ) VALUES (
                    300, 'content_test', 100, 200, 'markdown-text', 'plain_text',
                    'WikiForge 是一个本地优先知识系统。它先整理资料，再进入 AI 提炼和审核。', 'text-hash', 40, TRUE, 'success',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
    }

    private void deleteDirectory(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }
}
