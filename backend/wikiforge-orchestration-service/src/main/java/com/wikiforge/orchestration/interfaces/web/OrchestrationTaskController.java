package com.wikiforge.orchestration.interfaces.web;

import com.wikiforge.common.web.ApiResponse;
import com.wikiforge.orchestration.application.dto.OrchestrationOverviewResponse;
import com.wikiforge.orchestration.application.dto.OrchestrationTaskResponse;
import com.wikiforge.orchestration.application.service.OrchestrationTaskService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orchestration")
public class OrchestrationTaskController {

    private final OrchestrationTaskService taskService;

    public OrchestrationTaskController(OrchestrationTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/overview")
    public ApiResponse<OrchestrationOverviewResponse> overview() {
        return ApiResponse.ok(taskService.overview());
    }

    @GetMapping("/tasks")
    public ApiResponse<List<OrchestrationTaskResponse>> listTasks() {
        return ApiResponse.ok(taskService.listTasks());
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<OrchestrationTaskResponse> getTask(@PathVariable String taskId) {
        return ApiResponse.ok(taskService.getTask(taskId));
    }
}
