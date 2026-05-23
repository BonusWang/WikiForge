package com.wikiforge.orchestration.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrchestrationHealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "service", "wikiforge-orchestration-service",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
