package com.wikiforge.worker.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/worker")
public class WorkerHealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "service", "wikiforge-worker-service",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
