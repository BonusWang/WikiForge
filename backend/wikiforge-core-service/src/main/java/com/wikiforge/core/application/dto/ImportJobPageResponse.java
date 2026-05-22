package com.wikiforge.core.application.dto;

import java.util.List;

public record ImportJobPageResponse(
        List<ImportJobResponse> items,
        int page,
        int pageSize,
        long total
) {
}
