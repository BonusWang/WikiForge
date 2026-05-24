package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.UploadSourcesResponse;
import com.wikiforge.core.application.service.ImportJobService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload-sources")
public class UploadSourceController {

    private final ImportJobService importJobService;

    public UploadSourceController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadSourcesResponse> uploadSources(
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(required = false) String wikiWritebackMode
    ) {
        return ApiResponse.ok(importJobService.uploadSources(files, wikiWritebackMode));
    }
}
