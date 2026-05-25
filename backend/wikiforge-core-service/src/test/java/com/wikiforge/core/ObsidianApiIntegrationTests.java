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
class ObsidianApiIntegrationTests {

    private static final Path TEST_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "wikiforge-obsidian-api-test-" + UUID.randomUUID()
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
        jdbcTemplate.execute("DROP TABLE IF EXISTS wiki_ingest_runs");
        jdbcTemplate.execute("DROP TABLE IF EXISTS source_contents");
        jdbcTemplate.execute("DROP TABLE IF EXISTS source_files");
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
                CREATE TABLE source_files (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    file_uid VARCHAR(64) NOT NULL,
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
                CREATE TABLE wiki_ingest_runs (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    run_uid VARCHAR(64) NOT NULL,
                    source_file_id BIGINT NOT NULL,
                    file_uid VARCHAR(64) NOT NULL,
                    file_name VARCHAR(512) NULL,
                    status_code VARCHAR(128) NOT NULL DEFAULT '已创建',
                    status_label VARCHAR(128) NOT NULL DEFAULT '已创建',
                    source_page_path VARCHAR(1024) NULL,
                    wiki_page_paths CLOB NULL,
                    index_updated BOOLEAN NOT NULL DEFAULT FALSE,
                    log_entry_appended BOOLEAN NOT NULL DEFAULT FALSE,
                    write_status_code VARCHAR(128) NOT NULL DEFAULT '已创建',
                    write_status_label VARCHAR(128) NOT NULL DEFAULT '已创建',
                    fallback_reason CLOB NULL,
                    failure_reason CLOB NULL,
                    managed_block_preview CLOB NULL,
                    log_entry_preview CLOB NULL,
                    obsidian_uri VARCHAR(2048) NULL,
                    retryable BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    completed_at TIMESTAMP NULL,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_wiki_ingest_runs_run_uid (run_uid)
                )
                """);
        seedSourceFile();
    }

    @Test
    void initializeVaultCreatesMvp0ManagedRootOnly() throws Exception {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/obsidian/init",
                null,
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("vaultName").asText()).isEqualTo("WikiForgeVault");
        assertThat(data.path("managedRoot").asText()).isEqualTo("WikiForge/");
        assertThat(data.path("createdPaths").toString()).contains("WikiForge/index.md");
        assertThat(Files.isDirectory(OBSIDIAN_VAULT.resolve("WikiForge/00_规则"))).isTrue();
        assertThat(Files.isDirectory(OBSIDIAN_VAULT.resolve("WikiForge/10_来源"))).isTrue();
        assertThat(Files.isRegularFile(OBSIDIAN_VAULT.resolve("WikiForge/index.md"))).isTrue();
        assertThat(Files.isRegularFile(OBSIDIAN_VAULT.resolve("WikiForge/00_规则/LLM-Wiki写入规则.md"))).isTrue();
        assertThat(Files.exists(OBSIDIAN_VAULT.resolve("00_Inbox_收集箱"))).isFalse();
    }

    @Test
    void statusShowsManagedRootAndLatestWikiWrite() {
        ResponseEntity<JsonNode> initialResponse = restTemplate.getForEntity(
                "/api/v1/obsidian/status",
                JsonNode.class
        );

        assertThat(initialResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode initialData = initialResponse.getBody().path("data");
        assertThat(initialData.path("vaultName").asText()).isEqualTo("WikiForgeVault");
        assertThat(initialData.path("managedRoot").asText()).isEqualTo("WikiForge/");
        assertThat(initialData.path("exists").asBoolean()).isTrue();
        assertThat(initialData.path("writable").asBoolean()).isTrue();
        assertThat(initialData.path("managedRootExists").asBoolean()).isFalse();
        assertThat(initialData.path("lastWriteAt").isMissingNode()).isTrue();

        restTemplate.postForEntity("/api/v1/obsidian/init", null, JsonNode.class);
        restTemplate.postForEntity(
                "/api/v1/source-files/file_test/wiki-ingest-runs",
                Map.of("writeMode", "自动"),
                JsonNode.class
        );

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/obsidian/status",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("managedRootExists").asBoolean()).isTrue();
        assertThat(data.path("lastWriteAt").asText()).isNotBlank();
    }

    @Test
    void sourceNoteEndpointsAreNotExposed() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/source-files/file_test/obsidian-note",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void wikiIngestRunWritesSourcePageIndexAndLog() {
        jdbcTemplate.update("""
                INSERT INTO source_contents (
                    id, content_uid, source_file_id, parser_name, content_type,
                    raw_text, text_hash, char_count, raw_text_saved, parse_status,
                    created_at, updated_at
                ) VALUES (
                    401, 'content_wiki_ingest', 200, 'markdown-text', 'plain_text',
                    '这是用于 Wiki ingest 的正文。', 'text-hash-wiki', 18, TRUE, 'success',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/wiki-ingest-runs",
                Map.of("writeMode", "自动"),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("runUid").asText()).matches("wir_\\d{8}_[0-9a-f]{12}");
        assertThat(data.path("statusCode").asText()).isEqualTo("已写入");
        assertThat(data.path("sourcePagePath").asText()).startsWith("WikiForge/10_来源/");
        assertThat(data.path("indexUpdated").asBoolean()).isTrue();
        assertThat(data.path("logEntryAppended").asBoolean()).isTrue();

        Path sourcePage = OBSIDIAN_VAULT.resolve(data.path("sourcePagePath").asText()).normalize();
        assertThat(Files.isRegularFile(sourcePage)).isTrue();
        assertThat(readString(sourcePage)).contains("wikiforge:managed:start");
        assertThat(readString(sourcePage)).contains("这是用于 Wiki ingest 的正文。");
        assertThat(readString(OBSIDIAN_VAULT.resolve("WikiForge/index.md"))).contains("file_test");
        assertThat(readString(OBSIDIAN_VAULT.resolve("WikiForge/log.md"))).contains(data.path("runUid").asText());

        Integer runCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wiki_ingest_runs", Integer.class);
        assertThat(runCount).isEqualTo(1);

        ResponseEntity<JsonNode> fileResponse = restTemplate.getForEntity(
                "/api/v1/source-files/file_test",
                JsonNode.class
        );
        JsonNode file = fileResponse.getBody().path("data");
        assertThat(file.path("wikiStatusCode").asText()).isEqualTo("已写入 Wiki");
        assertThat(file.path("latestWikiIngestRun").path("runUid").asText()).isEqualTo(data.path("runUid").asText());
    }

    private void seedSourceFile() {
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
                INSERT INTO source_files (
                    id, file_uid, import_job_id, file_name, file_ext, original_path,
                    managed_path, file_size, mime_type, content_hash, parse_status,
                    organize_status, created_at
                ) VALUES (
                    200, 'file_test', 10, 'example.pdf', 'pdf', ?,
                    ?, 1024, 'application/pdf', 'hash-test', 'pending',
                    'copied', CURRENT_TIMESTAMP
                )
                """, TEST_ROOT.resolve("input/example.pdf").toString(), RAW_SOURCES_ROOT.resolve("example.pdf").toString());
    }

    private String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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
