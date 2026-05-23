package com.wikiforge.common.error;

public enum ErrorCode {
    SOURCE_INVALID_PATH("SOURCE_001", "invalid or unsafe local path"),
    SOURCE_PATH_NOT_FOUND("SOURCE_002", "input path not found"),
    SOURCE_UNSUPPORTED_INPUT_TYPE("SOURCE_003", "unsupported input type"),
    SOURCE_FILE_NOT_FOUND("SOURCE_004", "source file not found"),
    AI_REVIEW_RUN_NOT_FOUND("AI_REVIEW_001", "ai review run not found"),
    AI_REVIEW_ITEM_NOT_FOUND("AI_REVIEW_002", "ai review item not found"),
    IMPORT_JOB_NOT_FOUND("IMPORT_001", "import job not found"),
    IMPORT_INVALID_STATUS_TRANSITION("IMPORT_002", "invalid import job status transition"),
    OBSIDIAN_NOTE_NOT_FOUND("OBSIDIAN_001", "obsidian note not found"),
    OBSIDIAN_INVALID_VAULT("OBSIDIAN_002", "invalid obsidian vault"),
    WORKER_REJECTED_IMPORT_TASK("WORKER_001", "worker rejected import task"),
    VALIDATION_FAILED("COMMON_001", "validation failed");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
