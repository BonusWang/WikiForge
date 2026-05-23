package com.wikiforge.core.domain.model;

public record SourceFileSubmission(
        String fileName,
        String fileExt,
        String originalPath,
        String managedPath,
        Long fileSize,
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
