package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record PersonalRecordObsidianNoteResponse(
        String recordUid,
        String title,
        String vaultName,
        String vaultPath,
        String obsidianUri,
        String status,
        OffsetDateTime archivedAt
) {
}
