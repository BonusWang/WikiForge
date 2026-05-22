package com.wikiforge.worker.application.dto;

public record RunLocalImportJobResponse(
        String jobUid,
        boolean accepted,
        String workerStatus
) {
}
