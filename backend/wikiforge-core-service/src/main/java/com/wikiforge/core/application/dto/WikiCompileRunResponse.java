package com.wikiforge.core.application.dto;

public record WikiCompileRunResponse(
        String runUid,
        String integrationUid,
        String status,
        String finalDecision
) {
}
