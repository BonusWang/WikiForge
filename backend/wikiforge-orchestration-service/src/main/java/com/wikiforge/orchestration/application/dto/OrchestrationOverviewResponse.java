package com.wikiforge.orchestration.application.dto;

import java.util.List;

public record OrchestrationOverviewResponse(
        String mode,
        String workflowEntry,
        String projectRoadmap,
        String activeBranch,
        String currentStage,
        String source,
        TaskStatsResponse stats,
        List<String> nextActions
) {
}
