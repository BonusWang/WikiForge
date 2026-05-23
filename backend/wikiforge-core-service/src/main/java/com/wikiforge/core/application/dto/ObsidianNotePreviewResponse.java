package com.wikiforge.core.application.dto;

public record ObsidianNotePreviewResponse(
        String noteUid,
        String title,
        String vaultName,
        String vaultPath,
        String obsidianUri,
        String markdown
) {
}
