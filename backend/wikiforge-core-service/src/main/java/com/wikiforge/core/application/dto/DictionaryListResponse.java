package com.wikiforge.core.application.dto;

import java.util.List;

public record DictionaryListResponse(
        List<DictionaryItemResponse> items
) {
}
