package com.wikiforge.core.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalRecordResponse(
        String recordUid,
        String recordType,
        String title,
        OffsetDateTime occurredAt,
        String sourceChannel,
        String sourceRef,
        String rawContent,
        String structuredJson,
        String status,
        String sensitivityLevel,
        String createdBy,
        String obsidianVaultPath,
        String obsidianUri,
        OffsetDateTime archivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
