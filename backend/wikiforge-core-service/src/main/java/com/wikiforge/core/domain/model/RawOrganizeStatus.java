package com.wikiforge.core.domain.model;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;

public enum RawOrganizeStatus {
    PENDING("pending"),
    COPIED("copied"),
    DUPLICATE("duplicate"),
    NEED_CONFIRM("need_confirm"),
    FAILED("failed");

    private final String value;

    RawOrganizeStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static RawOrganizeStatus fromValue(String value) {
        for (RawOrganizeStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "invalid organizeStatus");
    }
}
