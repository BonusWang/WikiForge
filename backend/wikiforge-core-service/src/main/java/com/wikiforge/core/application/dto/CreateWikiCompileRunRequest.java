package com.wikiforge.core.application.dto;

import java.math.BigDecimal;

public record CreateWikiCompileRunRequest(
        String targetPageUid,
        String riskLevel,
        BigDecimal confidenceScore,
        String changeSummary,
        String proposedMarkdown
) {
}
