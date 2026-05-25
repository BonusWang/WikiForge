package com.wikiforge.core.domain.model;

import java.time.LocalDateTime;

public record SourceFileRecord(
        Long sourceFileId,
        String fileUid,
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
        String parseError,
        String duplicateOfFileUid,
        LocalDateTime createdAt
) {
}
