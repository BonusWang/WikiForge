package com.wikiforge.core.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WikiIngestRunResponse(
        String runUid,
        String fileUid,
        String fileName,
        String statusCode,
        String statusLabel,
        String sourcePagePath,
        List<String> wikiPagePaths,
        Boolean indexUpdated,
        Boolean logEntryAppended,
        String writeStatusCode,
        String writeStatusLabel,
        String fallbackReason,
        String failureReason,
        String managedBlockPreview,
        String logEntryPreview,
        String obsidianUri,
        Boolean retryable,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}
