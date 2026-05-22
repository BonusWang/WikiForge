package com.wikiforge.worker.domain.model;

public record LocalScanFile(
        String fileUid,
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
