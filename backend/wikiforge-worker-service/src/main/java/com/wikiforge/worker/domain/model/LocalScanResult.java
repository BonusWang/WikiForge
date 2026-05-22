package com.wikiforge.worker.domain.model;

import java.util.List;

public record LocalScanResult(
        int totalCount,
        int successCount,
        int skippedCount,
        int failedCount,
        List<LocalScanFile> files
) {
}
