package com.wikiforge.worker.application.service;

import com.wikiforge.worker.application.dto.RunLocalImportJobRequest;
import com.wikiforge.worker.application.dto.RunLocalImportJobResponse;
import com.wikiforge.worker.application.dto.SubmitSourceFilesBatchRequest;
import com.wikiforge.worker.application.dto.UpdateImportJobStatusRequest;
import com.wikiforge.worker.application.port.CoreImportJobClient;
import com.wikiforge.worker.infrastructure.filesystem.LocalFileScanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class LocalImportJobRunnerTests {

    @TempDir
    Path tempDir;

    @Test
    void runScansFilesAndSubmitsStatusAndSourceFileBatchToCore() throws Exception {
        Path inputPath = tempDir.resolve("input");
        Path rawSourcesRoot = tempDir.resolve("raw-sources");
        Files.createDirectories(inputPath);
        Files.writeString(inputPath.resolve("note.md"), "---\ntitle: Note\n---\n# Heading\n\n正文内容", StandardCharsets.UTF_8);
        Files.writeString(inputPath.resolve("note-copy.md"), "---\ntitle: Note\n---\n# Heading\n\n正文内容", StandardCharsets.UTF_8);

        RecordingCoreImportJobClient coreClient = new RecordingCoreImportJobClient();
        LocalImportJobRunner runner = new LocalImportJobRunner(new LocalFileScanner(), coreClient);

        RunLocalImportJobResponse response = runner.run(new RunLocalImportJobRequest(
                "job_20260523_000001",
                inputPath.toString(),
                rawSourcesRoot.toString(),
                true,
                "copy",
                100,
                true,
                true,
                false
        ));

        assertThat(response.jobUid()).isEqualTo("job_20260523_000001");
        assertThat(response.accepted()).isTrue();
        assertThat(response.workerStatus()).isEqualTo("accepted");

        assertThat(coreClient.statusUpdates).extracting(UpdateImportJobStatusRequest::status)
                .containsExactly("running", "completed");
        assertThat(coreClient.statusUpdates.get(1).totalCount()).isEqualTo(2);
        assertThat(coreClient.statusUpdates.get(1).successCount()).isEqualTo(1);
        assertThat(coreClient.statusUpdates.get(1).skippedCount()).isZero();
        assertThat(coreClient.statusUpdates.get(1).failedCount()).isZero();

        assertThat(coreClient.submittedBatch.files()).hasSize(2);
        assertThat(coreClient.submittedBatch.files())
                .extracting(file -> file.organizeStatus())
                .containsExactly("copied", "duplicate");
        assertThat(coreClient.submittedBatch.files())
                .extracting(file -> file.duplicateOfFileUid())
                .containsOnlyNulls();
        assertThat(coreClient.submittedBatch.files())
                .extracting(file -> file.parseStatus())
                .containsOnly("success");
        assertThat(coreClient.submittedBatch.files().get(0).parserName()).isEqualTo("markdown-text");
        assertThat(coreClient.submittedBatch.files().get(0).parsedText()).contains("# Heading");
        assertThat(coreClient.submittedBatch.files().get(0).parsedText()).doesNotContain("title: Note");
        assertThat(coreClient.submittedBatch.files().get(0).rawTextSaved()).isTrue();
        assertThat(Path.of(coreClient.submittedBatch.files().get(0).managedPath())).isRegularFile();
        assertThat(coreClient.submittedBatch.files().get(1).managedPath())
                .isEqualTo(coreClient.submittedBatch.files().get(0).managedPath());
    }

    @Test
    void runUsesConfiguredRawSourcesRootWhenRequestOmitsIt() throws Exception {
        Path inputPath = tempDir.resolve("input");
        Path configuredRawSourcesRoot = tempDir.resolve("configured-raw-sources");
        Files.createDirectories(inputPath);
        Files.writeString(inputPath.resolve("note.md"), "note", StandardCharsets.UTF_8);

        RecordingCoreImportJobClient coreClient = new RecordingCoreImportJobClient();
        LocalImportJobRunner runner = new LocalImportJobRunner(
                new LocalFileScanner(),
                coreClient,
                configuredRawSourcesRoot.toString()
        );

        RunLocalImportJobResponse response = runner.run(new RunLocalImportJobRequest(
                "job_20260523_000002",
                inputPath.toString(),
                null,
                true,
                "copy",
                100,
                true,
                true,
                false
        ));

        assertThat(response.accepted()).isTrue();
        assertThat(coreClient.submittedBatch.files()).hasSize(1);
        assertThat(coreClient.submittedBatch.files().getFirst().managedPath())
                .startsWith(configuredRawSourcesRoot.toAbsolutePath().normalize().toString());
    }

    private static final class RecordingCoreImportJobClient implements CoreImportJobClient {
        private final List<UpdateImportJobStatusRequest> statusUpdates = new ArrayList<>();
        private SubmitSourceFilesBatchRequest submittedBatch;

        @Override
        public void updateStatus(String jobUid, UpdateImportJobStatusRequest request) {
            statusUpdates.add(request);
        }

        @Override
        public void submitSourceFilesBatch(String jobUid, SubmitSourceFilesBatchRequest request) {
            submittedBatch = request;
        }
    }
}
