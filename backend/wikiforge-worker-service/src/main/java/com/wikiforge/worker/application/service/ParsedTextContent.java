package com.wikiforge.worker.application.service;

public record ParsedTextContent(
        String parserName,
        String contentType,
        String parsedText,
        String textHash,
        Integer charCount,
        Boolean rawTextSaved,
        String parseStatus,
        String parseError
) {
}
