package com.wikiforge.core.domain.model;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;

public enum OrganizeMode {
    COPY("copy"),
    INDEX_ONLY("index_only");

    private final String value;

    OrganizeMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OrganizeMode fromValue(String value) {
        for (OrganizeMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "invalid organizeMode");
    }
}
