package com.wikiforge.core.domain.model;

public enum SourceStatus {
    PENDING("pending"),
    ORGANIZED("organized"),
    FAILED("failed");

    private final String value;

    SourceStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
