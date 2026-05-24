package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.core.application.dto.DictionaryListResponse;
import com.wikiforge.core.application.service.SystemDictionaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dictionaries")
public class SystemDictionaryController {

    private final SystemDictionaryService systemDictionaryService;

    public SystemDictionaryController(SystemDictionaryService systemDictionaryService) {
        this.systemDictionaryService = systemDictionaryService;
    }

    @GetMapping
    public ApiResponse<DictionaryListResponse> listDictionaries(
            @RequestParam(required = false) String dictType
    ) {
        return ApiResponse.ok(systemDictionaryService.listDictionaries(dictType));
    }
}
