package com.wikiforge.core.application.dto;

import java.util.List;

public record KnowledgeMaintenanceItemPageResponse(
        List<KnowledgeMaintenanceItemResponse> items,
        int page,
        int pageSize,
        long total
) {
}
