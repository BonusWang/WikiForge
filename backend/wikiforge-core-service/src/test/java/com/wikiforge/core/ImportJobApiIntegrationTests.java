package com.wikiforge.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.wikiforge.core.application.dto.RunLocalImportJobRequest;
import com.wikiforge.core.application.port.WorkerImportJobClient;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
                    recursive BOOLEAN NOT NULL DEFAULT TRUE,
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
    }

    @Test
    void createLocalImportJobPersistsPendingJobWithConfiguredRawRootAndDefaults() throws Exception {
        Path inputPath = createInputDirectory("messy-sources");

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/import-jobs/local",
                Map.of("inputPath", inputPath.toString(), "rawSourcesRoot", RAW_SOURCES_ROOT.toString()),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("message").asText()).isEqualTo("ok");
        JsonNode data = body.path("data");
        assertThat(data.path("jobUid").asText()).startsWith("job_");
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
                Map.of("inputPath", inputPath.toString(), "rawSourcesRoot", RAW_SOURCES_ROOT.toString()),
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
                Map.of("inputPath", inputPath.toString(), "rawSourcesRoot", RAW_SOURCES_ROOT.toString()),
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
