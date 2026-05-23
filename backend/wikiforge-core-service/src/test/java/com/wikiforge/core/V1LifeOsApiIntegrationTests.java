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
class V1LifeOsApiIntegrationTests {

    private static final Path TEST_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "wikiforge-v1-lifeos-test-" + UUID.randomUUID()
    ).toAbsolutePath().normalize();
    private static final Path OBSIDIAN_VAULT = TEST_ROOT.resolve("WikiForgeVault").normalize();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private WorkerImportJobClient workerImportJobClient;

    @DynamicPropertySource
    static void coreProperties(DynamicPropertyRegistry registry) {
        registry.add("wikiforge.obsidian-vault-path", OBSIDIAN_VAULT::toString);
        registry.add("wikiforge.obsidian-vault-name", () -> "WikiForgeVault");
    }

    @BeforeEach
    void prepareSchema() throws Exception {
        deleteDirectory(TEST_ROOT);
        Files.createDirectories(OBSIDIAN_VAULT);
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
                    input_url CLOB NULL,
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
                    source_url CLOB NULL,
                    connector_name VARCHAR(128) NULL,
                    connector_status VARCHAR(64) NULL,
                    connector_trace_id VARCHAR(128) NULL,
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
    }

    @Test
    void createLinkSourceStoresUnifiedSourceDraft() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/link-sources",
                Map.of(
                        "title", "Feishu LLM Wiki",
                        "sourceUrl", "https://example.feishu.cn/docx/wiki",
                        "sourcePlatform", "feishu",
                        "rawContent", "飞书文档正文占位，后续由连接器读取。",
                        "processingIntent", "organize_only"
                ),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("sourceUid").asText()).startsWith("src_");
        assertThat(data.path("fileUid").asText()).startsWith("file_");
        assertThat(data.path("jobUid").asText()).startsWith("job_");
        assertThat(data.path("sourcePlatform").asText()).isEqualTo("feishu");

        Integer sourceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sources WHERE source_uid = ? AND source_type = 'link' AND source_platform = 'feishu'",
                Integer.class,
                data.path("sourceUid").asText()
        );
        assertThat(sourceCount).isEqualTo(1);
        String rawText = jdbcTemplate.queryForObject(
                "SELECT raw_text FROM source_contents sc JOIN source_files sf ON sf.id = sc.source_file_id WHERE sf.file_uid = ?",
                String.class,
                data.path("fileUid").asText()
        );
        assertThat(rawText).contains("飞书文档正文占位");
    }

    @Test
    void personalRecordCanBeListedSummarizedAndArchivedToObsidian() throws Exception {
        ResponseEntity<JsonNode> createResponse = restTemplate.postForEntity(
                "/api/v1/personal-records",
                Map.of(
                        "recordType", "expense",
                        "title", "咖啡消费",
                        "occurredAt", "2026-05-24T09:30:00+08:00",
                        "rawContent", "拿铁 18 元，和项目评审有关。",
                        "sourceChannel", "web-ui",
                        "sourceRef", "manual-form",
                        "structured", Map.of("amount", 18, "currency", "CNY"),
                        "sensitivityLevel", "medium"
                ),
                JsonNode.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode created = createResponse.getBody().path("data");
        String recordUid = created.path("recordUid").asText();
        assertThat(recordUid).startsWith("record_");
        assertThat(created.path("status").asText()).isEqualTo("pending");
        assertThat(created.path("structuredJson").asText()).contains("amount");

        ResponseEntity<JsonNode> listResponse = restTemplate.getForEntity(
                "/api/v1/personal-records?recordType=expense&status=pending&page=1&pageSize=10",
                JsonNode.class
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody().path("data").path("total").asLong()).isEqualTo(1);
        assertThat(listResponse.getBody().path("data").path("items").get(0).path("recordUid").asText())
                .isEqualTo(recordUid);

        ResponseEntity<JsonNode> summaryResponse = restTemplate.getForEntity(
                "/api/v1/personal-records/summary?period=all",
                JsonNode.class
        );
        assertThat(summaryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode summary = summaryResponse.getBody().path("data");
        assertThat(summary.path("total").asLong()).isEqualTo(1);
        assertThat(summary.path("byType").path("expense").asLong()).isEqualTo(1);
        assertThat(summary.path("byStatus").path("pending").asLong()).isEqualTo(1);

        ResponseEntity<JsonNode> archiveResponse = restTemplate.postForEntity(
                "/api/v1/personal-records/" + recordUid + "/obsidian-note",
                null,
                JsonNode.class
        );
        assertThat(archiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode note = archiveResponse.getBody().path("data");
        assertThat(note.path("status").asText()).isEqualTo("archived");
        String vaultPath = note.path("vaultPath").asText();
        assertThat(vaultPath).startsWith("00_Inbox_收集箱/Personal_个人记录/expense/");
        assertThat(note.path("obsidianUri").asText()).startsWith("obsidian://open");

        Path markdownPath = OBSIDIAN_VAULT.resolve(vaultPath).normalize();
        assertThat(markdownPath).startsWith(OBSIDIAN_VAULT);
        assertThat(Files.readString(markdownPath)).contains("咖啡消费", "拿铁 18 元", "\"amount\" : 18");

        ResponseEntity<JsonNode> detailResponse = restTemplate.getForEntity(
                "/api/v1/personal-records/" + recordUid,
                JsonNode.class
        );
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode detail = detailResponse.getBody().path("data");
        assertThat(detail.path("status").asText()).isEqualTo("archived");
        assertThat(detail.path("obsidianVaultPath").asText()).isEqualTo(vaultPath);
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
