package com.wikiforge.core.application.dto;

import java.util.List;

public record WikiIngestRunPageResponse(
        List<WikiIngestRunResponse> items,
        int page,
        int pageSize,
        long total
) {
}
