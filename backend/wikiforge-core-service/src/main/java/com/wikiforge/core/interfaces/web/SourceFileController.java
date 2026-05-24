package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.SourceFileResponse;
import com.wikiforge.core.application.service.ImportJobService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/source-files")
public class SourceFileController {

    private final ImportJobService importJobService;

    public SourceFileController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listSourceFiles(
            @RequestParam String jobUid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        return ApiResponse.ok(importJobService.listSourceFiles(jobUid, page, pageSize));
    }

    @GetMapping("/{fileUid}")
    public ApiResponse<SourceFileResponse> getSourceFile(@PathVariable String fileUid) {
        return ApiResponse.ok(importJobService.getSourceFile(fileUid));
    }
}
