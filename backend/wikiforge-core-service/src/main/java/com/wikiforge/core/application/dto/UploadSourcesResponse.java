package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record UploadSourcesResponse(
        String jobUid,
        String importType,
        String statusCode,
        String statusLabel,
        String statusDescription,
        int uploadedCount,
        OffsetDateTime createdAt
) {
}
