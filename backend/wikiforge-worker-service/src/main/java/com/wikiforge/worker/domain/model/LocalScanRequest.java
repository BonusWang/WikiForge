package com.wikiforge.worker.domain.model;

import java.nio.file.Path;

public record LocalScanRequest(
        Path inputPath,
        Path rawSourcesRoot,
        boolean recursive,
        boolean skipHidden,
        boolean skipTemporary,
        boolean followSymlinks,
        long maxCopyFileSizeMb,
        String organizeMode
) {
    public LocalScanRequest {
        if (organizeMode == null || organizeMode.isBlank()) {
            organizeMode = "copy";
        }
    }
}
