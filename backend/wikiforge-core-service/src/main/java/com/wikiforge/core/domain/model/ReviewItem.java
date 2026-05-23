package com.wikiforge.core.domain.model;

import java.time.LocalDateTime;

public record ReviewItem(
        Long id,
        String reviewUid,
        Long sourceId,
        Long sourceFileId,
        Long runId,
        String reviewType,
        String status,
        String reason,
        String suggestedChangesJson,
        String markdownDraft,
        String userDecision,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String sourceUid,
        String sourceFileUid,
        String runUid
) {
}
