package com.wikiforge.worker.application.dto;

public record UpdateImportJobStatusRequest(
        String status,
        Integer totalCount,
        Integer successCount,
        Integer skippedCount,
        Integer failedCount,
        String errorMessage
) {
}
