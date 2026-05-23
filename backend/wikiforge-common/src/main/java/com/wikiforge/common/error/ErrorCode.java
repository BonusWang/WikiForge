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
    MCP_TOOL_NOT_FOUND("MCP_001", "mcp tool not found"),
    MCP_TOOL_DISABLED("MCP_002", "mcp tool disabled"),
    MCP_INVALID_INPUT("MCP_003", "invalid mcp input"),
    MCP_SOURCE_NOT_FOUND("MCP_004", "mcp source not found"),
    MCP_OBSIDIAN_NOTE_NOT_FOUND("MCP_005", "mcp obsidian note not found"),
    MCP_FORBIDDEN_PATH_EXPOSURE("MCP_006", "mcp path exposure is forbidden"),
    MCP_CALL_FAILED("MCP_007", "mcp call failed"),
    PERSONAL_RECORD_INVALID_TYPE("RECORD_001", "invalid personal record type"),
    PERSONAL_RECORD_NOT_FOUND("RECORD_002", "personal record not found"),
    PERSONAL_RECORD_INVALID_INPUT("RECORD_003", "invalid personal record input"),
    LINK_SOURCE_INVALID_INPUT("LINK_SOURCE_001", "invalid link source input"),
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
