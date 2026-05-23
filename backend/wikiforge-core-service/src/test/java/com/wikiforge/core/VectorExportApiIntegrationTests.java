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
class VectorExportApiIntegrationTests {

    private static final Path TEST_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "wikiforge-vector-export-test-" + UUID.randomUUID()
    ).toAbsolutePath().normalize();
    private static final Path VECTOR_EXPORT_ROOT = TEST_ROOT.resolve("vector-exports").normalize();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private WorkerImportJobClient workerImportJobClient;

    @DynamicPropertySource
    static void coreProperties(DynamicPropertyRegistry registry) {
        registry.add("wikiforge.vector-export-root", VECTOR_EXPORT_ROOT::toString);
    }

    @BeforeEach
    void prepareSchema() throws Exception {
        deleteDirectory(TEST_ROOT);
        Files.createDirectories(VECTOR_EXPORT_ROOT);
        jdbcTemplate.execute("DROP TABLE IF EXISTS content_chunks");
        jdbcTemplate.execute("DROP TABLE IF EXISTS vector_export_jobs");
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
        jdbcTemplate.execute("""
                CREATE TABLE vector_export_jobs (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    export_uid VARCHAR(64) NOT NULL,
                    scope VARCHAR(64) NOT NULL,
                    target_collection VARCHAR(128) NOT NULL,
                    export_format VARCHAR(32) NOT NULL DEFAULT 'jsonl',
                    status VARCHAR(64) NOT NULL DEFAULT 'completed',
                    total_count INT NOT NULL DEFAULT 0,
                    export_file_name VARCHAR(255) NULL,
                    export_relative_path VARCHAR(1024) NULL,
                    error_message CLOB NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    finished_at TIMESTAMP NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_vector_export_jobs_export_uid (export_uid)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE content_chunks (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    chunk_uid VARCHAR(64) NOT NULL,
                    export_uid VARCHAR(64) NOT NULL,
                    content_type VARCHAR(64) NOT NULL,
                    source_uid VARCHAR(64) NULL,
                    file_uid VARCHAR(64) NULL,
                    record_uid VARCHAR(64) NULL,
                    title VARCHAR(512) NULL,
                    chunk_index INT NOT NULL DEFAULT 0,
                    chunk_text CLOB NOT NULL,
                    text_hash VARCHAR(128) NOT NULL,
                    char_count INT NOT NULL DEFAULT 0,
                    token_estimate INT NOT NULL DEFAULT 0,
                    metadata_json CLOB NULL,
                    embedding_status VARCHAR(64) NOT NULL DEFAULT 'pending',
                    target_collection VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_content_chunks_chunk_uid (chunk_uid)
                )
                """);
        seedKnowledgeRows();
    }

    @Test
    void createVectorExportWritesJsonlAndChunkLedger() throws Exception {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/vector-exports",
                Map.of(
                        "scope", "all",
                        "targetCollection", "wikiforge_test",
                        "maxChunkChars", 200,
                        "limit", 20
                ),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("exportUid").asText()).startsWith("vexp_");
        assertThat(data.path("scope").asText()).isEqualTo("all");
        assertThat(data.path("targetCollection").asText()).isEqualTo("wikiforge_test");
        assertThat(data.path("exportFormat").asText()).isEqualTo("jsonl");
        assertThat(data.path("status").asText()).isEqualTo("completed");
        assertThat(data.path("totalCount").asInt()).isGreaterThanOrEqualTo(4);

        String exportRelativePath = data.path("exportRelativePath").asText();
        assertThat(exportRelativePath).doesNotContain(":").endsWith(".jsonl");
        Path exportFile = VECTOR_EXPORT_ROOT.resolve(exportRelativePath).normalize();
        assertThat(exportFile).startsWith(VECTOR_EXPORT_ROOT);
        assertThat(Files.exists(exportFile)).isTrue();

        String jsonl = Files.readString(exportFile);
        assertThat(jsonl)
                .contains("\"contentType\":\"source_content\"")
                .contains("\"contentType\":\"personal_record\"")
                .contains("\"targetCollection\":\"wikiforge_test\"")
                .contains("\"metadata\":{");

        Integer chunkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM content_chunks WHERE export_uid = ? AND embedding_status = 'pending'",
                Integer.class,
                data.path("exportUid").asText()
        );
        assertThat(chunkCount).isEqualTo(data.path("totalCount").asInt());

        String metadataJson = jdbcTemplate.queryForObject(
                "SELECT metadata_json FROM content_chunks WHERE content_type = 'personal_record' LIMIT 1",
                String.class
        );
        assertThat(metadataJson).contains("recordUid", "recordType");
    }

    @Test
    void listVectorExportsReturnsCreatedJobWithoutAbsolutePath() {
        restTemplate.postForEntity(
                "/api/v1/vector-exports",
                Map.of("scope", "sources", "targetCollection", "wikiforge_sources", "maxChunkChars", 200),
                JsonNode.class
        );

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/vector-exports?status=completed&page=1&pageSize=10",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("total").asLong()).isEqualTo(1);
        JsonNode item = data.path("items").get(0);
        assertThat(item.path("targetCollection").asText()).isEqualTo("wikiforge_sources");
        assertThat(item.path("exportRelativePath").asText()).doesNotContain(TEST_ROOT.toString());
    }

    private void seedKnowledgeRows() {
        jdbcTemplate.update("""
                INSERT INTO import_jobs (id, job_uid, import_type, input_path, raw_sources_root)
                VALUES (1, 'job_vector_seed', 'local_path', 'E:/seed', 'E:/raw')
                """);
        jdbcTemplate.update("""
                INSERT INTO sources (
                    id, source_uid, title, source_type, source_platform, source_url, processing_intent
                ) VALUES (1, 'src_vector_seed', 'LLM Wiki 笔记', 'link', 'feishu',
                    'https://example.feishu.cn/docx/wiki', 'organize_only')
                """);
        jdbcTemplate.update("""
                INSERT INTO source_files (
                    id, file_uid, source_id, import_job_id, file_name, file_ext,
                    original_path, file_size, parse_status, organize_status
                ) VALUES (1, 'file_vector_seed', 1, 1, 'llm-wiki.md', 'md',
                    'E:/seed/llm-wiki.md', 512, 'parsed', 'copied')
                """);
        jdbcTemplate.update("""
                INSERT INTO source_contents (
                    id, content_uid, source_id, source_file_id, parser_name, raw_text,
                    text_hash, char_count, raw_text_saved, parse_status
                ) VALUES (1, 'content_vector_seed', 1, 1, 'markdown', ?, 'seed-text-hash', ?, TRUE, 'parsed')
                """, repeatedText("LLM Wiki 把原始资料编译成稳定、可维护的知识页面。", 12), 360);
        jdbcTemplate.update("""
                INSERT INTO personal_records (
                    id, record_uid, record_type, title, occurred_at, source_channel,
                    source_ref, raw_content, structured_json, status, sensitivity_level, created_by
                ) VALUES (1, 'record_vector_seed', 'expense', '项目咖啡消费',
                    CURRENT_TIMESTAMP, 'web-ui', 'manual', ?, '{"amount":18}', 'pending', 'medium', 'web-ui')
                """, repeatedText("拿铁 18 元，与 WikiForge 架构评审和知识复盘有关。", 9));
    }

    private String repeatedText(String unit, int times) {
        return unit.repeat(times);
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
