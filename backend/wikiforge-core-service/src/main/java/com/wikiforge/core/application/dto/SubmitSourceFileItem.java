package com.wikiforge.core.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SubmitSourceFileItem(
        @NotBlank String fileName,
        String fileExt,
        @NotBlank String originalPath,
        @NotBlank String managedPath,
        @NotNull @PositiveOrZero Long fileSize,
        String mimeType,
        String contentHash,
        @NotBlank String parseStatus,
        @NotBlank String organizeStatus,
        String duplicateOfFileUid
) {
}
