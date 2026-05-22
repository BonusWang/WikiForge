package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.SubmitSourceFilesBatchRequest;
import com.wikiforge.core.application.dto.UpdateImportJobStatusRequest;
import com.wikiforge.core.application.service.ImportJobService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/import-jobs")
public class InternalImportJobController {

    private static final String INTERNAL_TOKEN_HEADER = "X-WikiForge-Internal-Token";

    private final ImportJobService importJobService;

    public InternalImportJobController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @PatchMapping("/{jobUid}/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateStatus(
            @PathVariable String jobUid,
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @Valid @RequestBody UpdateImportJobStatusRequest request
    ) {
        if (!importJobService.hasValidInternalToken(internalToken)) {
            return unauthorized();
        }
        importJobService.updateStatus(jobUid, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("jobUid", jobUid)));
    }

    @PostMapping("/{jobUid}/source-files/batch")
    public ResponseEntity<ApiResponse<Map<String, String>>> submitSourceFiles(
            @PathVariable String jobUid,
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @Valid @RequestBody SubmitSourceFilesBatchRequest request
    ) {
        if (!importJobService.hasValidInternalToken(internalToken)) {
            return unauthorized();
        }
        importJobService.submitSourceFiles(jobUid, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("jobUid", jobUid)));
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> unauthorized() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED));
    }
}
