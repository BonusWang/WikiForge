package com.wikiforge.core.application.dto;

import java.util.List;

public record PersonalRecordPageResponse(
        List<PersonalRecordResponse> items,
        int page,
        int pageSize,
        long total
) {
}
