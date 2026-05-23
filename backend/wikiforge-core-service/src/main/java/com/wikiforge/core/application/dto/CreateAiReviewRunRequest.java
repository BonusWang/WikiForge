package com.wikiforge.core.application.dto;

public record CreateAiReviewRunRequest(
        String providerName,
        String modelName,
        String baseUrl,
        String providerType,
        String configSource
) {
}
