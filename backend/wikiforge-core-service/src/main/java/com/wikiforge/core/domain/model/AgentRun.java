package com.wikiforge.core.domain.model;

import java.time.LocalDateTime;

public record AgentRun(
        Long id,
        String runUid,
        Long sourceId,
        Long sourceFileId,
        String runType,
        String pipelineVersion,
        String status,
        String currentStep,
        String modelProvider,
        String modelName,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String finalDecision,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
