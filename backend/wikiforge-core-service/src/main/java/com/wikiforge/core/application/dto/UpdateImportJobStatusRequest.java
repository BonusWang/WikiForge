package com.wikiforge.core.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateImportJobStatusRequest(
        @NotBlank String status,
        @PositiveOrZero Integer totalCount,
        @PositiveOrZero Integer successCount,
        @PositiveOrZero Integer skippedCount,
        @PositiveOrZero Integer failedCount,
        String errorMessage
) {
}
