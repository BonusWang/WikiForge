package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record ApproveReviewItemResponse(
        String reviewUid,
        String status,
        String userDecision,
        OffsetDateTime reviewedAt,
        ObsidianNoteResponse obsidianNote
) {
}
