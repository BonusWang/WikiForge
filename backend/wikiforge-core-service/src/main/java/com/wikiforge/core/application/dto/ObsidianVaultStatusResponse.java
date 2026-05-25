package com.wikiforge.core.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ObsidianVaultStatusResponse(
        String vaultName,
        String vaultPathMasked,
        String managedRoot,
        Boolean exists,
        Boolean writable,
        Boolean managedRootExists,
        OffsetDateTime lastWriteAt,
        String failureReason
) {
}
