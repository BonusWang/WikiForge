package com.wikiforge.core.application.dto;

import java.util.List;

public record ObsidianInitResponse(
        String vaultName,
        String managedRoot,
        List<String> createdPaths
) {
}
