package com.wikiforge.core.domain.model;

import java.time.LocalDateTime;

public record ObsidianNote(
        Long id,
        String noteUid,
        Long sourceId,
        Long sourceFileId,
        String noteType,
        String vaultName,
        String vaultPath,
        String absolutePath,
        String obsidianUri,
        String title,
        String frontmatterJson,
        String contentHash,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
