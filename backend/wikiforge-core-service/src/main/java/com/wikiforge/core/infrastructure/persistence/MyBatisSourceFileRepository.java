package com.wikiforge.core.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wikiforge.core.domain.model.ImportJob;
import com.wikiforge.core.domain.model.RawOrganizeStatus;
import com.wikiforge.core.domain.model.SourceFilePage;
import com.wikiforge.core.domain.model.SourceFileRecord;
import com.wikiforge.core.domain.model.SourceFileSubmission;
import com.wikiforge.core.domain.model.SourceStatus;
import com.wikiforge.core.domain.repository.SourceFileRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSourceFileRepository implements SourceFileRepository {

    private final SourceMapper sourceMapper;
    private final SourceFileMapper sourceFileMapper;
    private final ImportJobMapper importJobMapper;

    public MyBatisSourceFileRepository(
            SourceMapper sourceMapper,
            SourceFileMapper sourceFileMapper,
            ImportJobMapper importJobMapper
    ) {
        this.sourceMapper = sourceMapper;
        this.sourceFileMapper = sourceFileMapper;
        this.importJobMapper = importJobMapper;
    }

    @Override
    public void saveAll(ImportJob importJob, List<SourceFileSubmission> files) {
        for (SourceFileSubmission file : files) {
            if (findExistingSubmission(importJob, file) != null) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            SourceFileEntity duplicate = findDuplicate(file.contentHash());
            String organizeStatus = duplicate == null
                    ? file.organizeStatus()
                    : RawOrganizeStatus.DUPLICATE.value();

            SourceEntity source = new SourceEntity();
            source.setSourceUid(newUid("src"));
            source.setTitle(file.fileName());
            source.setSourceType(sourceType(file.fileExt()));
            source.setSourcePlatform("local");
            source.setRawOriginalPath(file.originalPath());
            source.setRawManagedPath(file.managedPath());
            source.setRawOrganizeStatus(organizeStatus);
            source.setContentHash(file.contentHash());
            source.setStatus(sourceStatus(organizeStatus));
            source.setCollectedAt(now);
            source.setCreatedAt(now);
            source.setUpdatedAt(now);
            sourceMapper.insert(source);

            SourceFileEntity sourceFile = new SourceFileEntity();
            sourceFile.setFileUid(newUid("file"));
            sourceFile.setSourceId(source.getId());
            sourceFile.setImportJobId(importJob.getId());
            sourceFile.setFileName(file.fileName());
            sourceFile.setFileExt(file.fileExt());
            sourceFile.setOriginalPath(file.originalPath());
            sourceFile.setManagedPath(file.managedPath());
            sourceFile.setFileSize(file.fileSize());
            sourceFile.setMimeType(file.mimeType());
            sourceFile.setContentHash(file.contentHash());
            sourceFile.setParseStatus(file.parseStatus());
            sourceFile.setOrganizeStatus(organizeStatus);
            sourceFile.setDuplicateOfFileId(duplicate == null ? null : duplicate.getId());
            sourceFile.setCreatedAt(now);
            sourceFileMapper.insert(sourceFile);
        }
    }

    @Override
    public SourceFilePage findByJob(ImportJob importJob, int page, int pageSize) {
        LambdaQueryWrapper<SourceFileEntity> wrapper = new LambdaQueryWrapper<SourceFileEntity>()
                .eq(SourceFileEntity::getImportJobId, importJob.getId())
                .orderByAsc(SourceFileEntity::getId);
        Page<SourceFileEntity> result = sourceFileMapper.selectPage(new Page<>(page, pageSize), wrapper);

        return new SourceFilePage(
                result.getRecords().stream()
                        .map(sourceFile -> toRecord(importJob, sourceFile))
                        .toList(),
                page,
                pageSize,
                result.getTotal()
        );
    }

    @Override
    public Optional<SourceFileRecord> findByFileUid(String fileUid) {
        SourceFileEntity sourceFile = sourceFileMapper.selectOne(
                new LambdaQueryWrapper<SourceFileEntity>()
                        .eq(SourceFileEntity::getFileUid, fileUid)
        );
        if (sourceFile == null) {
            return Optional.empty();
        }
        SourceEntity source = sourceMapper.selectById(sourceFile.getSourceId());
        ImportJobEntity importJob = importJobMapper.selectById(sourceFile.getImportJobId());
        if (source == null || importJob == null) {
            return Optional.empty();
        }
        return Optional.of(toRecord(importJob.getJobUid(), source, sourceFile));
    }

    private SourceFileRecord toRecord(ImportJob importJob, SourceFileEntity sourceFile) {
        SourceEntity source = sourceMapper.selectById(sourceFile.getSourceId());
        return toRecord(importJob.getJobUid(), source, sourceFile);
    }

    private SourceFileRecord toRecord(String jobUid, SourceEntity source, SourceFileEntity sourceFile) {
        return new SourceFileRecord(
                sourceFile.getId(),
                source.getId(),
                sourceFile.getFileUid(),
                source.getSourceUid(),
                jobUid,
                sourceFile.getFileName(),
                sourceFile.getFileExt(),
                sourceFile.getOriginalPath(),
                sourceFile.getManagedPath(),
                sourceFile.getFileSize(),
                sourceFile.getMimeType(),
                sourceFile.getContentHash(),
                sourceFile.getParseStatus(),
                sourceFile.getOrganizeStatus(),
                duplicateFileUid(sourceFile.getDuplicateOfFileId()),
                sourceFile.getCreatedAt()
        );
    }

    private SourceFileEntity findExistingSubmission(ImportJob importJob, SourceFileSubmission file) {
        LambdaQueryWrapper<SourceFileEntity> wrapper = new LambdaQueryWrapper<SourceFileEntity>()
                .eq(SourceFileEntity::getImportJobId, importJob.getId())
                .eq(SourceFileEntity::getOriginalPath, file.originalPath());
        if (file.contentHash() == null) {
            wrapper.isNull(SourceFileEntity::getContentHash);
        } else {
            wrapper.eq(SourceFileEntity::getContentHash, file.contentHash());
        }
        List<SourceFileEntity> matches = sourceFileMapper.selectList(wrapper);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private SourceFileEntity findDuplicate(String contentHash) {
        if (contentHash == null || contentHash.isBlank()) {
            return null;
        }
        List<SourceFileEntity> matches = sourceFileMapper.selectList(
                new LambdaQueryWrapper<SourceFileEntity>()
                        .eq(SourceFileEntity::getContentHash, contentHash)
                        .orderByAsc(SourceFileEntity::getId)
        );
        return matches.isEmpty() ? null : matches.get(0);
    }

    private String duplicateFileUid(Long duplicateOfFileId) {
        if (duplicateOfFileId == null) {
            return null;
        }
        SourceFileEntity duplicate = sourceFileMapper.selectById(duplicateOfFileId);
        return duplicate == null ? null : duplicate.getFileUid();
    }

    private String sourceStatus(String organizeStatus) {
        if (RawOrganizeStatus.COPIED.value().equals(organizeStatus)
                || RawOrganizeStatus.DUPLICATE.value().equals(organizeStatus)) {
            return SourceStatus.ORGANIZED.value();
        }
        if (RawOrganizeStatus.FAILED.value().equals(organizeStatus)) {
            return SourceStatus.FAILED.value();
        }
        return SourceStatus.PENDING.value();
    }

    private String sourceType(String fileExt) {
        if (fileExt == null || fileExt.isBlank()) {
            return "file";
        }
        return fileExt.toLowerCase(Locale.ROOT);
    }

    private String newUid(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
