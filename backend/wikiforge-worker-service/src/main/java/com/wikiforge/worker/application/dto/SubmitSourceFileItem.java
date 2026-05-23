package com.wikiforge.worker.application.dto;

public record SubmitSourceFileItem(
        String fileName,
        String fileExt,
        String originalPath,
        String managedPath,
        long fileSize,
        String mimeType,
        String contentHash,
        String parserName,
        String parseStatus,
        String organizeStatus,
        String duplicateOfFileUid,
        String contentType,
        String parsedText,
        String textHash,
        Integer charCount,
        Boolean rawTextSaved,
        String parseError
) {
}
