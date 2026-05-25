package com.wikiforge.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.wikiforge.core.application.dto.RunLocalImportJobRequest;
import com.wikiforge.core.application.port.WorkerImportJobClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@SpringBootTest(classes = WikiForgeCoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImportJobApiIntegrationTests {

    private static final String INTERNAL_TOKEN = "test-internal-token";
    private static final Path TEST_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "wikiforge-core-api-test-" + UUID.randomUUID()
    ).toAbsolutePath().normalize();
    private static final Path ALLOWED_ROOT = TEST_ROOT.resolve("allowed").normalize();
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
        registry.add("wikiforge.security.internal-api-token", () -> INTERNAL_TOKEN);
        registry.add("wikiforge.security.allowed-scan-roots", ALLOWED_ROOT::toString);
    }

    @BeforeEach
    void prepareSchema() throws Exception {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        Files.createDirectories(ALLOWED_ROOT);
        Files.createDirectories(RAW_SOURCES_ROOT);
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
                    input_url CLOB NULL,
                    raw_sources_root CLOB NULL,
                    recursive_scan BOOLEAN NOT NULL DEFAULT TRUE,
                    organize_mode VARCHAR(64) NOT NULL DEFAULT 'copy',
                    max_copy_file_size_mb INT NOT NULL DEFAULT 100,
                    source_platform VARCHAR(128) NULL,
                    connector_name VARCHAR(128) NULL,
                    connector_status VARCHAR(64) NULL,
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
    }

    @Test
    void createLocalImportJobPersistsPendingJobWithConfiguredRawRootAndDefaults() throws Exception {
        Path inputPath = createInputDirectory("messy-sources");

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/import-jobs/local",
                Map.of("inputPath", inputPath.toString()),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("message").asText()).isEqualTo("ok");
        JsonNode data = body.path("data");
        assertThat(data.path("jobUid").asText()).startsWith("job_");
        assertThat(data.path("jobUid").asText()).matches("job_\\d{8}_[0-9a-f]{12}");
        assertThat(data.path("importType").asText()).isEqualTo("path_scan");
        assertThat(data.path("inputPath").asText()).isEqualTo(inputPath.toString());
        assertThat(data.path("rawSourcesRoot").asText()).isEqualTo(RAW_SOURCES_ROOT.toString());
        assertThat(data.path("recursive").asBoolean()).isTrue();
        assertThat(data.path("organizeMode").asText()).isEqualTo("copy");
        assertThat(data.path("maxCopyFileSizeMb").asInt()).isEqualTo(100);
        assertThat(data.path("status").asText()).isEqualTo("pending");
        assertThat(data.path("totalCount").asInt()).isZero();
        assertThat(data.path("createdAt").asText()).isNotBlank();

        verify(workerImportJobClient).startLocalImportJob(new RunLocalImportJobRequest(
                data.path("jobUid").asText(),
                inputPath.toString(),
                RAW_SOURCES_ROOT.toString(),
                true,
                "copy",
                100,
                true,
                true,
                false
        ));

        Integer persisted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_jobs WHERE job_uid = ? AND max_copy_file_size_mb = 100",
                Integer.class,
                data.path("jobUid").asText()
        );
        assertThat(persisted).isEqualTo(1);
    }

    @Test
    void uploadSourcesCopiesFilesToRawSourcesAndRegistersSourceFile() throws Exception {
        byte[] content = "浏览器上传进入 Raw Sources".getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "upload-note.md";
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.TEXT_PLAIN);
        fileHeaders.setContentDispositionFormData("files", resource.getFilename());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", new HttpEntity<>(resource, fileHeaders));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/upload-sources",
                new HttpEntity<>(body, headers),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("jobUid").asText()).startsWith("job_");
        assertThat(data.path("importType").asText()).isEqualTo("浏览器上传");
        assertThat(data.path("statusCode").asText()).isEqualTo("已完成");
        assertThat(data.path("uploadedCount").asInt()).isEqualTo(1);

        Map<String, Object> sourceFile = jdbcTemplate.queryForMap(
                "SELECT sf.file_name, sf.original_path, sf.managed_path, sf.content_hash, "
                        + "sf.organize_status, sf.parse_status, ij.import_type, ij.status "
                        + "FROM source_files sf JOIN import_jobs ij ON sf.import_job_id = ij.id "
                        + "WHERE ij.job_uid = ?",
                data.path("jobUid").asText()
        );
        assertThat(sourceFile.get("file_name")).isEqualTo("upload-note.md");
        assertThat(sourceFile.get("original_path").toString()).startsWith("browser-upload://");
        assertThat(sourceFile.get("content_hash").toString()).hasSize(64);
        assertThat(sourceFile.get("organize_status")).isEqualTo("copied");
        assertThat(sourceFile.get("parse_status")).isEqualTo("success");
        assertThat(sourceFile.get("import_type")).isEqualTo("upload");
        assertThat(sourceFile.get("status")).isEqualTo("completed");

        Map<String, Object> sourceContent = jdbcTemplate.queryForMap(
                "SELECT parser_name, raw_text, parse_status FROM source_contents"
        );
        assertThat(sourceContent.get("parser_name")).isEqualTo("markdown-text");
        assertThat(sourceContent.get("raw_text")).isEqualTo("浏览器上传进入 Raw Sources");
        assertThat(sourceContent.get("parse_status")).isEqualTo("success");

        Path managedPath = Path.of(sourceFile.get("managed_path").toString());
        assertThat(managedPath).startsWith(RAW_SOURCES_ROOT);
        assertThat(Files.exists(managedPath)).isTrue();
        assertThat(Files.readString(managedPath, StandardCharsets.UTF_8)).isEqualTo("浏览器上传进入 Raw Sources");
    }

    @Test
    void createLocalImportJobRejectsRawRootMismatchAndDisallowedInputRoot() throws Exception {
        Path inputPath = createInputDirectory("accepted");
        Path wrongRawRoot = TEST_ROOT.resolve("wrong-raw-root").normalize();
        Files.createDirectories(wrongRawRoot);

        ResponseEntity<JsonNode> rawRootResponse = restTemplate.postForEntity(
                "/api/v1/import-jobs/local",
                Map.of("inputPath", inputPath.toString(), "rawSourcesRoot", wrongRawRoot.toString()),
                JsonNode.class
        );

        assertThat(rawRootResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rawRootResponse.getBody().path("code").asText()).isEqualTo("SOURCE_001");

        Path disallowedInput = TEST_ROOT.resolve("outside/input").normalize();
        Files.createDirectories(disallowedInput);
        ResponseEntity<JsonNode> allowedRootResponse = restTemplate.postForEntity(
                "/api/v1/import-jobs/local",
                Map.of("inputPath", disallowedInput.toString(), "rawSourcesRoot", RAW_SOURCES_ROOT.toString()),
                JsonNode.class
        );

        assertThat(allowedRootResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(allowedRootResponse.getBody().path("code").asText()).isEqualTo("SOURCE_001");
    }

    @Test
    void createLocalImportJobRejectsMissingInputPath() {
        Path inputPath = ALLOWED_ROOT.resolve("missing").normalize();

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/import-jobs/local",
                Map.of("inputPath", inputPath.toString()),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = response.getBody();
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo("SOURCE_002");
    }

    @Test
    void updateAndListImportJobReflectsRunningStatusWhenInternalTokenIsValid() throws Exception {
        String jobUid = createJob();

        ResponseEntity<JsonNode> patchResponse = restTemplate.exchange(
                "/api/v1/internal/import-jobs/{jobUid}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of(
                        "status", "running",
                        "totalCount", 10,
                        "successCount", 3,
                        "skippedCount", 1,
                        "failedCount", 0
                ), internalHeaders()),
                JsonNode.class,
                jobUid
        );

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody().path("success").asBoolean()).isTrue();

        ResponseEntity<JsonNode> detailResponse = restTemplate.getForEntity(
                "/api/v1/import-jobs/{jobUid}",
                JsonNode.class,
                jobUid
        );
        JsonNode detail = detailResponse.getBody().path("data");
        assertThat(detail.path("status").asText()).isEqualTo("running");
        assertThat(detail.path("totalCount").asInt()).isEqualTo(10);
        assertThat(detail.path("successCount").asInt()).isEqualTo(3);
        assertThat(detail.path("skippedCount").asInt()).isEqualTo(1);
        assertThat(detail.path("startedAt").asText()).isNotBlank();

        ResponseEntity<JsonNode> listResponse = restTemplate.getForEntity(
                "/api/v1/import-jobs?status=running&page=1&pageSize=20",
                JsonNode.class
        );
        JsonNode page = listResponse.getBody().path("data");
        assertThat(page.path("total").asInt()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("jobUid").asText()).isEqualTo(jobUid);
    }

    @Test
    void internalImportJobStatusApiRejectsMissingToken() throws Exception {
        String jobUid = createJob();

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/internal/import-jobs/{jobUid}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "running")),
                JsonNode.class,
                jobUid
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void updateImportJobStatusRejectsInvalidTransition() throws Exception {
        String jobUid = createJob();

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/internal/import-jobs/{jobUid}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "completed"), internalHeaders()),
                JsonNode.class,
                jobUid
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = response.getBody();
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo("IMPORT_002");
    }

    @Test
    void submitSourceFilesBatchComputesDuplicatesAndIsIdempotent() throws Exception {
        String jobUid = createJob();
        Map<String, Object> batch = Map.of("files", List.of(
                sourceFile("first.pdf", "a/first.pdf", "hash-1"),
                sourceFile("second.pdf", "b/second.pdf", "hash-1")
        ));

        ResponseEntity<JsonNode> firstSubmit = postInternalBatch(jobUid, batch);
        ResponseEntity<JsonNode> secondSubmit = postInternalBatch(jobUid, batch);

        assertThat(firstSubmit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondSubmit.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM source_files", Integer.class);
        assertThat(rowCount).isEqualTo(2);

        ResponseEntity<JsonNode> listResponse = restTemplate.getForEntity(
                "/api/v1/source-files?jobUid={jobUid}&page=1&pageSize=50",
                JsonNode.class,
                jobUid
        );

        JsonNode page = listResponse.getBody().path("data");
        assertThat(page.path("total").asInt()).isEqualTo(2);
        JsonNode first = page.path("items").get(0);
        JsonNode duplicate = page.path("items").get(1);
        assertThat(first.path("fileUid").asText()).startsWith("file_");
        assertThat(first.path("duplicateOfFileUid").isMissingNode() || first.path("duplicateOfFileUid").isNull()).isTrue();
        assertThat(duplicate.path("duplicateOfFileUid").asText()).isEqualTo(first.path("fileUid").asText());
        assertThat(duplicate.path("organizeStatus").asText()).isEqualTo("duplicate");
    }

    @Test
    void submitSourceFilesBatchPersistsParsedMarkdownText() throws Exception {
        String jobUid = createJob();
        Map<String, Object> parsedFile = sourceFile("note.md", "notes/note.md", "hash-md");
        parsedFile = new java.util.HashMap<>(parsedFile);
        parsedFile.put("fileExt", "md");
        parsedFile.put("mimeType", "text/markdown");
        parsedFile.put("parseStatus", "success");
        parsedFile.put("parserName", "markdown-text");
        parsedFile.put("contentType", "plain_text");
        parsedFile.put("parsedText", "第一段正文\n第二段正文");
        parsedFile.put("textHash", "text-hash-md");
        parsedFile.put("charCount", 11);
        parsedFile.put("rawTextSaved", true);

        ResponseEntity<JsonNode> submit = postInternalBatch(jobUid, Map.of("files", List.of(parsedFile)));

        assertThat(submit.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer contentRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM source_contents", Integer.class);
        assertThat(contentRows).isEqualTo(1);
        String rawText = jdbcTemplate.queryForObject("SELECT raw_text FROM source_contents", String.class);
        assertThat(rawText).isEqualTo("第一段正文\n第二段正文");
        String parseStatus = jdbcTemplate.queryForObject("SELECT parse_status FROM source_contents", String.class);
        assertThat(parseStatus).isEqualTo("success");
    }

    private ResponseEntity<JsonNode> postInternalBatch(String jobUid, Map<String, Object> batch) {
        return restTemplate.exchange(
                "/api/v1/internal/import-jobs/{jobUid}/source-files/batch",
                HttpMethod.POST,
                new HttpEntity<>(batch, internalHeaders()),
                JsonNode.class,
                jobUid
        );
    }

    private Map<String, Object> sourceFile(String fileName, String relativeOriginalPath, String contentHash) {
        return Map.of(
                "fileName", fileName,
                "fileExt", "pdf",
                "originalPath", ALLOWED_ROOT.resolve(relativeOriginalPath).normalize().toString(),
                "managedPath", RAW_SOURCES_ROOT.resolve(fileName).normalize().toString(),
                "fileSize", 1024,
                "mimeType", "application/pdf",
                "contentHash", contentHash,
                "parseStatus", "pending",
                "organizeStatus", "copied"
        );
    }

    private String createJob() throws Exception {
        Path inputPath = createInputDirectory("messy-sources");

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/import-jobs/local",
                Map.of("inputPath", inputPath.toString()),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("data").path("jobUid").asText();
    }

    private Path createInputDirectory(String name) throws Exception {
        Path inputPath = ALLOWED_ROOT.resolve(name).normalize();
        Files.createDirectories(inputPath);
        return inputPath.toRealPath();
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-WikiForge-Internal-Token", INTERNAL_TOKEN);
        return headers;
    }
}
