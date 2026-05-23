package com.wikiforge.core.application.dto;

public record CreateVectorExportRequest(
        String scope,
        String targetCollection,
        Integer maxChunkChars,
        Integer limit
) {
}
