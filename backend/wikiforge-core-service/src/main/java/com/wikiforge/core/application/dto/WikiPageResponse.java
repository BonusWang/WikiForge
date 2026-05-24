package com.wikiforge.core.application.dto;

import java.time.OffsetDateTime;

public record WikiPageResponse(
        String pageUid,
        String pageType,
        String title,
        String slug,
        String vaultPath,
        String status,
        OffsetDateTime createdAt
) {
}
