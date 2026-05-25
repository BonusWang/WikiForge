package com.wikiforge.core.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SourceFileResponse(
        String fileUid,
        String sourceUid,
        String jobUid,
        String fileName,
        String fileExt,
        String originalPath,
        String originalPathMasked,
        String managedPath,
        String rawSourceRelativePath,
        Long fileSize,
        Long fileSizeBytes,
        String mimeType,
        String contentHash,
        String parseStatus,
        String collectStatusCode,
        String collectStatusLabel,
        String extractStatusCode,
        String extractStatusLabel,
        String wikiStatusCode,
        String wikiStatusLabel,
        String organizeStatus,
        String duplicateOfFileUid,
        String extractFailureReason,
        String wikiFailureReason,
        WikiIngestRunResponse latestWikiIngestRun,
        OffsetDateTime createdAt
) {
}
