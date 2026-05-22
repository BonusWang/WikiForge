package com.wikiforge.worker.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RunLocalImportJobRequest(
        @NotBlank String jobUid,
        @NotBlank String inputPath,
        String rawSourcesRoot,
        boolean recursive,
        String organizeMode,
        @Min(1) long maxCopyFileSizeMb,
        boolean skipHidden,
        boolean skipTemporary,
        boolean followSymlinks
) {
}
