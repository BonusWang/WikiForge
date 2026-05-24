package com.wikiforge.core.application.dto;

public record CreateWikiIngestRunRequest(
        String writeMode,
        String targetTopic,
        String targetProject,
        Boolean forceRewriteManagedBlock
) {
}
