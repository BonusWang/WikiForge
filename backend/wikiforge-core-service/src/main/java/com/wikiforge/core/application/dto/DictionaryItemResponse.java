package com.wikiforge.core.application.dto;

public record DictionaryItemResponse(
        String dictType,
        String dictCode,
        String labelZh,
        String descriptionZh,
        Integer sortOrder,
        String colorToken,
        Boolean isTerminal,
        Boolean isSuccess
) {
}
