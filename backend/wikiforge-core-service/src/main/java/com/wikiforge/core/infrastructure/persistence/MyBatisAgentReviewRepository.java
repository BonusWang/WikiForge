package com.wikiforge.core.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wikiforge.core.domain.model.AgentRun;
import com.wikiforge.core.domain.model.AgentStep;
import com.wikiforge.core.domain.model.ReviewItem;
import com.wikiforge.core.domain.model.ReviewItemPage;
import com.wikiforge.core.domain.repository.AgentReviewRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAgentReviewRepository implements AgentReviewRepository {

    private final AgentRunMapper agentRunMapper;
    private final AgentStepMapper agentStepMapper;
    private final ReviewItemMapper reviewItemMapper;
    private final SourceMapper sourceMapper;
    private final SourceFileMapper sourceFileMapper;

    public MyBatisAgentReviewRepository(
            AgentRunMapper agentRunMapper,
            AgentStepMapper agentStepMapper,
            ReviewItemMapper reviewItemMapper,
            SourceMapper sourceMapper,
            SourceFileMapper sourceFileMapper
    ) {
        this.agentRunMapper = agentRunMapper;
        this.agentStepMapper = agentStepMapper;
        this.reviewItemMapper = reviewItemMapper;
        this.sourceMapper = sourceMapper;
        this.sourceFileMapper = sourceFileMapper;
    }

    @Override
    public AgentRun saveRun(AgentRun run) {
        AgentRunEntity entity = toEntity(run);
        agentRunMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public void saveStep(AgentStep step) {
        agentStepMapper.insert(toEntity(step));
    }

    @Override
    public ReviewItem saveReviewItem(ReviewItem reviewItem) {
        ReviewItemEntity entity = toEntity(reviewItem);
        reviewItemMapper.insert(entity);
        return toModel(entity);
    }

    @Override
    public Optional<AgentRun> findRunByRunUid(String runUid) {
        AgentRunEntity entity = agentRunMapper.selectOne(
                new LambdaQueryWrapper<AgentRunEntity>()
                        .eq(AgentRunEntity::getRunUid, runUid)
        );
        return Optional.ofNullable(entity).map(this::toModel);
    }

    @Override
    public Optional<ReviewItem> findLatestReviewItemByRunId(Long runId) {
        ReviewItemEntity entity = reviewItemMapper.selectOne(
                new LambdaQueryWrapper<ReviewItemEntity>()
                        .eq(ReviewItemEntity::getRunId, runId)
                        .orderByDesc(ReviewItemEntity::getId)
                        .last("LIMIT 1")
        );
        return Optional.ofNullable(entity).map(this::toModel);
    }

    @Override
    public Optional<ReviewItem> findReviewItemByReviewUid(String reviewUid) {
        ReviewItemEntity entity = reviewItemMapper.selectOne(
                new LambdaQueryWrapper<ReviewItemEntity>()
                        .eq(ReviewItemEntity::getReviewUid, reviewUid)
        );
        return Optional.ofNullable(entity).map(this::toModel);
    }

    @Override
    public ReviewItemPage findReviewItems(String status, int page, int pageSize) {
        LambdaQueryWrapper<ReviewItemEntity> wrapper = new LambdaQueryWrapper<ReviewItemEntity>()
                .orderByDesc(ReviewItemEntity::getCreatedAt)
                .orderByDesc(ReviewItemEntity::getId);
        if (hasText(status)) {
            wrapper.eq(ReviewItemEntity::getStatus, status);
        }
        Page<ReviewItemEntity> result = reviewItemMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new ReviewItemPage(
                result.getRecords().stream().map(this::toModel).toList(),
                page,
                pageSize,
                result.getTotal()
        );
    }

    @Override
    public ReviewItem updateReviewItem(ReviewItem reviewItem) {
        ReviewItemEntity entity = toEntity(reviewItem);
        reviewItemMapper.updateById(entity);
        return findReviewItemByReviewUid(reviewItem.reviewUid()).orElse(reviewItem);
    }

    private AgentRun toModel(AgentRunEntity entity) {
        return new AgentRun(
                entity.getId(),
                entity.getRunUid(),
                entity.getSourceId(),
                entity.getSourceFileId(),
                entity.getRunType(),
                entity.getPipelineVersion(),
                entity.getStatus(),
                entity.getCurrentStep(),
                entity.getModelProvider(),
                entity.getModelName(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getFinalDecision(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AgentRunEntity toEntity(AgentRun run) {
        AgentRunEntity entity = new AgentRunEntity();
        entity.setId(run.id());
        entity.setRunUid(run.runUid());
        entity.setSourceId(run.sourceId());
        entity.setSourceFileId(run.sourceFileId());
        entity.setRunType(run.runType());
        entity.setPipelineVersion(run.pipelineVersion());
        entity.setStatus(run.status());
        entity.setCurrentStep(run.currentStep());
        entity.setModelProvider(run.modelProvider());
        entity.setModelName(run.modelName());
        entity.setStartedAt(run.startedAt());
        entity.setFinishedAt(run.finishedAt());
        entity.setFinalDecision(run.finalDecision());
        entity.setErrorMessage(run.errorMessage());
        entity.setCreatedAt(run.createdAt());
        entity.setUpdatedAt(run.updatedAt());
        return entity;
    }

    private AgentStepEntity toEntity(AgentStep step) {
        AgentStepEntity entity = new AgentStepEntity();
        entity.setId(step.id());
        entity.setStepUid(step.stepUid());
        entity.setRunId(step.runId());
        entity.setSourceId(step.sourceId());
        entity.setSourceFileId(step.sourceFileId());
        entity.setStepName(step.stepName());
        entity.setAgentName(step.agentName());
        entity.setStatus(step.status());
        entity.setInputJson(step.inputJson());
        entity.setOutputJson(step.outputJson());
        entity.setModelProvider(step.modelProvider());
        entity.setModelName(step.modelName());
        entity.setPromptVersion(step.promptVersion());
        entity.setErrorMessage(step.errorMessage());
        entity.setStartedAt(step.startedAt());
        entity.setFinishedAt(step.finishedAt());
        entity.setCreatedAt(step.createdAt());
        return entity;
    }

    private ReviewItem toModel(ReviewItemEntity entity) {
        SourceEntity source = sourceMapper.selectById(entity.getSourceId());
        SourceFileEntity sourceFile = entity.getSourceFileId() == null
                ? null
                : sourceFileMapper.selectById(entity.getSourceFileId());
        AgentRunEntity run = agentRunMapper.selectById(entity.getRunId());
        return new ReviewItem(
                entity.getId(),
                entity.getReviewUid(),
                entity.getSourceId(),
                entity.getSourceFileId(),
                entity.getRunId(),
                entity.getReviewType(),
                entity.getStatus(),
                entity.getReason(),
                entity.getSuggestedChangesJson(),
                entity.getMarkdownDraft(),
                entity.getUserDecision(),
                entity.getReviewedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                source == null ? null : source.getSourceUid(),
                sourceFile == null ? null : sourceFile.getFileUid(),
                run == null ? null : run.getRunUid()
        );
    }

    private ReviewItemEntity toEntity(ReviewItem reviewItem) {
        ReviewItemEntity entity = new ReviewItemEntity();
        entity.setId(reviewItem.id());
        entity.setReviewUid(reviewItem.reviewUid());
        entity.setSourceId(reviewItem.sourceId());
        entity.setSourceFileId(reviewItem.sourceFileId());
        entity.setRunId(reviewItem.runId());
        entity.setReviewType(reviewItem.reviewType());
        entity.setStatus(reviewItem.status());
        entity.setReason(reviewItem.reason());
        entity.setSuggestedChangesJson(reviewItem.suggestedChangesJson());
        entity.setMarkdownDraft(reviewItem.markdownDraft());
        entity.setUserDecision(reviewItem.userDecision());
        entity.setReviewedAt(reviewItem.reviewedAt());
        entity.setCreatedAt(reviewItem.createdAt());
        entity.setUpdatedAt(reviewItem.updatedAt());
        return entity;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
