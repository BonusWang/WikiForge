package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.CreateWikiCompileRunRequest;
import com.wikiforge.core.application.dto.CreateWikiPageRequest;
import com.wikiforge.core.application.dto.WikiCompileRunResponse;
import com.wikiforge.core.application.dto.WikiIntegrationDecisionRequest;
import com.wikiforge.core.application.dto.WikiIntegrationPageResponse;
import com.wikiforge.core.application.dto.WikiIntegrationResponse;
import com.wikiforge.core.application.dto.WikiPagePageResponse;
import com.wikiforge.core.application.dto.WikiPageResponse;
import com.wikiforge.core.application.service.WikiCompileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WikiCompileController {

    private final WikiCompileService wikiCompileService;

    public WikiCompileController(WikiCompileService wikiCompileService) {
        this.wikiCompileService = wikiCompileService;
    }

    @PostMapping("/wiki-pages")
    public ApiResponse<WikiPageResponse> createPage(@RequestBody CreateWikiPageRequest request) {
        return ApiResponse.ok(wikiCompileService.createPage(request));
    }

    @GetMapping("/wiki-pages")
    public ApiResponse<WikiPagePageResponse> listPages(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(wikiCompileService.listPages(type, status, page, pageSize));
    }

    @PostMapping("/source-files/{fileUid}/wiki-compile-runs")
    public ApiResponse<WikiCompileRunResponse> createCompileRun(
            @PathVariable String fileUid,
            @RequestBody(required = false) CreateWikiCompileRunRequest request
    ) {
        return ApiResponse.ok(wikiCompileService.createCompileRun(fileUid, request));
    }

    @GetMapping("/wiki-integrations")
    public ApiResponse<WikiIntegrationPageResponse> listIntegrations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pageUid,
            @RequestParam(required = false) String sourceUid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(wikiCompileService.listIntegrations(status, pageUid, sourceUid, page, pageSize));
    }

    @PostMapping("/wiki-integrations/{integrationUid}/approve")
    public ApiResponse<WikiIntegrationResponse> approveIntegration(
            @PathVariable String integrationUid,
            @RequestBody(required = false) WikiIntegrationDecisionRequest request
    ) {
        return ApiResponse.ok(wikiCompileService.approveIntegration(integrationUid, request));
    }

    @PostMapping("/wiki-integrations/{integrationUid}/reject")
    public ApiResponse<WikiIntegrationResponse> rejectIntegration(
            @PathVariable String integrationUid,
            @RequestBody(required = false) WikiIntegrationDecisionRequest request
    ) {
        return ApiResponse.ok(wikiCompileService.rejectIntegration(integrationUid, request));
    }
}
