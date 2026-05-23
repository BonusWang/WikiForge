package com.wikiforge.core.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLinkSourceRequest(
        @NotBlank String title,
        @NotBlank String sourceUrl,
        String sourcePlatform,
        String rawContent,
        String sourceType,
        String processingIntent
) {
}
