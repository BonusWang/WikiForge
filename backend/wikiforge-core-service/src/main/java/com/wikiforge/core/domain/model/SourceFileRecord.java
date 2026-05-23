package com.wikiforge.core.domain.model;

import java.time.LocalDateTime;

public record SourceFileRecord(
        Long sourceFileId,
        Long sourceId,
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
        LocalDateTime createdAt
) {
}
