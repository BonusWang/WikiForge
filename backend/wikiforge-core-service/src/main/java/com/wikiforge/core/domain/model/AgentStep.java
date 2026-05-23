package com.wikiforge.core.domain.model;

import java.time.LocalDateTime;

public record AgentStep(
        Long id,
        String stepUid,
        Long runId,
        Long sourceId,
        Long sourceFileId,
        String stepName,
        String agentName,
        String status,
        String inputJson,
        String outputJson,
        String modelProvider,
        String modelName,
        String promptVersion,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {
}
