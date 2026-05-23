package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record KnowledgeMaintenanceRunResponse(
        String runUid,
        String runType,
        String status,
        Integer staleDays,
        Integer totalCount,
        Integer issueCount,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime createdAt,
        String errorMessage
) {
}
