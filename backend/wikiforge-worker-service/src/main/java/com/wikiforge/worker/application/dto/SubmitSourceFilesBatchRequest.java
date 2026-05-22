package com.wikiforge.worker.application.dto;

import java.util.List;

public record SubmitSourceFilesBatchRequest(
        List<SubmitSourceFileItem> files
) {
}
