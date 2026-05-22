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
        String managedPath,
        Long fileSize,
        String mimeType,
        String contentHash,
        String parseStatus,
        String organizeStatus,
        String duplicateOfFileUid,
        OffsetDateTime createdAt
) {
}
