package com.wikiforge.core.application.dto;

import java.util.List;

public record ReviewItemPageResponse(
        List<ReviewItemResponse> items,
        int page,
        int pageSize,
        long total
) {
}
