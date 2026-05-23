package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record ReviewItemResponse(
        String reviewUid,
        String sourceUid,
        String sourceFileUid,
        String runUid,
        String reviewType,
        String status,
        String reason,
        String suggestedChanges,
        String markdownDraft,
        OffsetDateTime createdAt
) {
}
