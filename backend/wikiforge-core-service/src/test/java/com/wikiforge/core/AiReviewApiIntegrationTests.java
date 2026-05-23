package com.wikiforge.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.wikiforge.core.application.port.WorkerImportJobClient;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
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
class AiReviewApiIntegrationTests {

    private static final Path TEST_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "wikiforge-ai-review-test-" + UUID.randomUUID()
    ).toAbsolutePath().normalize();
    private static final Path RAW_SOURCES_ROOT = TEST_ROOT.resolve("raw-sources").normalize();

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
    }

    @BeforeEach
    void prepareSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS review_items");
        jdbcTemplate.execute("DROP TABLE IF EXISTS agent_steps");
        jdbcTemplate.execute("DROP TABLE IF EXISTS agent_runs");
        jdbcTemplate.execute("DROP TABLE IF EXISTS source_contents");
        jdbcTemplate.execute("DROP TABLE IF EXISTS source_files");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sources");
        jdbcTemplate.execute("DROP TABLE IF EXISTS import_jobs");
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
                    error_message CLOB NULL,
                    started_at TIMESTAMP NULL,
                    finished_at TIMESTAMP NULL,
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
                    parse_error CLOB NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_source_contents_content_uid (content_uid),
                    UNIQUE KEY uk_source_contents_source_file (source_file_id)
                )
                """);
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
                    model_provider VARCHAR(128) NULL,
                    model_name VARCHAR(128) NULL,
                    prompt_version VARCHAR(64) NULL,
                    error_message CLOB NULL,
                    started_at TIMESTAMP NULL,
                    finished_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_steps_step_uid (step_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE review_items (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    review_uid VARCHAR(64) NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_file_id BIGINT NULL,
                    run_id BIGINT NOT NULL,
                    review_type VARCHAR(64) NOT NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    reason CLOB NULL,
                    suggested_changes_json CLOB NULL,
                    markdown_draft CLOB NULL,
                    user_decision CLOB NULL,
                    reviewed_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_review_items_review_uid (review_uid)
                )
                """);
        seedSourceContent();
    }

    @Test
    void createAiReviewRunCreatesLedgerAndPendingReviewItem() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/ai-review-runs",
                Map.of("providerName", "rule-based", "modelName", "wikiforge-local-rules"),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("runUid").asText()).startsWith("run_");
        assertThat(data.path("status").asText()).isEqualTo("completed");
        assertThat(data.path("reviewItemUid").asText()).startsWith("review_");
        assertThat(data.path("reviewStatus").asText()).isEqualTo("pending");

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_runs", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_steps", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM review_items", Integer.class)).isEqualTo(1);
        String suggestedChanges = jdbcTemplate.queryForObject(
                "SELECT suggested_changes_json FROM review_items",
                String.class
        );
        assertThat(suggestedChanges).contains("WikiForge 是一个本地优先知识系统");
    }

    @Test
    void listPendingReviewItemsReturnsAiDraft() {
        restTemplate.postForEntity(
                "/api/v1/source-files/file_test/ai-review-runs",
                Map.of("providerName", "rule-based", "modelName", "wikiforge-local-rules"),
                JsonNode.class
        );

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/review-items?status=pending&page=1&pageSize=20",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode page = response.getBody().path("data");
        assertThat(page.path("total").asInt()).isEqualTo(1);
        JsonNode item = page.path("items").get(0);
        assertThat(item.path("reviewUid").asText()).startsWith("review_");
        assertThat(item.path("sourceFileUid").asText()).isEqualTo("file_test");
        assertThat(item.path("reviewType").asText()).isEqualTo("ai整理建议");
        assertThat(item.path("status").asText()).isEqualTo("pending");
        assertThat(item.path("suggestedChanges").asText()).contains("WikiForge 是一个本地优先知识系统");
    }

    @Test
    void minimaxWithoutConfiguredModelFallsBackToLocalRulesWithoutNetworkCall() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/ai-review-runs",
                Map.of("providerName", "minimax", "configSource", "env"),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("modelProvider").asText()).isEqualTo("minimax");
        assertThat(data.path("modelName").isMissingNode() || data.path("modelName").isNull()).isTrue();

        String suggestedChanges = jdbcTemplate.queryForObject(
                "SELECT suggested_changes_json FROM review_items",
                String.class
        );
        assertThat(suggestedChanges)
                .contains("WikiForge 是一个本地优先知识系统")
                .contains("MiniMax 未配置密钥或模型");
    }

    @Test
    void openAiCompatibleProviderCanBeSelectedByConfiguration() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/ai-review-runs",
                Map.of(
                        "providerName", "deepseek",
                        "providerType", "openai_compatible",
                        "modelName", "deepseek-chat",
                        "baseUrl", "http://127.0.0.1:9/v1",
                        "configSource", "request"
                ),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("modelProvider").asText()).isEqualTo("deepseek");
        assertThat(data.path("modelName").asText()).isEqualTo("deepseek-chat");

        String suggestedChanges = jdbcTemplate.queryForObject(
                "SELECT suggested_changes_json FROM review_items",
                String.class
        );
        assertThat(suggestedChanges)
                .contains("WikiForge 是一个本地优先知识系统")
                .contains("deepseek 未配置密钥");
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
                    raw_managed_path, raw_organize_status, content_hash,
                    status, collected_at, created_at, updated_at
                ) VALUES (
                    100, 'src_test', 'wikiforge.md', 'md', 'local', ?,
                    ?, 'copied', 'hash-test',
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
}
