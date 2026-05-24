package com.wikiforge.core.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.core.application.dto.CreateWikiIngestRunRequest;
import com.wikiforge.core.application.dto.WikiIngestRunPageResponse;
import com.wikiforge.core.application.dto.WikiIngestRunResponse;
import com.wikiforge.core.domain.model.SourceFileRecord;
import com.wikiforge.core.domain.repository.SourceFileRepository;
import com.wikiforge.core.infrastructure.persistence.WikiIngestRunEntity;
import com.wikiforge.core.infrastructure.persistence.WikiIngestRunMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiIngestRunService {

    private static final Set<String> ALLOWED_WRITE_MODES = Set.of("自动", "兜底", "仅预览");

    private final SourceFileRepository sourceFileRepository;
    private final WikiIngestRunMapper wikiIngestRunMapper;
    private final ObsidianVaultService obsidianVaultService;

    public WikiIngestRunService(
            SourceFileRepository sourceFileRepository,
            WikiIngestRunMapper wikiIngestRunMapper,
            ObsidianVaultService obsidianVaultService
    ) {
        this.sourceFileRepository = sourceFileRepository;
        this.wikiIngestRunMapper = wikiIngestRunMapper;
        this.obsidianVaultService = obsidianVaultService;
    }

    @Transactional
    public WikiIngestRunResponse createRun(String fileUid, CreateWikiIngestRunRequest request) {
        validateWriteMode(request == null ? null : request.writeMode());
        SourceFileRecord sourceFile = sourceFileRepository.findByFileUid(fileUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_FILE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        WikiIngestRunEntity entity = new WikiIngestRunEntity();
        entity.setRunUid(nextRunUid());
        entity.setSourceFileId(sourceFile.sourceFileId());
        entity.setFileUid(sourceFile.fileUid());
        entity.setFileName(sourceFile.fileName());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        try {
            ObsidianVaultService.WikiIngestWriteResult writeResult =
                    obsidianVaultService.writeWikiIngestSourcePage(entity.getRunUid(), sourceFile);
            entity.setStatusCode("已写入");
            entity.setStatusLabel("已写入");
            entity.setSourcePagePath(writeResult.sourcePagePath());
            entity.setWikiPagePaths(String.join("\n", writeResult.wikiPagePaths()));
            entity.setIndexUpdated(writeResult.indexUpdated());
            entity.setLogEntryAppended(writeResult.logEntryAppended());
            entity.setWriteStatusCode("已写入");
            entity.setWriteStatusLabel("已写入");
            entity.setManagedBlockPreview(writeResult.managedBlockPreview());
            entity.setLogEntryPreview(writeResult.logEntryPreview());
            entity.setObsidianUri(writeResult.obsidianUri());
            entity.setRetryable(false);
            entity.setCompletedAt(now);
        } catch (BusinessException exception) {
            entity.setStatusCode("失败");
            entity.setStatusLabel("失败");
            entity.setWikiPagePaths("[]");
            entity.setIndexUpdated(false);
            entity.setLogEntryAppended(false);
            entity.setWriteStatusCode("失败");
            entity.setWriteStatusLabel("失败");
            entity.setFallbackReason("请检查 Obsidian Vault 设置后重试");
            entity.setFailureReason(exception.getMessage());
            entity.setRetryable(true);
            entity.setCompletedAt(now);
        }
        wikiIngestRunMapper.insert(entity);
        return toResponse(entity);
    }

    private void validateWriteMode(String writeMode) {
        if (writeMode == null || writeMode.isBlank()) {
            return;
        }
        if (!ALLOWED_WRITE_MODES.contains(writeMode.trim())) {
            throw new BusinessException(ErrorCode.WIKI_INVALID_INPUT, "writeMode must be 自动、兜底 or 仅预览");
        }
    }

    @Transactional(readOnly = true)
    public WikiIngestRunPageResponse listRuns(String statusCode, String fileUid, int page, int pageSize) {
        LambdaQueryWrapper<WikiIngestRunEntity> wrapper =
                new LambdaQueryWrapper<WikiIngestRunEntity>().orderByDesc(WikiIngestRunEntity::getCreatedAt);
        if (statusCode != null && !statusCode.isBlank()) {
            wrapper.eq(WikiIngestRunEntity::getStatusCode, statusCode.trim());
        }
        if (fileUid != null && !fileUid.isBlank()) {
            wrapper.eq(WikiIngestRunEntity::getFileUid, fileUid.trim());
        }

        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<WikiIngestRunEntity> result = wikiIngestRunMapper.selectPage(
                new Page<>(normalizedPage, normalizedPageSize),
                wrapper
        );
        return new WikiIngestRunPageResponse(
                result.getRecords().stream().map(this::toResponse).toList(),
                normalizedPage,
                normalizedPageSize,
                result.getTotal()
        );
    }

    @Transactional(readOnly = true)
    public WikiIngestRunResponse getRun(String runUid) {
        WikiIngestRunEntity entity = wikiIngestRunMapper.selectOne(
                new LambdaQueryWrapper<WikiIngestRunEntity>()
                        .eq(WikiIngestRunEntity::getRunUid, runUid)
        );
        if (entity == null) {
            throw new BusinessException(ErrorCode.WIKI_INGEST_RUN_NOT_FOUND);
        }
        return toResponse(entity);
    }

    private WikiIngestRunResponse toResponse(WikiIngestRunEntity entity) {
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

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String nextRunUid() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "wir_" + date + "_" + randomSuffix;
    }
}
