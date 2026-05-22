package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.CreateLocalImportJobRequest;
import com.wikiforge.core.application.dto.ImportJobPageResponse;
import com.wikiforge.core.application.dto.ImportJobResponse;
import com.wikiforge.core.application.service.ImportJobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/import-jobs")
public class ImportJobController {

    private final ImportJobService importJobService;

    public ImportJobController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @PostMapping("/local")
    public ApiResponse<ImportJobResponse> createLocalImportJob(
            @Valid @RequestBody CreateLocalImportJobRequest request
    ) {
        return ApiResponse.ok(importJobService.createLocalImportJob(request));
    }

    @GetMapping
    public ApiResponse<ImportJobPageResponse> listImportJobs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(importJobService.listImportJobs(status, page, pageSize));
    }

    @GetMapping("/{jobUid}")
    public ApiResponse<ImportJobResponse> getImportJob(@PathVariable String jobUid) {
        return ApiResponse.ok(importJobService.getImportJob(jobUid));
    }
}
