package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record LinkSourceResponse(
        String sourceUid,
        String fileUid,
        String jobUid,
        String title,
        String sourceUrl,
        String sourcePlatform,
        String status,
        OffsetDateTime createdAt
) {
}
