package com.wikiforge.core.application.dto;

import java.util.List;

public record WikiPagePageResponse(
        List<WikiPageResponse> items,
        int page,
        int pageSize,
        long total
) {
}
