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
        jdbcTemplate.execute("DROP TABLE IF EXISTS obsidian_notes");
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
                CREATE TABLE obsidian_notes (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    note_uid VARCHAR(64) NOT NULL,
                    source_id BIGINT NOT NULL,
                    source_file_id BIGINT NULL,
                    note_type VARCHAR(64) NOT NULL DEFAULT 'source_note',
                    vault_name VARCHAR(128) NOT NULL,
                    vault_path VARCHAR(1024) NOT NULL,
                    absolute_path CLOB NOT NULL,
                    obsidian_uri CLOB NOT NULL,
                    title VARCHAR(512) NOT NULL,
                    frontmatter_json CLOB NULL,
                    content_hash VARCHAR(128) NULL,
                    status VARCHAR(64) NOT NULL DEFAULT 'written',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_obsidian_notes_note_uid (note_uid)
                )
                """);
        seedSourceFile();
    }

    @Test
    void initializeVaultCreatesExpectedDirectories() throws Exception {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/obsidian/init",
                null,
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("vaultName").asText()).isEqualTo("WikiForgeVault");
        assertThat(Path.of(data.path("vaultPath").asText()).toRealPath()).isEqualTo(OBSIDIAN_VAULT.toRealPath());
        assertThat(Files.isDirectory(OBSIDIAN_VAULT.resolve("00_Inbox_收集箱"))).isTrue();
        assertThat(Files.isDirectory(OBSIDIAN_VAULT.resolve("00_Inbox_收集箱/Sources_来源"))).isTrue();
        assertThat(Files.isDirectory(OBSIDIAN_VAULT.resolve("10_Wiki_主题库"))).isTrue();
    }

    @Test
    void generateDraftUsesSourceFileMetadata() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/obsidian-note/draft",
                null,
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("fileUid").asText()).isEqualTo("file_test");
        assertThat(data.path("sourceUid").asText()).isEqualTo("src_test");
        assertThat(data.path("vaultPath").asText())
                .isEqualTo("00_Inbox_收集箱/Sources_来源/example.pdf-src_test.md");
        assertThat(data.path("markdown").asText()).contains("# example.pdf");
        assertThat(data.path("markdown").asText()).contains("Source UID: `src_test`");
        assertThat(data.path("markdown").asText()).contains("原始路径");
    }

    @Test
    void statusShowsVaultAndLatestNote() {
        ResponseEntity<JsonNode> initialResponse = restTemplate.getForEntity(
                "/api/v1/obsidian/status",
                JsonNode.class
        );

        assertThat(initialResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode initialData = initialResponse.getBody().path("data");
        assertThat(initialData.path("vaultName").asText()).isEqualTo("WikiForgeVault");
        assertThat(initialData.path("exists").asBoolean()).isTrue();
        assertThat(initialData.path("writable").asBoolean()).isTrue();
        assertThat(initialData.path("sourceNoteDirectoryExists").asBoolean()).isFalse();
        assertThat(initialData.path("lastNoteUid").isMissingNode()).isTrue();

        ResponseEntity<JsonNode> writeResponse = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/obsidian-note/write",
                Map.of("markdown", "# status note"),
                JsonNode.class
        );
        String noteUid = writeResponse.getBody().path("data").path("noteUid").asText();

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/obsidian/status",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("sourceNoteDirectoryExists").asBoolean()).isTrue();
        assertThat(data.path("lastNoteUid").asText()).isEqualTo(noteUid);
        assertThat(data.path("lastWrittenAt").asText()).isNotBlank();
    }

    @Test
    void sourceFileNoteReturnsLatestWrittenNoteAndListShowsStatus() {
        ResponseEntity<JsonNode> emptyResponse = restTemplate.getForEntity(
                "/api/v1/source-files/file_test/obsidian-note",
                JsonNode.class
        );
        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emptyResponse.getBody().path("data").isNull()).isTrue();

        ResponseEntity<JsonNode> writeResponse = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/obsidian-note/write",
                Map.of("markdown", "# first note"),
                JsonNode.class
        );
        String firstNoteUid = writeResponse.getBody().path("data").path("noteUid").asText();

        restTemplate.postForEntity(
                "/api/v1/source-files/file_test/obsidian-note/write",
                Map.of("markdown", "# latest note"),
                JsonNode.class
        );

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/source-files/file_test/obsidian-note",
                JsonNode.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("noteUid").asText()).isNotEqualTo(firstNoteUid);
        assertThat(data.path("fileUid").asText()).isEqualTo("file_test");
        assertThat(data.path("status").asText()).isEqualTo("written");

        ResponseEntity<JsonNode> listResponse = restTemplate.getForEntity(
                "/api/v1/source-files?jobUid=job_test&page=1&pageSize=10",
                JsonNode.class
        );
        JsonNode file = listResponse.getBody().path("data").path("items").path(0);
        assertThat(file.path("obsidianNoteUid").asText()).isEqualTo(data.path("noteUid").asText());
        assertThat(file.path("obsidianNoteStatus").asText()).isEqualTo("written");
        assertThat(file.path("obsidianVaultPath").asText()).contains("Sources_来源");
    }

    @Test
    void writeNotePersistsFileAndPreviewReadsFromVault() {
        String markdown = "# example.pdf\n\nMVP2 source note.";

        ResponseEntity<JsonNode> writeResponse = restTemplate.postForEntity(
                "/api/v1/source-files/file_test/obsidian-note/write",
                Map.of("markdown", markdown),
                JsonNode.class
        );

        assertThat(writeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode note = writeResponse.getBody().path("data");
        assertThat(note.path("noteUid").asText()).matches("note_\\d{8}_[0-9a-f]{12}");
        assertThat(note.path("obsidianUri").asText()).startsWith("obsidian://open?vault=WikiForgeVault&file=");
        Path notePath = OBSIDIAN_VAULT.resolve(note.path("vaultPath").asText()).normalize();
        assertThat(Files.isRegularFile(notePath)).isTrue();
        assertThat(readString(notePath)).isEqualTo(markdown);

        Integer rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM obsidian_notes", Integer.class);
        assertThat(rowCount).isEqualTo(1);

        ResponseEntity<JsonNode> previewResponse = restTemplate.getForEntity(
                "/api/v1/obsidian/notes/{noteUid}/preview",
                JsonNode.class,
                note.path("noteUid").asText()
        );
        assertThat(previewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewResponse.getBody().path("data").path("markdown").asText()).isEqualTo(markdown);
    }

    @Test
    void previewRejectsVaultTraversal() {
        jdbcTemplate.update("""
                INSERT INTO obsidian_notes (
                    id, note_uid, source_id, source_file_id, note_type, vault_name, vault_path,
                    absolute_path, obsidian_uri, title, status, created_at, updated_at
                ) VALUES (
                    300, 'note_bad', 100, 200, 'source_note', 'WikiForgeVault', '../outside.md',
                    'outside', 'obsidian://open?vault=WikiForgeVault&file=outside.md',
                    'bad', 'written', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/obsidian/notes/note_bad/preview",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("code").asText()).isEqualTo("OBSIDIAN_002");
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
                INSERT INTO sources (
                    id, source_uid, title, source_type, source_platform, raw_original_path,
                    raw_managed_path, raw_organize_status, processing_intent, content_hash,
                    status, collected_at, created_at, updated_at
                ) VALUES (
                    100, 'src_test', 'example.pdf', 'pdf', 'local', ?,
                    ?, 'copied', 'organize_only', 'hash-test',
                    'organized', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """, TEST_ROOT.resolve("input/example.pdf").toString(), RAW_SOURCES_ROOT.resolve("example.pdf").toString());
        jdbcTemplate.update("""
                INSERT INTO source_files (
                    id, file_uid, source_id, import_job_id, file_name, file_ext, original_path,
                    managed_path, file_size, mime_type, content_hash, parse_status,
                    organize_status, created_at
                ) VALUES (
                    200, 'file_test', 100, 10, 'example.pdf', 'pdf', ?,
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
