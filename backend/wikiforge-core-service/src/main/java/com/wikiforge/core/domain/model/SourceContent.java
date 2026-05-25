package com.wikiforge.core.domain.model;

import java.time.LocalDateTime;

public record SourceContent(
        Long id,
        String contentUid,
        Long sourceFileId,
        String parserName,
        String contentType,
        String rawText,
        String textHash,
        Integer charCount,
        Boolean rawTextSaved,
        String parseStatus,
        String parseError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
