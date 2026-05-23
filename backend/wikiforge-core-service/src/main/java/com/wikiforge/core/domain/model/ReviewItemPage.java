package com.wikiforge.core.domain.model;

import java.util.List;

public record ReviewItemPage(
        List<ReviewItem> items,
        int page,
        int pageSize,
        long total
) {
}
