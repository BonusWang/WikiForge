package com.wikiforge.core.application.dto;

public record CreateKnowledgeMaintenanceRunRequest(
        Integer staleDays,
        Integer limit
) {
}
