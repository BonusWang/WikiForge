package com.wikiforge.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.wikiforge.core.application.port.WorkerImportJobClient;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = WikiForgeCoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KnowledgeMaintenanceApiIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private WorkerImportJobClient workerImportJobClient;

    @BeforeEach
    void prepareSchema() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        jdbcTemplate.execute("DROP TABLE IF EXISTS knowledge_maintenance_items");
        jdbcTemplate.execute("DROP TABLE IF EXISTS knowledge_maintenance_runs");
        jdbcTemplate.execute("DROP TABLE IF EXISTS personal_records");
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
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
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
                    source_url CLOB NULL,
                    local_path CLOB NULL,
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
                    parse_error CLOB NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_source_contents_content_uid (content_uid),
                    UNIQUE KEY uk_source_contents_source_file (source_file_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE personal_records (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    record_uid VARCHAR(64) NOT NULL,
                    record_type VARCHAR(64) NOT NULL,
                    title VARCHAR(512) NOT NULL,
                    occurred_at TIMESTAMP NULL,
                    source_channel VARCHAR(128) NOT NULL DEFAULT 'mcp',
                    source_ref CLOB NULL,
                    raw_content CLOB NOT NULL,
                    structured_json CLOB NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    sensitivity_level VARCHAR(32) NOT NULL DEFAULT 'medium',
                    created_by VARCHAR(128) NOT NULL,
                    obsidian_vault_path CLOB NULL,
                    obsidian_uri CLOB NULL,
                    archived_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_personal_records_record_uid (record_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE knowledge_maintenance_runs (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    run_uid VARCHAR(64) NOT NULL,
                    run_type VARCHAR(64) NOT NULL DEFAULT 'manual',
                    status VARCHAR(64) NOT NULL DEFAULT 'completed',
                    stale_days INT NOT NULL DEFAULT 7,
                    total_count INT NOT NULL DEFAULT 0,
                    issue_count INT NOT NULL DEFAULT 0,
                    error_message CLOB NULL,
                    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    finished_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_maintenance_runs_run_uid (run_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE knowledge_maintenance_items (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    item_uid VARCHAR(64) NOT NULL,
                    run_uid VARCHAR(64) NOT NULL,
                    issue_type VARCHAR(64) NOT NULL,
                    severity VARCHAR(32) NOT NULL,
                    content_type VARCHAR(64) NOT NULL,
                    source_uid VARCHAR(64) NULL,
                    file_uid VARCHAR(64) NULL,
                    record_uid VARCHAR(64) NULL,
                    chunk_uid VARCHAR(64) NULL,
                    export_uid VARCHAR(64) NULL,
                    title VARCHAR(512) NULL,
                    summary CLOB NOT NULL,
                    evidence_json CLOB NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'open',
                    resolution_note CLOB NULL,
                    resolved_by VARCHAR(128) NULL,
                    resolved_at TIMESTAMP NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_maintenance_items_item_uid (item_uid)
                )
                """);
        seedRows();
    }

    @Test
    void createRunFindsKnowledgeMaintenanceIssues() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/maintenance-runs",
                Map.of("staleDays", 1, "limit", 50),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("runUid").asText()).startsWith("maint_");
        assertThat(data.path("status").asText()).isEqualTo("completed");
        assertThat(data.path("staleDays").asInt()).isEqualTo(1);
        assertThat(data.path("issueCount").asInt()).isEqualTo(3);

        Integer itemRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_maintenance_items WHERE run_uid = ? AND status = 'open'",
                Integer.class,
                data.path("runUid").asText()
        );
        assertThat(itemRows).isEqualTo(3);
    }

    @Test
    void listItemsCanFilterByRunAndIssueType() {
        String runUid = restTemplate.postForEntity(
                "/api/v1/maintenance-runs",
                Map.of("staleDays", 1, "limit", 50),
                JsonNode.class
        ).getBody().path("data").path("runUid").asText();

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/maintenance-items?runUid={runUid}&page=1&pageSize=20",
                JsonNode.class,
                runUid
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode items = response.getBody().path("data").path("items");
        assertThat(items).hasSize(3);
        Set<String> issueTypes = new HashSet<>();
        for (JsonNode item : items) {
            issueTypes.add(item.path("issueType").asText());
            assertThat(item.path("itemUid").asText()).startsWith("maint_item_");
            assertThat(item.path("status").asText()).isEqualTo("open");
            assertThat(item.path("summary").asText()).isNotBlank();
        }
        assertThat(issueTypes).containsExactlyInAnyOrder(
                "missing_source_content",
                "duplicate_source_content",
                "unarchived_personal_record"
        );

        ResponseEntity<JsonNode> duplicateResponse = restTemplate.getForEntity(
                "/api/v1/maintenance-items?runUid={runUid}&issueType=duplicate_source_content&page=1&pageSize=20",
                JsonNode.class,
                runUid
        );

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode duplicateData = duplicateResponse.getBody().path("data");
        assertThat(duplicateData.path("total").asLong()).isEqualTo(1);
        assertThat(duplicateData.path("items").get(0).path("evidenceJson").asText()).contains("duplicate-hash");
    }

    @Test
    void listRunsReturnsLatestMaintenanceRun() {
        String runUid = restTemplate.postForEntity(
                "/api/v1/maintenance-runs",
                Map.of("staleDays", 1, "limit", 50),
                JsonNode.class
        ).getBody().path("data").path("runUid").asText();

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/maintenance-runs?page=1&pageSize=10",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("total").asLong()).isEqualTo(1);
        assertThat(data.path("items").get(0).path("runUid").asText()).isEqualTo(runUid);
        assertThat(data.path("items").get(0).path("issueCount").asInt()).isEqualTo(3);
    }

    @Test
    void updateItemStatusCanResolveAndReopenMaintenanceIssue() {
        String runUid = restTemplate.postForEntity(
                "/api/v1/maintenance-runs",
                Map.of("staleDays", 1, "limit", 50),
                JsonNode.class
        ).getBody().path("data").path("runUid").asText();
        String itemUid = firstMaintenanceItemUid(runUid);

        ResponseEntity<JsonNode> resolvedResponse = restTemplate.exchange(
                "/api/v1/maintenance-items/{itemUid}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of(
                        "status", "resolved",
                        "resolutionNote", "已人工确认完成处理",
                        "resolvedBy", "web-ui"
                )),
                JsonNode.class,
                itemUid
        );

        assertThat(resolvedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode resolved = resolvedResponse.getBody().path("data");
        assertThat(resolved.path("itemUid").asText()).isEqualTo(itemUid);
        assertThat(resolved.path("status").asText()).isEqualTo("resolved");
        assertThat(resolved.path("resolutionNote").asText()).isEqualTo("已人工确认完成处理");
        assertThat(resolved.path("resolvedBy").asText()).isEqualTo("web-ui");
        assertThat(resolved.path("resolvedAt").asText()).isNotBlank();

        Integer resolvedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_maintenance_items WHERE run_uid = ? AND status = 'resolved' AND resolved_at IS NOT NULL",
                Integer.class,
                runUid
        );
        assertThat(resolvedCount).isEqualTo(1);

        ResponseEntity<JsonNode> openResponse = restTemplate.exchange(
                "/api/v1/maintenance-items/{itemUid}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "open")),
                JsonNode.class,
                itemUid
        );

        assertThat(openResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode opened = openResponse.getBody().path("data");
        assertThat(opened.path("status").asText()).isEqualTo("open");
        assertThat(opened.path("resolutionNote").isNull()).isTrue();
        assertThat(opened.path("resolvedBy").isNull()).isTrue();
        assertThat(opened.path("resolvedAt").isNull()).isTrue();
    }

    @Test
    void updateItemStatusRejectsInvalidStatusAndMissingItem() {
        String runUid = restTemplate.postForEntity(
                "/api/v1/maintenance-runs",
                Map.of("staleDays", 1, "limit", 50),
                JsonNode.class
        ).getBody().path("data").path("runUid").asText();
        String itemUid = firstMaintenanceItemUid(runUid);

        ResponseEntity<JsonNode> invalidStatusResponse = restTemplate.exchange(
                "/api/v1/maintenance-items/{itemUid}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "closed")),
                JsonNode.class,
                itemUid
        );

        assertThat(invalidStatusResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidStatusResponse.getBody().path("code").asText()).isEqualTo("MAINTENANCE_001");

        ResponseEntity<JsonNode> missingItemResponse = restTemplate.exchange(
                "/api/v1/maintenance-items/{itemUid}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "resolved")),
                JsonNode.class,
                "maint_item_missing"
        );

        assertThat(missingItemResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingItemResponse.getBody().path("code").asText()).isEqualTo("MAINTENANCE_004");
    }

    private void seedRows() {
        jdbcTemplate.update("""
                INSERT INTO import_jobs (id, job_uid, import_type, input_path, raw_sources_root)
                VALUES (1, 'job_maintenance_seed', 'local_path', 'E:/seed', 'E:/raw')
                """);
        jdbcTemplate.update("""
                INSERT INTO sources (id, source_uid, title, source_type, source_platform, source_url, processing_intent)
                VALUES
                    (1, 'src_missing', '空内容资料', 'file', 'local', NULL, 'organize_only'),
                    (2, 'src_dup_a', '重复资料 A', 'file', 'local', NULL, 'organize_only'),
                    (3, 'src_dup_b', '重复资料 B', 'file', 'local', NULL, 'organize_only')
                """);
        jdbcTemplate.update("""
                INSERT INTO source_files (
                    id, file_uid, source_id, import_job_id, file_name, file_ext,
                    original_path, file_size, parse_status, organize_status
                ) VALUES
                    (1, 'file_missing', 1, 1, 'missing.md', 'md', 'E:/seed/missing.md', 1, 'parsed', 'copied'),
                    (2, 'file_dup_a', 2, 1, 'dup-a.md', 'md', 'E:/seed/dup-a.md', 128, 'parsed', 'copied'),
                    (3, 'file_dup_b', 3, 1, 'dup-b.md', 'md', 'E:/seed/dup-b.md', 128, 'parsed', 'copied')
                """);
        jdbcTemplate.update("""
                INSERT INTO source_contents (
                    id, content_uid, source_id, source_file_id, parser_name, raw_text,
                    text_hash, char_count, raw_text_saved, parse_status
                ) VALUES
                    (1, 'content_missing', 1, 1, 'markdown', '   ', 'blank-hash', 0, TRUE, 'parsed'),
                    (2, 'content_dup_a', 2, 2, 'markdown', '相同知识片段', 'duplicate-hash', 6, TRUE, 'parsed'),
                    (3, 'content_dup_b', 3, 3, 'markdown', '相同知识片段', 'duplicate-hash', 6, TRUE, 'parsed')
                """);
        jdbcTemplate.update("""
                INSERT INTO personal_records (
                    id, record_uid, record_type, title, occurred_at, source_channel,
                    source_ref, raw_content, structured_json, status, sensitivity_level,
                    created_by, archived_at, created_at
                ) VALUES
                    (1, 'record_old_open', 'expense', '未归档账单', CURRENT_TIMESTAMP,
                        'web-ui', 'manual', '午餐 28 元', '{"amount":28}', 'pending',
                        'medium', 'web-ui', NULL, TIMESTAMP '2026-01-01 00:00:00'),
                    (2, 'record_archived', 'note', '已归档记录', CURRENT_TIMESTAMP,
                        'web-ui', 'manual', '已经归档', NULL, 'archived',
                        'medium', 'web-ui', CURRENT_TIMESTAMP, TIMESTAMP '2026-01-01 00:00:00')
                """);
    }

    private String firstMaintenanceItemUid(String runUid) {
        return jdbcTemplate.queryForObject(
                "SELECT item_uid FROM knowledge_maintenance_items WHERE run_uid = ? ORDER BY id ASC LIMIT 1",
                String.class,
                runUid
        );
    }
}
