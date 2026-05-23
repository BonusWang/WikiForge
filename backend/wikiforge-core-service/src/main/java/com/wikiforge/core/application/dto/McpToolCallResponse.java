package com.wikiforge.core.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpToolCallResponse(
        String callUid,
        String toolName,
        String status,
        Object result,
        Map<String, Object> error,
        long durationMs,
        OffsetDateTime createdAt
) {
}
