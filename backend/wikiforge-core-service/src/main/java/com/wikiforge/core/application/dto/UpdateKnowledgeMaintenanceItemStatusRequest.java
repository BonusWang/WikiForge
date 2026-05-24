package com.wikiforge.core.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateKnowledgeMaintenanceItemStatusRequest(
        @NotBlank String status,
        String resolutionNote,
        String resolvedBy
) {
}
