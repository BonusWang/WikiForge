package com.wikiforge.core.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "service", "wikiforge-core-service",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
