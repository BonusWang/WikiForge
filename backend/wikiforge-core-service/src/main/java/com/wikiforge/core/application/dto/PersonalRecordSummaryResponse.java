package com.wikiforge.core.application.dto;

import java.util.List;
import java.util.Map;

public record PersonalRecordSummaryResponse(
        String period,
        long total,
        Map<String, Long> byType,
        Map<String, Long> byStatus,
        List<PersonalRecordResponse> recentItems
) {
}
