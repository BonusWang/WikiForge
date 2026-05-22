package com.wikiforge.core.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitSourceFilesBatchRequest(
        @Valid @NotEmpty List<SubmitSourceFileItem> files
) {
}
