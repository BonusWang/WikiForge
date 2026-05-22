package com.wikiforge.core.domain.model;

public enum ImportType {
    PATH_SCAN("path_scan"),
    URL("url"),
    UPLOAD("upload");

    private final String value;

    ImportType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
