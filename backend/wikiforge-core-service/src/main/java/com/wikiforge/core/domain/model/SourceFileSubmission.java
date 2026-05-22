package com.wikiforge.core.domain.model;

public record SourceFileSubmission(
        String fileName,
        String fileExt,
        String originalPath,
        String managedPath,
        Long fileSize,
        String mimeType,
        String contentHash,
        String parseStatus,
        String organizeStatus,
        String duplicateOfFileUid
) {
}
