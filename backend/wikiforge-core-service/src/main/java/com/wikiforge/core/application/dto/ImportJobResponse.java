package com.wikiforge.core.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImportJobResponse(
        String jobUid,
        String importType,
        String inputPath,
        String rawSourcesRoot,
        Boolean recursive,
        String organizeMode,
        Integer maxCopyFileSizeMb,
        String status,
        String statusCode,
        String statusLabel,
        String statusDescription,
        String statusColor,
        Boolean isTerminal,
        Integer totalCount,
        Integer successCount,
        Integer skippedCount,
        Integer failedCount,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorMessage
) {
}
