package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record KnowledgeMaintenanceItemResponse(
        String itemUid,
        String runUid,
        String issueType,
        String severity,
        String contentType,
        String sourceUid,
        String fileUid,
        String recordUid,
        String chunkUid,
        String exportUid,
        String title,
        String summary,
        String evidenceJson,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
