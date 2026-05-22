package com.wikiforge.worker.application.service;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.worker.application.dto.RunLocalImportJobRequest;
import com.wikiforge.worker.application.dto.RunLocalImportJobResponse;
import com.wikiforge.worker.application.dto.SubmitSourceFileItem;
import com.wikiforge.worker.application.dto.SubmitSourceFilesBatchRequest;
import com.wikiforge.worker.application.dto.UpdateImportJobStatusRequest;
import com.wikiforge.worker.application.port.CoreImportJobClient;
import com.wikiforge.worker.domain.model.LocalScanFile;
import com.wikiforge.worker.domain.model.LocalScanRequest;
import com.wikiforge.worker.domain.model.LocalScanResult;
import com.wikiforge.worker.infrastructure.filesystem.LocalFileScanner;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalImportJobRunner {

    private static final String ORGANIZE_MODE_COPY = "copy";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";

    private final LocalFileScanner localFileScanner;
    private final CoreImportJobClient coreImportJobClient;
    private final String configuredRawSourcesRoot;

    @Autowired
    public LocalImportJobRunner(
            LocalFileScanner localFileScanner,
            CoreImportJobClient coreImportJobClient,
            @Value("${WIKIFORGE_RAW_SOURCES_ROOT:${wikiforge.raw-sources-root:${wikiforge.raw-sources-path:}}}") String configuredRawSourcesRoot
    ) {
        this.localFileScanner = localFileScanner;
        this.coreImportJobClient = coreImportJobClient;
        this.configuredRawSourcesRoot = configuredRawSourcesRoot;
    }

    public LocalImportJobRunner(LocalFileScanner localFileScanner, CoreImportJobClient coreImportJobClient) {
        this(localFileScanner, coreImportJobClient, "");
    }

    public RunLocalImportJobResponse run(RunLocalImportJobRequest request) {
        requireCopyMode(request.organizeMode());

        coreImportJobClient.updateStatus(request.jobUid(), new UpdateImportJobStatusRequest(
                STATUS_RUNNING,
                0,
                0,
                0,
                0,
                null
        ));

        try {
            LocalScanResult scanResult = localFileScanner.scan(toLocalScanRequest(request));
            SubmitSourceFilesBatchRequest batchRequest = toBatchRequest(scanResult.files());
            if (!batchRequest.files().isEmpty()) {
                coreImportJobClient.submitSourceFilesBatch(request.jobUid(), batchRequest);
            }
            coreImportJobClient.updateStatus(request.jobUid(), new UpdateImportJobStatusRequest(
                    scanResult.failedCount() == 0 ? STATUS_COMPLETED : STATUS_FAILED,
                    scanResult.totalCount(),
                    scanResult.successCount(),
                    scanResult.skippedCount(),
                    scanResult.failedCount(),
                    scanResult.failedCount() == 0 ? null : "one or more files failed to copy"
            ));
            return new RunLocalImportJobResponse(request.jobUid(), true, "accepted");
        } catch (RuntimeException exception) {
            coreImportJobClient.updateStatus(request.jobUid(), new UpdateImportJobStatusRequest(
                    STATUS_FAILED,
                    null,
                    null,
                    null,
                    null,
                    exception.getMessage()
            ));
            throw exception;
        }
    }

    private void requireCopyMode(String organizeMode) {
        if (organizeMode != null && !organizeMode.isBlank() && !ORGANIZE_MODE_COPY.equals(organizeMode)) {
            throw new BusinessException(ErrorCode.WORKER_REJECTED_IMPORT_TASK, "only copy organizeMode is supported in MVP1");
        }
    }

    private LocalScanRequest toLocalScanRequest(RunLocalImportJobRequest request) {
        return new LocalScanRequest(
                Path.of(request.inputPath()),
                rawSourcesRoot(request),
                request.recursive(),
                request.skipHidden(),
                request.skipTemporary(),
                request.followSymlinks(),
                request.maxCopyFileSizeMb()
        );
    }

    private Path rawSourcesRoot(RunLocalImportJobRequest request) {
        String rawSourcesRoot = hasText(request.rawSourcesRoot())
                ? request.rawSourcesRoot()
                : configuredRawSourcesRoot;
        if (!hasText(rawSourcesRoot)) {
            throw new BusinessException(ErrorCode.SOURCE_INVALID_PATH, "rawSourcesRoot is required");
        }
        return Path.of(rawSourcesRoot);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private SubmitSourceFilesBatchRequest toBatchRequest(List<LocalScanFile> files) {
        return new SubmitSourceFilesBatchRequest(files.stream()
                .map(this::toBatchItem)
                .toList());
    }

    private SubmitSourceFileItem toBatchItem(LocalScanFile file) {
        return new SubmitSourceFileItem(
                file.fileName(),
                file.fileExt(),
                file.originalPath(),
                file.managedPath(),
                file.fileSize(),
                file.mimeType(),
                file.contentHash(),
                file.parseStatus(),
                file.organizeStatus(),
                null
        );
    }
}
