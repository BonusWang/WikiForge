package com.wikiforge.core.application.dto;

public record VersionInfoResponse(
        String product,
        String service,
        String version,
        String stage,
        String releaseDate,
        String apiBasePath
) {
}
