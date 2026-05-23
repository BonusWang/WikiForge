package com.wikiforge.core.application.dto;

import java.util.List;

public record KnowledgeMaintenanceRunPageResponse(
        List<KnowledgeMaintenanceRunResponse> items,
        int page,
        int pageSize,
        long total
) {
}
