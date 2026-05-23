package com.wikiforge.core.application.service;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CoreRuntimeProperties {

    private final Environment environment;

    public CoreRuntimeProperties(Environment environment) {
        this.environment = environment;
    }

    public String rawSourcesRoot() {
        return firstConfigured(
                "wikiforge.storage.raw-sources-root",
                "WIKIFORGE_RAW_SOURCES_ROOT",
                "wikiforge.raw-sources-root",
                "wikiforge.raw-sources-path",
                "WIKIFORGE_RAW_SOURCES_PATH"
        );
    }

    public String workerBaseUrl() {
        String value = firstConfigured("wikiforge.worker.base-url", "WIKIFORGE_WORKER_BASE_URL");
        return value == null ? "http://wikiforge-worker-service:8081" : value;
    }

    public String internalApiToken() {
        return firstConfigured("wikiforge.security.internal-api-token", "WIKIFORGE_INTERNAL_API_TOKEN");
    }

    public String modelProvider() {
        return firstConfigured("wikiforge.model.provider", "WIKIFORGE_MODEL_PROVIDER");
    }

    public AiProviderConfig aiProviderConfig(
            String providerName,
            String providerType,
            String baseUrl,
            String modelName
    ) {
        String selectedProvider = firstText(providerName, modelProvider(), "rule-based");
        String selectedType = firstText(
                providerType,
                providerConfiguredValue(selectedProvider, "type", "TYPE"),
                defaultProviderType(selectedProvider)
        );
        String selectedBaseUrl = firstText(
                baseUrl,
                providerConfiguredValue(selectedProvider, "base-url", "BASE_URL"),
                legacyProviderValue(selectedProvider, "base-url", "BASE_URL"),
                defaultProviderBaseUrl(selectedProvider)
        );
        String selectedModel = firstText(
                modelName,
                providerConfiguredValue(selectedProvider, "model", "MODEL"),
                legacyProviderValue(selectedProvider, "model", "MODEL"),
                defaultProviderModel(selectedProvider)
        );
        String selectedApiKey = firstText(
                providerConfiguredValue(selectedProvider, "api-key", "API_KEY"),
                legacyProviderValue(selectedProvider, "api-key", "API_KEY"),
                firstConfigured("wikiforge.model.api-key", "WIKIFORGE_MODEL_API_KEY")
        );
        return new AiProviderConfig(
                selectedProvider,
                selectedType,
                selectedBaseUrl,
                selectedApiKey,
                selectedModel
        );
    }

    public String minimaxApiKey() {
        return aiProviderConfig("minimax", null, null, null).apiKey();
    }

    public String minimaxBaseUrl() {
        return aiProviderConfig("minimax", null, null, null).baseUrl();
    }

    public String minimaxModel() {
        return aiProviderConfig("minimax", null, null, null).modelName();
    }

    public String obsidianVaultPath() {
        return firstConfigured("wikiforge.obsidian-vault-path", "WIKIFORGE_OBSIDIAN_VAULT_PATH");
    }

    public String vectorExportRoot() {
        String configured = firstConfigured("wikiforge.vector-export-root", "WIKIFORGE_VECTOR_EXPORT_ROOT");
        return configured == null ? "./data/vector-exports" : configured;
    }

    public String obsidianVaultName() {
        String configured = firstConfigured("wikiforge.obsidian-vault-name", "WIKIFORGE_OBSIDIAN_VAULT_NAME");
        if (configured != null) {
            return configured;
        }
        String vaultPath = obsidianVaultPath();
        if (vaultPath == null || vaultPath.isBlank()) {
            return "WikiForgeVault";
        }
        Path fileName = Path.of(vaultPath).normalize().getFileName();
        return fileName == null ? "WikiForgeVault" : fileName.toString();
    }

    public List<String> allowedScanRoots() {
        String value = firstConfigured(
                "wikiforge.security.allowed-scan-roots",
                "WIKIFORGE_ALLOWED_SCAN_ROOTS",
                "wikiforge.allowed-scan-roots"
        );
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(root -> !root.isBlank())
                .toList();
    }

    private String firstConfigured(String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String providerConfiguredValue(String providerName, String propertySuffix, String envSuffix) {
        String propertyProviderName = propertyProviderName(providerName);
        String envProviderName = envProviderName(providerName);
        return firstConfigured(
                "wikiforge.model.providers." + propertyProviderName + "." + propertySuffix,
                "WIKIFORGE_MODEL_" + envProviderName + "_" + envSuffix
        );
    }

    private String legacyProviderValue(String providerName, String propertySuffix, String envSuffix) {
        String propertyProviderName = propertyProviderName(providerName);
        String envProviderName = envProviderName(providerName);
        return firstConfigured(
                "wikiforge.model." + propertyProviderName + "." + propertySuffix,
                "WIKIFORGE_" + envProviderName + "_" + envSuffix
        );
    }

    private String defaultProviderType(String providerName) {
        if ("rule-based".equalsIgnoreCase(providerName)) {
            return "rule_based";
        }
        return "openai_compatible";
    }

    private String defaultProviderBaseUrl(String providerName) {
        String normalized = propertyProviderName(providerName);
        if ("minimax".equals(normalized) || "minmax".equals(normalized)) {
            return "https://api.minimax.io/v1";
        }
        return null;
    }

    private String defaultProviderModel(String providerName) {
        if ("rule-based".equalsIgnoreCase(providerName)) {
            return "wikiforge-local-rules";
        }
        return null;
    }

    private String propertyProviderName(String providerName) {
        return normalizeProviderName(providerName, "-").toLowerCase(Locale.ROOT);
    }

    private String envProviderName(String providerName) {
        return normalizeProviderName(providerName, "_").toUpperCase(Locale.ROOT);
    }

    private String normalizeProviderName(String providerName, String replacement) {
        if (providerName == null || providerName.isBlank()) {
            return "default";
        }
        return providerName.trim().replaceAll("[^A-Za-z0-9]+", replacement);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
