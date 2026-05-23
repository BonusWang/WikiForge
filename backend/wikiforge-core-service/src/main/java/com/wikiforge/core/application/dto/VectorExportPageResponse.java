package com.wikiforge.core.application.dto;

import java.util.List;

public record VectorExportPageResponse(
        List<VectorExportJobResponse> items,
        int page,
        int pageSize,
        long total
) {
}
