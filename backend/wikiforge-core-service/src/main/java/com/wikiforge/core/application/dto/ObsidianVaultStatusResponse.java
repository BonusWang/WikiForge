package com.wikiforge.core.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ObsidianVaultStatusResponse(
        String vaultName,
        String vaultPath,
        Boolean exists,
        Boolean writable,
        Boolean sourceNoteDirectoryExists,
        String lastNoteUid,
        OffsetDateTime lastWrittenAt,
        String errorMessage
) {
}
