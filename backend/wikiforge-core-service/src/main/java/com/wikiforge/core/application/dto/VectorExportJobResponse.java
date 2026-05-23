package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record VectorExportJobResponse(
        String exportUid,
        String scope,
        String targetCollection,
        String exportFormat,
        String status,
        Integer totalCount,
        String exportFileName,
        String exportRelativePath,
        OffsetDateTime createdAt,
        OffsetDateTime finishedAt,
        String errorMessage
) {
}
