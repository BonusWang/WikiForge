package com.wikiforge.core.application.dto;

import java.util.List;
import java.util.Map;

public record McpToolCallPageResponse(
        List<Map<String, Object>> items,
        int page,
        int pageSize,
        long total
) {
}
