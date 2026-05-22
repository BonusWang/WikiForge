package com.wikiforge.worker.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.worker.application.dto.RunLocalImportJobRequest;
import com.wikiforge.worker.application.dto.RunLocalImportJobResponse;
import com.wikiforge.worker.application.service.LocalImportJobRunner;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/worker/import-jobs/local")
public class WorkerImportJobController {

    private final LocalImportJobRunner localImportJobRunner;

    public WorkerImportJobController(LocalImportJobRunner localImportJobRunner) {
        this.localImportJobRunner = localImportJobRunner;
    }

    @PostMapping("/run")
    public ApiResponse<RunLocalImportJobResponse> run(@Valid @RequestBody RunLocalImportJobRequest request) {
        return ApiResponse.ok(localImportJobRunner.run(request));
    }
}
