package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.CreateVectorExportRequest;
import com.wikiforge.core.application.dto.VectorExportJobResponse;
import com.wikiforge.core.application.dto.VectorExportPageResponse;
import com.wikiforge.core.application.service.VectorExportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vector-exports")
public class VectorExportController {

    private final VectorExportService vectorExportService;

    public VectorExportController(VectorExportService vectorExportService) {
        this.vectorExportService = vectorExportService;
    }

    @PostMapping
    public ApiResponse<VectorExportJobResponse> createExport(@RequestBody CreateVectorExportRequest request) {
        return ApiResponse.ok(vectorExportService.createExport(request));
    }

    @GetMapping
    public ApiResponse<VectorExportPageResponse> listExports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(vectorExportService.listExports(status, page, pageSize));
    }
}
