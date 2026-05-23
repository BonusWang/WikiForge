package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.ReviewItemPageResponse;
import com.wikiforge.core.application.service.AiReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/review-items")
public class ReviewItemController {

    private final AiReviewService aiReviewService;

    public ReviewItemController(AiReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    @GetMapping
    public ApiResponse<ReviewItemPageResponse> listReviewItems(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(aiReviewService.listReviewItems(status, page, pageSize));
    }
}
