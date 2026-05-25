package com.wikiforge.core.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wikiforge.core.domain.model.ImportJob;
import com.wikiforge.core.domain.model.RawOrganizeStatus;
import com.wikiforge.core.domain.model.SourceFilePage;
import com.wikiforge.core.domain.model.SourceFileRecord;
import com.wikiforge.core.domain.model.SourceFileSubmission;
import com.wikiforge.core.domain.repository.SourceFileRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSourceFileRepository implements SourceFileRepository {

    private final SourceFileMapper sourceFileMapper;
    private final SourceContentMapper sourceContentMapper;
    private final ImportJobMapper importJobMapper;

    public MyBatisSourceFileRepository(
            SourceFileMapper sourceFileMapper,
            SourceContentMapper sourceContentMapper,
            ImportJobMapper importJobMapper
    ) {
        this.sourceFileMapper = sourceFileMapper;
        this.sourceContentMapper = sourceContentMapper;
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

            SourceFileEntity sourceFile = new SourceFileEntity();
            sourceFile.setFileUid(newUid("file"));
            sourceFile.setImportJobId(importJob.getId());
            sourceFile.setFileName(file.fileName());
            sourceFile.setFileExt(file.fileExt());
            sourceFile.setOriginalPath(file.originalPath());
            sourceFile.setManagedPath(file.managedPath());
            sourceFile.setFileSize(file.fileSize());
            sourceFile.setMimeType(file.mimeType());
            sourceFile.setContentHash(file.contentHash());
            sourceFile.setParserName(file.parserName());
            sourceFile.setParseStatus(file.parseStatus());
            sourceFile.setOrganizeStatus(organizeStatus);
            sourceFile.setDuplicateOfFileId(duplicate == null ? null : duplicate.getId());
            sourceFile.setParseError(file.parseError());
            sourceFile.setCreatedAt(now);
            sourceFileMapper.insert(sourceFile);
            saveSourceContent(sourceFile, file, now);
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
        ImportJobEntity importJob = importJobMapper.selectById(sourceFile.getImportJobId());
        if (importJob == null) {
            return Optional.empty();
        }
        return Optional.of(toRecord(importJob.getJobUid(), sourceFile));
    }

    private SourceFileRecord toRecord(ImportJob importJob, SourceFileEntity sourceFile) {
        return toRecord(importJob.getJobUid(), sourceFile);
    }

    private SourceFileRecord toRecord(String jobUid, SourceFileEntity sourceFile) {
        return new SourceFileRecord(
                sourceFile.getId(),
                sourceFile.getFileUid(),
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
                sourceFile.getParseError(),
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

    private void saveSourceContent(
            SourceFileEntity sourceFile,
            SourceFileSubmission file,
            LocalDateTime now
    ) {
        if (!hasText(file.parsedText()) && !hasText(file.parserName()) && !hasText(file.parseError())) {
            return;
        }
        SourceContentEntity content = new SourceContentEntity();
        content.setContentUid(newUid("content"));
        content.setSourceFileId(sourceFile.getId());
        content.setParserName(file.parserName());
        content.setContentType(hasText(file.contentType()) ? file.contentType() : "plain_text");
        content.setRawText(file.parsedText());
        content.setTextHash(file.textHash());
        content.setCharCount(file.charCount() == null ? charCount(file.parsedText()) : file.charCount());
        content.setRawTextSaved(Boolean.TRUE.equals(file.rawTextSaved()));
        content.setParseStatus(file.parseStatus());
        content.setParseError(file.parseError());
        content.setCreatedAt(now);
        content.setUpdatedAt(now);
        sourceContentMapper.insert(content);
    }

    private int charCount(String text) {
        return text == null ? 0 : text.length();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String duplicateFileUid(Long duplicateOfFileId) {
        if (duplicateOfFileId == null) {
            return null;
        }
        SourceFileEntity duplicate = sourceFileMapper.selectById(duplicateOfFileId);
        return duplicate == null ? null : duplicate.getFileUid();
    }

    private String newUid(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
