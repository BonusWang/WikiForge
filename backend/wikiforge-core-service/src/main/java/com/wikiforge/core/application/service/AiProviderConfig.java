package com.wikiforge.core.application.service;

public record AiProviderConfig(
        String providerName,
        String providerType,
        String baseUrl,
        String apiKey,
        String modelName
) {
    public boolean ruleBased() {
        return "rule-based".equalsIgnoreCase(providerName())
                || "rule_based".equalsIgnoreCase(providerType())
                || "rule-based".equalsIgnoreCase(providerType());
    }

    public boolean openAiCompatible() {
        return "openai_compatible".equalsIgnoreCase(providerType())
                || "openai-compatible".equalsIgnoreCase(providerType());
    }

    public boolean readyForRemoteCall() {
        return hasText(apiKey()) && hasText(baseUrl()) && hasText(modelName());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
