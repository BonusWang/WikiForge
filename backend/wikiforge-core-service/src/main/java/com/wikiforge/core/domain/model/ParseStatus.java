package com.wikiforge.core.domain.model;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;

public enum ParseStatus {
    PENDING("pending"),
    SUCCESS("success"),
    FAILED("failed"),
    PARTIAL("partial");

    private final String value;

    ParseStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ParseStatus fromValue(String value) {
        for (ParseStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "invalid parseStatus");
    }
}
