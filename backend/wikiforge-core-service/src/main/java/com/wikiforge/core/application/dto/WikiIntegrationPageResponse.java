package com.wikiforge.core.application.dto;

import java.util.List;

public record WikiIntegrationPageResponse(
        List<WikiIntegrationResponse> items,
        int page,
        int pageSize,
        long total
) {
}
