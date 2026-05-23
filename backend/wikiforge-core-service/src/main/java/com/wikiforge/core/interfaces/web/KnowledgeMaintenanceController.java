package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.CreateKnowledgeMaintenanceRunRequest;
import com.wikiforge.core.application.dto.KnowledgeMaintenanceItemPageResponse;
import com.wikiforge.core.application.dto.KnowledgeMaintenanceRunPageResponse;
import com.wikiforge.core.application.dto.KnowledgeMaintenanceRunResponse;
import com.wikiforge.core.application.service.KnowledgeMaintenanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class KnowledgeMaintenanceController {

    private final KnowledgeMaintenanceService knowledgeMaintenanceService;

    public KnowledgeMaintenanceController(KnowledgeMaintenanceService knowledgeMaintenanceService) {
        this.knowledgeMaintenanceService = knowledgeMaintenanceService;
    }

    @PostMapping("/maintenance-runs")
    public ApiResponse<KnowledgeMaintenanceRunResponse> createRun(
            @RequestBody(required = false) CreateKnowledgeMaintenanceRunRequest request
    ) {
        return ApiResponse.ok(knowledgeMaintenanceService.createRun(request));
    }

    @GetMapping("/maintenance-runs")
    public ApiResponse<KnowledgeMaintenanceRunPageResponse> listRuns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(knowledgeMaintenanceService.listRuns(status, page, pageSize));
    }

    @GetMapping("/maintenance-items")
    public ApiResponse<KnowledgeMaintenanceItemPageResponse> listItems(
            @RequestParam(required = false) String runUid,
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(knowledgeMaintenanceService.listItems(
                runUid,
                issueType,
                severity,
                status,
                page,
                pageSize
        ));
    }
}
