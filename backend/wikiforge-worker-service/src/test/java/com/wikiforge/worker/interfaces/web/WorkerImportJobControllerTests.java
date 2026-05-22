package com.wikiforge.worker.interfaces.web;

import com.wikiforge.worker.WikiForgeWorkerApplication;
import com.wikiforge.worker.application.port.CoreImportJobClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WikiForgeWorkerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkerImportJobControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private CoreImportJobClient coreImportJobClient;

    @TempDir
    Path tempDir;

    @Test
    void runLocalImportJobReturnsAcceptedResponse() throws Exception {
        Path inputPath = tempDir.resolve("input");
        Path rawSourcesRoot = tempDir.resolve("raw-sources");
        Files.createDirectories(inputPath);
        Files.writeString(inputPath.resolve("note.md"), "note", StandardCharsets.UTF_8);

        Map<String, Object> request = Map.of(
                "jobUid", "job_20260523_000001",
                "inputPath", inputPath.toString(),
                "rawSourcesRoot", rawSourcesRoot.toString(),
                "recursive", true,
                "organizeMode", "copy",
                "maxCopyFileSizeMb", 100,
                "skipHidden", true,
                "skipTemporary", true,
                "followSymlinks", false
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/worker/import-jobs/local/run",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"success\":true");
        assertThat(response.getBody()).contains("\"jobUid\":\"job_20260523_000001\"");
        assertThat(response.getBody()).contains("\"accepted\":true");
        assertThat(response.getBody()).contains("\"workerStatus\":\"accepted\"");
    }
}
