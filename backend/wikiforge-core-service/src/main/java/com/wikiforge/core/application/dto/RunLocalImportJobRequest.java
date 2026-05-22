package com.wikiforge.core.application.dto;

public record RunLocalImportJobRequest(
        String jobUid,
        String inputPath,
        String rawSourcesRoot,
        boolean recursive,
        String organizeMode,
        int maxCopyFileSizeMb,
        boolean skipHidden,
        boolean skipTemporary,
        boolean followSymlinks
) {
}
