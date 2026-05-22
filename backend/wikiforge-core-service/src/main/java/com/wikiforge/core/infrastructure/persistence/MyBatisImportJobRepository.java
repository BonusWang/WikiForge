package com.wikiforge.core.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wikiforge.core.domain.model.ImportJob;
import com.wikiforge.core.domain.model.ImportJobPage;
import com.wikiforge.core.domain.repository.ImportJobRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisImportJobRepository implements ImportJobRepository {

    private final ImportJobMapper importJobMapper;

    public MyBatisImportJobRepository(ImportJobMapper importJobMapper) {
        this.importJobMapper = importJobMapper;
    }

    @Override
    public void save(ImportJob importJob) {
        ImportJobEntity entity = toEntity(importJob);
        importJobMapper.insert(entity);
        importJob.setId(entity.getId());
    }

    @Override
    public void update(ImportJob importJob) {
        importJobMapper.updateById(toEntity(importJob));
    }

    @Override
    public Optional<ImportJob> findByJobUid(String jobUid) {
        LambdaQueryWrapper<ImportJobEntity> wrapper = new LambdaQueryWrapper<ImportJobEntity>()
                .eq(ImportJobEntity::getJobUid, jobUid);
        return Optional.ofNullable(importJobMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public ImportJobPage findPage(String status, int page, int pageSize) {
        LambdaQueryWrapper<ImportJobEntity> wrapper = new LambdaQueryWrapper<ImportJobEntity>()
                .orderByDesc(ImportJobEntity::getCreatedAt)
                .orderByDesc(ImportJobEntity::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ImportJobEntity::getStatus, status);
        }

        Page<ImportJobEntity> result = importJobMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new ImportJobPage(
                result.getRecords().stream().map(this::toDomain).toList(),
                page,
                pageSize,
                result.getTotal()
        );
    }

    private ImportJobEntity toEntity(ImportJob importJob) {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(importJob.getId());
        entity.setJobUid(importJob.getJobUid());
        entity.setImportType(importJob.getImportType());
        entity.setInputPath(importJob.getInputPath());
        entity.setRawSourcesRoot(importJob.getRawSourcesRoot());
        entity.setRecursive(importJob.getRecursive());
        entity.setOrganizeMode(importJob.getOrganizeMode());
        entity.setMaxCopyFileSizeMb(importJob.getMaxCopyFileSizeMb());
        entity.setStatus(importJob.getStatus());
        entity.setTotalCount(importJob.getTotalCount());
        entity.setSuccessCount(importJob.getSuccessCount());
        entity.setSkippedCount(importJob.getSkippedCount());
        entity.setFailedCount(importJob.getFailedCount());
        entity.setStartedAt(importJob.getStartedAt());
        entity.setFinishedAt(importJob.getFinishedAt());
        entity.setErrorMessage(importJob.getErrorMessage());
        entity.setCreatedAt(importJob.getCreatedAt());
        entity.setUpdatedAt(importJob.getUpdatedAt());
        return entity;
    }

    private ImportJob toDomain(ImportJobEntity entity) {
        ImportJob importJob = new ImportJob();
        importJob.setId(entity.getId());
        importJob.setJobUid(entity.getJobUid());
        importJob.setImportType(entity.getImportType());
        importJob.setInputPath(entity.getInputPath());
        importJob.setRawSourcesRoot(entity.getRawSourcesRoot());
        importJob.setRecursive(entity.getRecursive());
        importJob.setOrganizeMode(entity.getOrganizeMode());
        importJob.setMaxCopyFileSizeMb(entity.getMaxCopyFileSizeMb());
        importJob.setStatus(entity.getStatus());
        importJob.setTotalCount(entity.getTotalCount());
        importJob.setSuccessCount(entity.getSuccessCount());
        importJob.setSkippedCount(entity.getSkippedCount());
        importJob.setFailedCount(entity.getFailedCount());
        importJob.setStartedAt(entity.getStartedAt());
        importJob.setFinishedAt(entity.getFinishedAt());
        importJob.setErrorMessage(entity.getErrorMessage());
        importJob.setCreatedAt(entity.getCreatedAt());
        importJob.setUpdatedAt(entity.getUpdatedAt());
        return importJob;
    }
}
