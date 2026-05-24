package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.CreateWikiIngestRunRequest;
import com.wikiforge.core.application.dto.WikiIngestRunPageResponse;
import com.wikiforge.core.application.dto.WikiIngestRunResponse;
import com.wikiforge.core.application.service.WikiIngestRunService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WikiIngestRunController {

    private final WikiIngestRunService wikiIngestRunService;

    public WikiIngestRunController(WikiIngestRunService wikiIngestRunService) {
        this.wikiIngestRunService = wikiIngestRunService;
    }

    @PostMapping("/source-files/{fileUid}/wiki-ingest-runs")
    public ApiResponse<WikiIngestRunResponse> createRun(
            @PathVariable String fileUid,
            @RequestBody(required = false) CreateWikiIngestRunRequest request
    ) {
        CreateWikiIngestRunRequest safeRequest = request == null
                ? new CreateWikiIngestRunRequest(null, null, null, null)
                : request;
        return ApiResponse.ok(wikiIngestRunService.createRun(fileUid, safeRequest));
    }

    @GetMapping("/wiki-ingest-runs")
    public ApiResponse<WikiIngestRunPageResponse> listRuns(
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String fileUid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(wikiIngestRunService.listRuns(statusCode, fileUid, page, pageSize));
    }

    @GetMapping("/wiki-ingest-runs/{runUid}")
    public ApiResponse<WikiIngestRunResponse> getRun(@PathVariable String runUid) {
        return ApiResponse.ok(wikiIngestRunService.getRun(runUid));
    }
}
