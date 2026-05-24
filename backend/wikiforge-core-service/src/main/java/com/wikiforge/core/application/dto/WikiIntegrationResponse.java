package com.wikiforge.core.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WikiIntegrationResponse(
        String integrationUid,
        String pageUid,
        String pageTitle,
        String pageType,
        String vaultPath,
        String sourceUid,
        String sourceFileUid,
        String runUid,
        String status,
        String riskLevel,
        BigDecimal confidenceScore,
        String changeSummary,
        String proposedMarkdown,
        OffsetDateTime appliedAt,
        OffsetDateTime createdAt
) {
}
