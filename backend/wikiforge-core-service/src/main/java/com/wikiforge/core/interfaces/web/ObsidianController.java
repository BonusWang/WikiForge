package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.ObsidianInitResponse;
import com.wikiforge.core.application.dto.ObsidianNotePreviewResponse;
import com.wikiforge.core.application.dto.ObsidianNoteResponse;
import com.wikiforge.core.application.dto.ObsidianVaultStatusResponse;
import com.wikiforge.core.application.dto.SourceNoteDraftResponse;
import com.wikiforge.core.application.dto.WriteSourceNoteRequest;
import com.wikiforge.core.application.service.ObsidianVaultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ObsidianController {

    private final ObsidianVaultService obsidianVaultService;

    public ObsidianController(ObsidianVaultService obsidianVaultService) {
        this.obsidianVaultService = obsidianVaultService;
    }

    @PostMapping("/obsidian/init")
    public ApiResponse<ObsidianInitResponse> initializeVault() {
        return ApiResponse.ok(obsidianVaultService.initializeVault());
    }

    @GetMapping("/obsidian/status")
    public ApiResponse<ObsidianVaultStatusResponse> status() {
        return ApiResponse.ok(obsidianVaultService.status());
    }

    @GetMapping("/source-files/{fileUid}/obsidian-note")
    public ApiResponse<ObsidianNoteResponse> findSourceFileNote(@PathVariable String fileUid) {
        return ApiResponse.ok(obsidianVaultService.findSourceFileNote(fileUid));
    }

    @PostMapping("/source-files/{fileUid}/obsidian-note/draft")
    public ApiResponse<SourceNoteDraftResponse> generateDraft(@PathVariable String fileUid) {
        return ApiResponse.ok(obsidianVaultService.generateDraft(fileUid));
    }

    @PostMapping("/source-files/{fileUid}/obsidian-note/write")
    public ApiResponse<ObsidianNoteResponse> writeSourceNote(
            @PathVariable String fileUid,
            @RequestBody(required = false) WriteSourceNoteRequest request
    ) {
        String markdown = request == null ? null : request.markdown();
        return ApiResponse.ok(obsidianVaultService.writeSourceNote(fileUid, markdown));
    }

    @GetMapping("/obsidian/notes/{noteUid}/preview")
    public ApiResponse<ObsidianNotePreviewResponse> preview(@PathVariable String noteUid) {
        return ApiResponse.ok(obsidianVaultService.preview(noteUid));
    }
}
