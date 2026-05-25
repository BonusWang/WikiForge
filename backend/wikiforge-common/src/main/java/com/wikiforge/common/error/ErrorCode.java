package com.wikiforge.common.error;

public enum ErrorCode {
    SOURCE_INVALID_PATH("SOURCE_001", "invalid or unsafe local path"),
    SOURCE_PATH_NOT_FOUND("SOURCE_002", "input path not found"),
    SOURCE_UNSUPPORTED_INPUT_TYPE("SOURCE_003", "unsupported input type"),
    SOURCE_FILE_NOT_FOUND("SOURCE_004", "source file not found"),
    IMPORT_JOB_NOT_FOUND("IMPORT_001", "import job not found"),
    IMPORT_INVALID_STATUS_TRANSITION("IMPORT_002", "invalid import job status transition"),
    OBSIDIAN_INVALID_VAULT("OBSIDIAN_002", "invalid obsidian vault"),
    WIKI_INVALID_INPUT("WIKI_001", "invalid wiki input"),
    WIKI_INGEST_RUN_NOT_FOUND("WIKI_004", "wiki ingest run not found"),
    UPLOAD_EMPTY_INPUT("UPLOAD_001", "upload files are required"),
    UPLOAD_WRITE_FAILED("UPLOAD_002", "upload file cannot be written"),
    DICTIONARY_INVALID_TYPE("DICT_001", "invalid dictionary type"),
    ORCHESTRATION_TASK_NOT_FOUND("ORCHESTRATION_001", "orchestration task not found"),
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
