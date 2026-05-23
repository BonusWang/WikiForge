package com.wikiforge.core.application.service;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.common.filesystem.PathSafety;
import com.wikiforge.core.application.dto.CreateLocalImportJobRequest;
import com.wikiforge.core.application.dto.ImportJobPageResponse;
import com.wikiforge.core.application.dto.ImportJobResponse;
import com.wikiforge.core.application.dto.RunLocalImportJobRequest;
import com.wikiforge.core.application.dto.SourceFileResponse;
import com.wikiforge.core.application.dto.SubmitSourceFileItem;
import com.wikiforge.core.application.dto.SubmitSourceFilesBatchRequest;
import com.wikiforge.core.application.dto.UpdateImportJobStatusRequest;
import com.wikiforge.core.application.port.WorkerImportJobClient;
import com.wikiforge.core.domain.model.ImportJob;
import com.wikiforge.core.domain.model.ImportJobPage;
import com.wikiforge.core.domain.model.ImportJobStatus;
import com.wikiforge.core.domain.model.ImportType;
import com.wikiforge.core.domain.model.ObsidianNote;
import com.wikiforge.core.domain.model.OrganizeMode;
import com.wikiforge.core.domain.model.ParseStatus;
import com.wikiforge.core.domain.model.RawOrganizeStatus;
import com.wikiforge.core.domain.model.SourceFilePage;
import com.wikiforge.core.domain.model.SourceFileRecord;
import com.wikiforge.core.domain.model.SourceFileSubmission;
import com.wikiforge.core.domain.repository.ImportJobRepository;
import com.wikiforge.core.domain.repository.ObsidianNoteRepository;
import com.wikiforge.core.domain.repository.SourceFileRepository;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ImportJobService {

    private final ImportJobRepository importJobRepository;
    private final SourceFileRepository sourceFileRepository;
    private final ObsidianNoteRepository obsidianNoteRepository;
    private final CoreRuntimeProperties runtimeProperties;
    private final WorkerImportJobClient workerImportJobClient;

    public ImportJobService(
            ImportJobRepository importJobRepository,
            SourceFileRepository sourceFileRepository,
            ObsidianNoteRepository obsidianNoteRepository,
            CoreRuntimeProperties runtimeProperties,
            WorkerImportJobClient workerImportJobClient
    ) {
        this.importJobRepository = importJobRepository;
        this.sourceFileRepository = sourceFileRepository;
        this.obsidianNoteRepository = obsidianNoteRepository;
        this.runtimeProperties = runtimeProperties;
        this.workerImportJobClient = workerImportJobClient;
    }

    @Transactional
    public ImportJobResponse createLocalImportJob(CreateLocalImportJobRequest request) {
        Path inputPath = PathSafety.normalizeAbsolute(Path.of(request.inputPath()));
        Path rawSourcesRoot = PathSafety.normalizeAbsolute(Path.of(request.rawSourcesRoot()));
        if (!Files.exists(inputPath)) {
            throw new BusinessException(ErrorCode.SOURCE_PATH_NOT_FOUND);
        }
        if (!Files.isDirectory(inputPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new BusinessException(ErrorCode.SOURCE_UNSUPPORTED_INPUT_TYPE);
        }
        ensureRawSourcesRootMatchesConfig(rawSourcesRoot);
        ensureInputPathAllowed(inputPath);
        PathSafety.ensureNoOverlap(inputPath, rawSourcesRoot);

        OrganizeMode organizeMode = request.organizeMode() == null || request.organizeMode().isBlank()
                ? OrganizeMode.COPY
                : OrganizeMode.fromValue(request.organizeMode());

        LocalDateTime now = LocalDateTime.now();
        ImportJob importJob = new ImportJob();
        importJob.setJobUid(nextJobUid());
        importJob.setImportType(ImportType.PATH_SCAN.value());
        importJob.setInputPath(inputPath.toString());
        importJob.setRawSourcesRoot(rawSourcesRoot.toString());
        importJob.setRecursive(request.recursive() == null ? Boolean.TRUE : request.recursive());
        importJob.setOrganizeMode(organizeMode.value());
        importJob.setMaxCopyFileSizeMb(
                request.maxCopyFileSizeMb() == null ? 100 : request.maxCopyFileSizeMb()
        );
        importJob.setStatus(ImportJobStatus.PENDING.value());
        importJob.setTotalCount(0);
        importJob.setSuccessCount(0);
        importJob.setSkippedCount(0);
        importJob.setFailedCount(0);
        importJob.setCreatedAt(now);
        importJob.setUpdatedAt(now);
        importJobRepository.save(importJob);
        ImportJobResponse response = toResponse(importJob);
        dispatchWorkerAfterCommit(importJob);
        return response;
    }

    @Transactional(readOnly = true)
    public ImportJobPageResponse listImportJobs(String status, int page, int pageSize) {
        if (status != null && !status.isBlank()) {
            ImportJobStatus.fromValue(status);
        }
        ImportJobPage result = importJobRepository.findPage(status, normalizePage(page), normalizePageSize(pageSize));
        return new ImportJobPageResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.pageSize(),
                result.total()
        );
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getImportJob(String jobUid) {
        return toResponse(findImportJob(jobUid));
    }

    @Transactional
    public void updateStatus(String jobUid, UpdateImportJobStatusRequest request) {
        ImportJob importJob = findImportJob(jobUid);
        ImportJobStatus current = ImportJobStatus.fromValue(importJob.getStatus());
        ImportJobStatus next = ImportJobStatus.fromValue(request.status());
        if (!current.canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.IMPORT_INVALID_STATUS_TRANSITION);
        }

        LocalDateTime now = LocalDateTime.now();
        importJob.setStatus(next.value());
        if (request.totalCount() != null) {
            importJob.setTotalCount(request.totalCount());
        }
        if (request.successCount() != null) {
            importJob.setSuccessCount(request.successCount());
        }
        if (request.skippedCount() != null) {
            importJob.setSkippedCount(request.skippedCount());
        }
        if (request.failedCount() != null) {
            importJob.setFailedCount(request.failedCount());
        }
        importJob.setErrorMessage(request.errorMessage());
        if (next == ImportJobStatus.RUNNING && importJob.getStartedAt() == null) {
            importJob.setStartedAt(now);
        }
        if (next == ImportJobStatus.COMPLETED || next == ImportJobStatus.FAILED || next == ImportJobStatus.CANCELLED) {
            importJob.setFinishedAt(now);
        }
        importJob.setUpdatedAt(now);
        importJobRepository.update(importJob);
    }

    @Transactional
    public void submitSourceFiles(String jobUid, SubmitSourceFilesBatchRequest request) {
        ImportJob importJob = findImportJob(jobUid);
        List<SourceFileSubmission> files = request.files().stream()
                .map(this::toSubmission)
                .toList();
        sourceFileRepository.saveAll(importJob, files);
    }

    public boolean hasValidInternalToken(String token) {
        String configuredToken = runtimeProperties.internalApiToken();
        return configuredToken != null && !configuredToken.isBlank() && configuredToken.equals(token);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listSourceFiles(String jobUid, int page, int pageSize) {
        ImportJob importJob = findImportJob(jobUid);
        SourceFilePage result = sourceFileRepository.findByJob(
                importJob,
                normalizePage(page),
                normalizePageSize(pageSize)
        );
        return Map.of(
                "items", result.items().stream().map(this::toResponse).toList(),
                "page", result.page(),
                "pageSize", result.pageSize(),
                "total", result.total()
        );
    }

    private SourceFileSubmission toSubmission(SubmitSourceFileItem item) {
        ParseStatus.fromValue(item.parseStatus());
        RawOrganizeStatus.fromValue(item.organizeStatus());
        return new SourceFileSubmission(
                item.fileName(),
                item.fileExt(),
                item.originalPath(),
                item.managedPath(),
                item.fileSize(),
                item.mimeType(),
                item.contentHash(),
                item.parserName(),
                item.parseStatus(),
                item.organizeStatus(),
                item.duplicateOfFileUid(),
                item.contentType(),
                item.parsedText(),
                item.textHash(),
                item.charCount(),
                item.rawTextSaved(),
                item.parseError()
        );
    }

    private ImportJob findImportJob(String jobUid) {
        return importJobRepository.findByJobUid(jobUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_JOB_NOT_FOUND));
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 200);
    }

    private ImportJobResponse toResponse(ImportJob importJob) {
        return new ImportJobResponse(
                importJob.getJobUid(),
                importJob.getImportType(),
                importJob.getInputPath(),
                importJob.getRawSourcesRoot(),
                importJob.getRecursive(),
                importJob.getOrganizeMode(),
                importJob.getMaxCopyFileSizeMb(),
                importJob.getStatus(),
                importJob.getTotalCount(),
                importJob.getSuccessCount(),
                importJob.getSkippedCount(),
                importJob.getFailedCount(),
                toOffset(importJob.getCreatedAt()),
                toOffset(importJob.getStartedAt()),
                toOffset(importJob.getFinishedAt()),
                importJob.getErrorMessage()
        );
    }

    private SourceFileResponse toResponse(SourceFileRecord sourceFile) {
        ObsidianNote obsidianNote = obsidianNoteRepository.findBySourceFileUid(sourceFile.fileUid()).orElse(null);
        return new SourceFileResponse(
                sourceFile.fileUid(),
                sourceFile.sourceUid(),
                sourceFile.jobUid(),
                sourceFile.fileName(),
                sourceFile.fileExt(),
                sourceFile.originalPath(),
                sourceFile.managedPath(),
                sourceFile.fileSize(),
                sourceFile.mimeType(),
                sourceFile.contentHash(),
                sourceFile.parseStatus(),
                sourceFile.organizeStatus(),
                sourceFile.duplicateOfFileUid(),
                obsidianNote == null ? null : obsidianNote.noteUid(),
                obsidianNote == null ? null : obsidianNote.status(),
                obsidianNote == null ? null : obsidianNote.title(),
                obsidianNote == null ? null : obsidianNote.vaultPath(),
                obsidianNote == null ? null : obsidianNote.obsidianUri(),
                obsidianNote == null ? null : toOffset(obsidianNote.createdAt()),
                toOffset(sourceFile.createdAt())
        );
    }

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String nextJobUid() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "job_" + date + "_" + randomSuffix;
    }

    private void dispatchWorkerAfterCommit(ImportJob importJob) {
        RunLocalImportJobRequest workerRequest = new RunLocalImportJobRequest(
                importJob.getJobUid(),
                importJob.getInputPath(),
                importJob.getRawSourcesRoot(),
                importJob.getRecursive(),
                importJob.getOrganizeMode(),
                importJob.getMaxCopyFileSizeMb(),
                true,
                true,
                false
        );
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatchWorker(importJob, workerRequest);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatchWorker(importJob, workerRequest);
            }
        });
    }

    private void dispatchWorker(ImportJob importJob, RunLocalImportJobRequest workerRequest) {
        try {
            workerImportJobClient.startLocalImportJob(workerRequest);
        } catch (RuntimeException exception) {
            markWorkerDispatchFailed(importJob, exception);
        }
    }

    private void markWorkerDispatchFailed(ImportJob importJob, RuntimeException exception) {
        ImportJob failedJob = importJobRepository.findByJobUid(importJob.getJobUid()).orElse(importJob);
        failedJob.setStatus(ImportJobStatus.FAILED.value());
        failedJob.setErrorMessage(exception.getMessage());
        failedJob.setFinishedAt(LocalDateTime.now());
        failedJob.setUpdatedAt(LocalDateTime.now());
        importJobRepository.update(failedJob);
    }

    private void ensureRawSourcesRootMatchesConfig(Path requestRawSourcesRoot) {
        String configuredValue = runtimeProperties.rawSourcesRoot();
        if (configuredValue == null) {
            throw new BusinessException(ErrorCode.SOURCE_INVALID_PATH, "rawSourcesRoot is not configured");
        }
        Path configuredRawSourcesRoot = PathSafety.normalizeAbsolute(Path.of(configuredValue));
        if (!configuredRawSourcesRoot.equals(requestRawSourcesRoot)) {
            throw new BusinessException(
                    ErrorCode.SOURCE_INVALID_PATH,
                    "rawSourcesRoot must match configured raw sources root"
            );
        }
    }

    private void ensureInputPathAllowed(Path inputPath) {
        List<String> allowedRoots = runtimeProperties.allowedScanRoots();
        if (allowedRoots.isEmpty()) {
            return;
        }

        Path realInputPath = toRealPath(inputPath);
        boolean allowed = allowedRoots.stream()
                .map(Path::of)
                .map(PathSafety::normalizeAbsolute)
                .map(this::toRealPathIfExists)
                .anyMatch(realInputPath::startsWith);
        if (!allowed) {
            throw new BusinessException(ErrorCode.SOURCE_INVALID_PATH, "inputPath is outside allowed scan roots");
        }
    }

    private Path toRealPath(Path path) {
        try {
            return path.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (java.io.IOException exception) {
            throw new BusinessException(ErrorCode.SOURCE_INVALID_PATH, "path cannot be resolved");
        }
    }

    private Path toRealPathIfExists(Path path) {
        if (!Files.exists(path)) {
            return path;
        }
        return toRealPath(path);
    }
}
