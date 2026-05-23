package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record AiReviewRunResponse(
        String runUid,
        String sourceUid,
        String sourceFileUid,
        String status,
        String currentStep,
        String modelProvider,
        String modelName,
        String reviewItemUid,
        String reviewStatus,
        OffsetDateTime createdAt
) {
}
