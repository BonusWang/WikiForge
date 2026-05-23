package com.wikiforge.core.application.dto;

import java.util.Map;

public record McpToolDefinition(
        String name,
        String description,
        boolean enabled,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema
) {
}
