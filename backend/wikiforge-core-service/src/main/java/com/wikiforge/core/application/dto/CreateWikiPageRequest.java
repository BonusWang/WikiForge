package com.wikiforge.core.application.dto;

public record CreateWikiPageRequest(
        String pageType,
        String title,
        String slug,
        String vaultPath,
        String status
) {
}
