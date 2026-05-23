package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.AiReviewRunResponse;
import com.wikiforge.core.application.dto.CreateAiReviewRunRequest;
import com.wikiforge.core.application.service.AiReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AiReviewRunController {

    private final AiReviewService aiReviewService;

    public AiReviewRunController(AiReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    @PostMapping("/source-files/{fileUid}/ai-review-runs")
    public ApiResponse<AiReviewRunResponse> createRun(
            @PathVariable String fileUid,
            @RequestBody(required = false) CreateAiReviewRunRequest request
    ) {
        return ApiResponse.ok(aiReviewService.createRun(fileUid, request));
    }

    @GetMapping("/ai-review-runs/{runUid}")
    public ApiResponse<AiReviewRunResponse> getRun(@PathVariable String runUid) {
        return ApiResponse.ok(aiReviewService.getRun(runUid));
    }
}
