package com.wikiforge.orchestration.application.dto;

public record TaskStatsResponse(
        int total,
        int ready,
        int doing,
        int review,
        int blocked,
        int done
) {
}
