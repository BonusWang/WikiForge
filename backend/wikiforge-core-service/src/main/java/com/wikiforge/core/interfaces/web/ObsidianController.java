package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.ObsidianInitResponse;
import com.wikiforge.core.application.dto.ObsidianVaultStatusResponse;
import com.wikiforge.core.application.service.ObsidianVaultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
}
