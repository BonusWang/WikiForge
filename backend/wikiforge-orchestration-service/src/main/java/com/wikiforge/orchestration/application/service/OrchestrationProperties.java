package com.wikiforge.orchestration.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wikiforge.orchestration")
public record OrchestrationProperties(
        String workflowEntry,
        String projectRoadmap,
        String activeBranch
) {
}
