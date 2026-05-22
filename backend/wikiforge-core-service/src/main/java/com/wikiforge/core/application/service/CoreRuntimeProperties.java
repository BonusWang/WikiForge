package com.wikiforge.core.application.service;

import java.util.Arrays;
import java.util.List;
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
}
