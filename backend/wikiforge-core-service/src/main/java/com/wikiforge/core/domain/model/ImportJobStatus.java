package com.wikiforge.core.domain.model;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;

public enum ImportJobStatus {
    PENDING("pending"),
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String value;

    ImportJobStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ImportJobStatus fromValue(String value) {
        for (ImportJobStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "invalid import job status");
    }

    public boolean canTransitionTo(ImportJobStatus next) {
        return switch (this) {
            case PENDING -> next == RUNNING || next == FAILED;
            case RUNNING -> next == COMPLETED || next == FAILED || next == CANCELLED;
            case COMPLETED, FAILED, CANCELLED -> false;
        };
    }
}
