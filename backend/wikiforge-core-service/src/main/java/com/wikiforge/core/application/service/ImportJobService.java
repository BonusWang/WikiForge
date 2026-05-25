package com.wikiforge.core.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.wikiforge.core.application.dto.UploadSourcesResponse;
import com.wikiforge.core.application.dto.UpdateImportJobStatusRequest;
import com.wikiforge.core.application.dto.WikiIngestRunResponse;
import com.wikiforge.core.application.port.WorkerImportJobClient;
import com.wikiforge.core.domain.model.ImportJob;
import com.wikiforge.core.domain.model.ImportJobPage;
import com.wikiforge.core.domain.model.ImportJobStatus;
import com.wikiforge.core.domain.model.ImportType;
import com.wikiforge.core.domain.model.OrganizeMode;
import com.wikiforge.core.domain.model.ParseStatus;
import com.wikiforge.core.domain.model.RawOrganizeStatus;
import com.wikiforge.core.domain.model.SourceFilePage;
import com.wikiforge.core.domain.model.SourceFileRecord;
import com.wikiforge.core.domain.model.SourceFileSubmission;
import com.wikiforge.core.domain.repository.ImportJobRepository;
import com.wikiforge.core.domain.repository.SourceFileRepository;
import com.wikiforge.core.infrastructure.persistence.WikiIngestRunEntity;
import com.wikiforge.core.infrastructure.persistence.WikiIngestRunMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportJobService {

    private final ImportJobRepository importJobRepository;
    private final SourceFileRepository sourceFileRepository;
    private final WikiIngestRunMapper wikiIngestRunMapper;
    private final CoreRuntimeProperties runtimeProperties;
    private final WorkerImportJobClient workerImportJobClient;
    private final UploadedTextContentExtractor uploadedTextContentExtractor;

    public ImportJobService(
            ImportJobRepository importJobRepository,
            SourceFileRepository sourceFileRepository,
            WikiIngestRunMapper wikiIngestRunMapper,
            CoreRuntimeProperties runtimeProperties,
            WorkerImportJobClient workerImportJobClient,
            UploadedTextContentExtractor uploadedTextContentExtractor
    ) {
        this.importJobRepository = importJobRepository;
        this.sourceFileRepository = sourceFileRepository;
        this.wikiIngestRunMapper = wikiIngestRunMapper;
        this.runtimeProperties = runtimeProperties;
        this.workerImportJobClient = workerImportJobClient;
        this.uploadedTextContentExtractor = uploadedTextContentExtractor;
    }

    @Transactional
    public ImportJobResponse createLocalImportJob(CreateLocalImportJobRequest request) {
        Path inputPath = PathSafety.normalizeAbsolute(Path.of(request.inputPath()));
        Path rawSourcesRoot = resolveRawSourcesRoot(request.rawSourcesRoot());
        if (!Files.exists(inputPath)) {
            throw new BusinessException(ErrorCode.SOURCE_PATH_NOT_FOUND);
        }
        if (!Files.isDirectory(inputPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new BusinessException(ErrorCode.SOURCE_UNSUPPORTED_INPUT_TYPE);
        }
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

    @Transactional
    public UploadSourcesResponse uploadSources(List<MultipartFile> files, String wikiWritebackMode) {
        validateWikiWritebackMode(wikiWritebackMode);
        List<MultipartFile> uploadFiles = files == null
                ? List.of()
                : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (uploadFiles.isEmpty()) {
            throw new BusinessException(ErrorCode.UPLOAD_EMPTY_INPUT);
        }

        Path rawSourcesRoot = resolveRawSourcesRoot(null);
        LocalDateTime now = LocalDateTime.now();
        ImportJob importJob = new ImportJob();
        importJob.setJobUid(nextJobUid());
        importJob.setImportType(ImportType.UPLOAD.value());
        importJob.setInputPath("浏览器上传");
        importJob.setRawSourcesRoot(rawSourcesRoot.toString());
        importJob.setRecursive(false);
        importJob.setOrganizeMode(OrganizeMode.COPY.value());
        importJob.setMaxCopyFileSizeMb(100);
        importJob.setStatus(ImportJobStatus.COMPLETED.value());
        importJob.setTotalCount(uploadFiles.size());
        importJob.setSuccessCount(uploadFiles.size());
        importJob.setSkippedCount(0);
        importJob.setFailedCount(0);
        importJob.setStartedAt(now);
        importJob.setFinishedAt(now);
        importJob.setCreatedAt(now);
        importJob.setUpdatedAt(now);
        importJobRepository.save(importJob);

        List<SourceFileSubmission> submissions = new ArrayList<>();
        for (int index = 0; index < uploadFiles.size(); index++) {
            submissions.add(copyUploadedFile(
                    uploadFiles.get(index),
                    rawSourcesRoot,
                    importJob.getJobUid(),
                    index + 1
            ));
        }
        sourceFileRepository.saveAll(importJob, submissions);

        StatusDisplay statusDisplay = importJobStatusDisplay(importJob.getStatus());
        return new UploadSourcesResponse(
                importJob.getJobUid(),
                "浏览器上传",
                statusDisplay.code(),
                statusDisplay.label(),
                statusDisplay.description(),
                submissions.size(),
                toOffset(importJob.getCreatedAt())
        );
    }

    @Transactional(readOnly = true)
    public ImportJobPageResponse listImportJobs(String status, String statusCode, int page, int pageSize) {
        String normalizedStatus = resolveImportStatusFilter(status, statusCode);
        ImportJobPage result = importJobRepository.findPage(
                normalizedStatus,
                normalizePage(page),
                normalizePageSize(pageSize)
        );
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

    @Transactional(readOnly = true)
    public SourceFileResponse getSourceFile(String fileUid) {
        return sourceFileRepository.findByFileUid(fileUid)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_FILE_NOT_FOUND));
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

    private SourceFileSubmission copyUploadedFile(
            MultipartFile file,
            Path rawSourcesRoot,
            String jobUid,
            int sequence
    ) {
        Path tempPath = null;
        try {
            String safeFileName = sanitizeUploadFileName(file.getOriginalFilename());
            Path uploadDir = uploadDirectory(rawSourcesRoot);
            Files.createDirectories(uploadDir);
            tempPath = uploadDir.resolve(".upload-" + UUID.randomUUID() + ".tmp");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(input, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String contentHash = HexFormat.of().formatHex(digest.digest());
            Path managedPath = uploadDir.resolve(contentHash.substring(0, 16) + "-" + safeFileName);
            if (Files.exists(managedPath)) {
                Files.deleteIfExists(tempPath);
            } else {
                Files.move(tempPath, managedPath);
            }

            String fileExt = fileExt(safeFileName);
            UploadedTextContentExtractor.ParsedUploadTextContent parsedContent =
                    uploadedTextContentExtractor.extract(managedPath, fileExt).orElse(null);

            return new SourceFileSubmission(
                    safeFileName,
                    fileExt,
                    "browser-upload://" + jobUid + "/" + sequence + "-" + safeFileName,
                    managedPath.toString(),
                    Files.size(managedPath),
                    uploadMimeType(file, managedPath),
                    contentHash,
                    parsedContent == null ? null : parsedContent.parserName(),
                    parsedContent == null ? ParseStatus.PENDING.value() : parsedContent.parseStatus(),
                    RawOrganizeStatus.COPIED.value(),
                    null,
                    parsedContent == null ? null : parsedContent.contentType(),
                    parsedContent == null ? null : parsedContent.parsedText(),
                    parsedContent == null ? null : parsedContent.textHash(),
                    parsedContent == null ? null : parsedContent.charCount(),
                    parsedContent == null ? null : parsedContent.rawTextSaved(),
                    parsedContent == null ? null : parsedContent.parseError()
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            deleteTempFile(tempPath);
            throw new BusinessException(ErrorCode.UPLOAD_WRITE_FAILED, "upload file cannot be written");
        }
    }

    private Path uploadDirectory(Path rawSourcesRoot) {
        LocalDate today = LocalDate.now();
        return rawSourcesRoot
                .resolve("uploads")
                .resolve(String.valueOf(today.getYear()))
                .resolve("%02d".formatted(today.getMonthValue()));
    }

    private String sanitizeUploadFileName(String originalFilename) {
        String name = originalFilename == null || originalFilename.isBlank()
                ? "upload.bin"
                : originalFilename.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_").trim();
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            return "upload.bin";
        }
        if (name.length() <= 180) {
            return name;
        }
        String ext = fileExt(name);
        int extLength = ext == null ? 0 : ext.length() + 1;
        int baseLimit = Math.max(1, 180 - extLength);
        String base = name.substring(0, Math.min(baseLimit, name.length()));
        return ext == null ? base : base + "." + ext;
    }

    private String fileExt(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return null;
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.length() > 32 ? ext.substring(0, 32) : ext;
    }

    private String uploadMimeType(MultipartFile file, Path managedPath) throws IOException {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        return Files.probeContentType(managedPath);
    }

    private void validateWikiWritebackMode(String wikiWritebackMode) {
        if (wikiWritebackMode == null || wikiWritebackMode.isBlank()) {
            return;
        }
        if (!"自动".equals(wikiWritebackMode) && !"关闭".equals(wikiWritebackMode)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "wikiWritebackMode must be 自动 or 关闭");
        }
    }

    private void deleteTempFile(Path tempPath) {
        if (tempPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempPath);
        } catch (IOException ignored) {
        }
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
        StatusDisplay statusDisplay = importJobStatusDisplay(importJob.getStatus());
        return new ImportJobResponse(
                importJob.getJobUid(),
                importJob.getImportType(),
                importJob.getInputPath(),
                importJob.getRawSourcesRoot(),
                importJob.getRecursive(),
                importJob.getOrganizeMode(),
                importJob.getMaxCopyFileSizeMb(),
                importJob.getStatus(),
                statusDisplay.code(),
                statusDisplay.label(),
                statusDisplay.description(),
                statusDisplay.color(),
                statusDisplay.terminal(),
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
        WikiIngestRunEntity latestWikiIngestRun = latestWikiIngestRun(sourceFile.fileUid());
        StatusDisplay collectStatus = collectStatusDisplay(sourceFile.organizeStatus());
        StatusDisplay extractStatus = extractStatusDisplay(sourceFile.parseStatus());
        StatusDisplay wikiStatus = wikiStatusDisplay(latestWikiIngestRun);
        return new SourceFileResponse(
                sourceFile.fileUid(),
                sourceFile.jobUid(),
                sourceFile.fileName(),
                sourceFile.fileExt(),
                sourceFile.originalPath(),
                sourceFile.originalPath(),
                sourceFile.managedPath(),
                sourceFile.managedPath(),
                sourceFile.fileSize(),
                sourceFile.fileSize(),
                sourceFile.mimeType(),
                sourceFile.contentHash(),
                sourceFile.parseStatus(),
                collectStatus.code(),
                collectStatus.label(),
                extractStatus.code(),
                extractStatus.label(),
                wikiStatus.code(),
                wikiStatus.label(),
                sourceFile.organizeStatus(),
                sourceFile.duplicateOfFileUid(),
                sourceFile.parseError(),
                latestWikiIngestRun == null ? null : latestWikiIngestRun.getFailureReason(),
                latestWikiIngestRun == null ? null : toWikiIngestRunResponse(latestWikiIngestRun),
                toOffset(sourceFile.createdAt())
        );
    }

    private WikiIngestRunEntity latestWikiIngestRun(String fileUid) {
        return wikiIngestRunMapper.selectOne(
                new LambdaQueryWrapper<WikiIngestRunEntity>()
                        .eq(WikiIngestRunEntity::getFileUid, fileUid)
                        .orderByDesc(WikiIngestRunEntity::getCreatedAt)
                        .last("LIMIT 1")
        );
    }

    private WikiIngestRunResponse toWikiIngestRunResponse(WikiIngestRunEntity entity) {
        return new WikiIngestRunResponse(
                entity.getRunUid(),
                entity.getFileUid(),
                entity.getFileName(),
                entity.getStatusCode(),
                entity.getStatusLabel(),
                entity.getSourcePagePath(),
                wikiPagePaths(entity.getWikiPagePaths()),
                Boolean.TRUE.equals(entity.getIndexUpdated()),
                Boolean.TRUE.equals(entity.getLogEntryAppended()),
                entity.getWriteStatusCode(),
                entity.getWriteStatusLabel(),
                entity.getFallbackReason(),
                entity.getFailureReason(),
                entity.getManagedBlockPreview(),
                entity.getLogEntryPreview(),
                entity.getObsidianUri(),
                Boolean.TRUE.equals(entity.getRetryable()),
                toOffset(entity.getCreatedAt()),
                toOffset(entity.getCompletedAt())
        );
    }

    private List<String> wikiPagePaths(String value) {
        if (value == null || value.isBlank() || "[]".equals(value.trim())) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .toList();
    }

    private String resolveImportStatusFilter(String status, String statusCode) {
        if (statusCode != null && !statusCode.isBlank()) {
            return switch (statusCode.trim()) {
                case "已创建", "待处理" -> ImportJobStatus.PENDING.value();
                case "执行中", "处理中" -> ImportJobStatus.RUNNING.value();
                case "已完成" -> ImportJobStatus.COMPLETED.value();
                case "失败" -> ImportJobStatus.FAILED.value();
                case "已取消" -> ImportJobStatus.CANCELLED.value();
                default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED, "invalid import job statusCode");
            };
        }
        if (status != null && !status.isBlank()) {
            ImportJobStatus.fromValue(status);
            return status;
        }
        return null;
    }

    private StatusDisplay importJobStatusDisplay(String status) {
        ImportJobStatus importJobStatus = ImportJobStatus.fromValue(status);
        return switch (importJobStatus) {
            case PENDING -> new StatusDisplay("已创建", "已创建", "收纳任务已创建，等待执行", "info", false);
            case RUNNING -> new StatusDisplay("执行中", "执行中", "正在收纳和整理文件", "primary", false);
            case COMPLETED -> new StatusDisplay("已完成", "已完成", "收纳任务已完成", "success", true);
            case FAILED -> new StatusDisplay("失败", "失败", "收纳任务执行失败", "danger", true);
            case CANCELLED -> new StatusDisplay("已取消", "已取消", "收纳任务已取消", "warning", true);
        };
    }

    private StatusDisplay collectStatusDisplay(String status) {
        RawOrganizeStatus organizeStatus = RawOrganizeStatus.fromValue(status);
        return switch (organizeStatus) {
            case PENDING -> new StatusDisplay("待收纳", "待收纳", "文件等待复制到 Raw Sources", "info", false);
            case COPIED -> new StatusDisplay("已收纳", "已收纳", "文件已复制到 Raw Sources", "success", true);
            case DUPLICATE -> new StatusDisplay("重复文件", "重复文件", "文件内容已存在，已按 hash 去重", "warning", true);
            case NEED_CONFIRM -> new StatusDisplay("待确认", "待确认", "文件需要人工确认后继续", "warning", false);
            case FAILED -> new StatusDisplay("收纳失败", "收纳失败", "文件收纳失败", "danger", true);
        };
    }

    private StatusDisplay extractStatusDisplay(String status) {
        ParseStatus parseStatus = ParseStatus.fromValue(status);
        return switch (parseStatus) {
            case PENDING -> new StatusDisplay("待抽取", "待抽取", "等待正文抽取", "info", false);
            case SUCCESS -> new StatusDisplay("已抽取", "已抽取", "正文已抽取", "success", true);
            case PARTIAL -> new StatusDisplay("部分抽取", "部分抽取", "正文部分抽取成功", "warning", true);
            case FAILED -> new StatusDisplay("抽取失败", "抽取失败", "正文抽取失败", "danger", true);
        };
    }

    private StatusDisplay wikiStatusDisplay(WikiIngestRunEntity latestWikiIngestRun) {
        if (latestWikiIngestRun == null) {
            return new StatusDisplay("待整理到 Wiki", "待整理到 Wiki", "等待整理写入 Obsidian Wiki", "info", false);
        }
        if ("已写入".equals(latestWikiIngestRun.getStatusCode())
                || "兜底写入".equals(latestWikiIngestRun.getStatusCode())) {
            return new StatusDisplay("已写入 Wiki", "已写入 Wiki", "资料已写入 Obsidian Wiki", "success", true);
        }
        if ("失败".equals(latestWikiIngestRun.getStatusCode())) {
            return new StatusDisplay("失败", "失败", "Wiki 写入失败", "danger", true);
        }
        return new StatusDisplay("写入中", "写入中", "正在写入 Obsidian Wiki", "primary", false);
    }

    private record StatusDisplay(
            String code,
            String label,
            String description,
            String color,
            boolean terminal
    ) {
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

    private Path resolveRawSourcesRoot(String requestRawSourcesRoot) {
        String configuredValue = runtimeProperties.rawSourcesRoot();
        if (configuredValue == null) {
            throw new BusinessException(ErrorCode.SOURCE_INVALID_PATH, "rawSourcesRoot is not configured");
        }
        Path configuredRawSourcesRoot = normalizeConfiguredPath(configuredValue);
        if (requestRawSourcesRoot == null || requestRawSourcesRoot.isBlank()) {
            return configuredRawSourcesRoot;
        }
        Path normalizedRequestRawSourcesRoot = normalizeConfiguredPath(requestRawSourcesRoot);
        if (!configuredRawSourcesRoot.equals(normalizedRequestRawSourcesRoot)) {
            throw new BusinessException(
                    ErrorCode.SOURCE_INVALID_PATH,
                    "rawSourcesRoot must match configured raw sources root"
            );
        }
        return configuredRawSourcesRoot;
    }

    private Path normalizeConfiguredPath(String value) {
        Path path = Path.of(value);
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath();
        }
        return PathSafety.normalizeAbsolute(path);
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
