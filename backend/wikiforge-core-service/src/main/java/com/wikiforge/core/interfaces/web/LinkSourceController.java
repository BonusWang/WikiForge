package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.CreateLinkSourceRequest;
import com.wikiforge.core.application.dto.LinkSourceResponse;
import com.wikiforge.core.application.service.LinkSourceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/link-sources")
public class LinkSourceController {

    private final LinkSourceService linkSourceService;

    public LinkSourceController(LinkSourceService linkSourceService) {
        this.linkSourceService = linkSourceService;
    }

    @PostMapping
    public ApiResponse<LinkSourceResponse> createLinkSource(
            @Valid @RequestBody CreateLinkSourceRequest request
    ) {
        return ApiResponse.ok(linkSourceService.createLinkSource(request));
    }
}
