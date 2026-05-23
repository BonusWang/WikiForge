package com.wikiforge.core.application.dto;

public record SourceNoteDraftResponse(
        String fileUid,
        String sourceUid,
        String title,
        String vaultName,
        String vaultPath,
        String markdown
) {
}
