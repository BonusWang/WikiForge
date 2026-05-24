package com.wikiforge.core.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateLocalImportJobRequest(
        @NotBlank String inputPath,
        String rawSourcesRoot,
        Boolean recursive,
        String organizeMode,
        @Positive Integer maxCopyFileSizeMb
) {
}
