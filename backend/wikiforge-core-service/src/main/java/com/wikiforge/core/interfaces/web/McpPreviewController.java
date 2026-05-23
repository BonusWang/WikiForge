package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.McpToolCallPageResponse;
import com.wikiforge.core.application.dto.McpToolCallRequest;
import com.wikiforge.core.application.dto.McpToolCallResponse;
import com.wikiforge.core.application.dto.McpToolListResponse;
import com.wikiforge.core.application.service.McpPreviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mcp")
public class McpPreviewController {

    private final McpPreviewService mcpPreviewService;

    public McpPreviewController(McpPreviewService mcpPreviewService) {
        this.mcpPreviewService = mcpPreviewService;
    }

    @GetMapping("/tools")
    public ApiResponse<McpToolListResponse> listTools() {
        return ApiResponse.ok(mcpPreviewService.listTools());
    }

    @PostMapping("/tools/{toolName}/call")
    public ApiResponse<McpToolCallResponse> callTool(
            @PathVariable String toolName,
            @RequestBody(required = false) McpToolCallRequest request,
            @RequestHeader(value = "X-WikiForge-Caller-Type", required = false) String callerType,
            @RequestHeader(value = "X-WikiForge-Caller-Id", required = false) String callerId
    ) {
        return ApiResponse.ok(mcpPreviewService.callTool(
                toolName,
                request == null ? null : request.arguments(),
                callerType,
                callerId
        ));
    }

    @GetMapping("/calls")
    public ApiResponse<McpToolCallPageResponse> listCalls(
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String callerType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(mcpPreviewService.listCalls(toolName, status, callerType, page, pageSize));
    }
}
