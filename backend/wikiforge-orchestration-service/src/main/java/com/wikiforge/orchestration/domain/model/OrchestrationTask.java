package com.wikiforge.orchestration.domain.model;

import java.util.List;

public record OrchestrationTask(
        String taskId,
        String parentTask,
        String title,
        String status,
        String owner,
        String goal,
        String scope,
        List<String> allowedFiles,
        List<String> forbiddenFiles,
        List<String> contracts,
        List<String> verificationCommands,
        String handoff,
        String nextStep,
        List<String> tags
) {
}
