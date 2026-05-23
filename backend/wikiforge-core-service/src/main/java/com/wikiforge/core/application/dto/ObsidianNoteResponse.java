package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record ObsidianNoteResponse(
        String noteUid,
        String fileUid,
        String sourceUid,
        String title,
        String vaultName,
        String vaultPath,
        String absolutePath,
        String obsidianUri,
        String contentHash,
        String status,
        OffsetDateTime createdAt
) {
}
