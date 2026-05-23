package com.wikiforge.core.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CreatePersonalRecordRequest(
        @NotBlank String recordType,
        @NotBlank String title,
        String occurredAt,
        @NotBlank String rawContent,
        String sourceChannel,
        String sourceRef,
        Map<String, Object> structured,
        String sensitivityLevel,
        String createdBy
) {
}
