package com.wikiforge.worker.application.dto;

public record SubmitSourceFileItem(
        String fileName,
        String fileExt,
        String originalPath,
        String managedPath,
        long fileSize,
        String mimeType,
        String contentHash,
        String parseStatus,
        String organizeStatus,
        String duplicateOfFileUid
) {
}
