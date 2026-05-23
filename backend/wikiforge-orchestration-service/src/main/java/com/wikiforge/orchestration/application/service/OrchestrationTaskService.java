package com.wikiforge.orchestration.application.service;

import com.wikiforge.common.error.BusinessException;
import com.wikiforge.common.error.ErrorCode;
import com.wikiforge.orchestration.application.dto.OrchestrationOverviewResponse;
import com.wikiforge.orchestration.application.dto.OrchestrationTaskResponse;
import com.wikiforge.orchestration.application.dto.TaskStatsResponse;
import com.wikiforge.orchestration.domain.model.OrchestrationTask;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(OrchestrationProperties.class)
public class OrchestrationTaskService {

    private final OrchestrationProperties properties;
    private final List<OrchestrationTask> tasks;

    public OrchestrationTaskService(OrchestrationProperties properties) {
        this.properties = properties;
        this.tasks = seedTasks();
    }

    public OrchestrationOverviewResponse overview() {
        return new OrchestrationOverviewResponse(
                "WikiForge Orchestration auxiliary workflow",
                properties.workflowEntry(),
                properties.projectRoadmap(),
                properties.activeBranch(),
                "S7 / R4 MVP5 Orchestration + MCP",
                "seeded-from-workflow-and-roadmap",
                stats(),
                List.of(
                        "Complete R4-1 orchestration service and UI skeleton",
                        "Update docs, Skill, archive index, Docker, and CI",
                        "Move to R4-2 MCP tool and permission boundary freeze"
                )
        );
    }

    public List<OrchestrationTaskResponse> listTasks() {
        return tasks.stream()
                .map(this::toResponse)
                .toList();
    }

    public OrchestrationTaskResponse getTask(String taskId) {
        return tasks.stream()
                .filter(task -> task.taskId().equalsIgnoreCase(taskId))
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ORCHESTRATION_TASK_NOT_FOUND,
                        "orchestration task not found"
                ));
    }

    private TaskStatsResponse stats() {
        return new TaskStatsResponse(
                tasks.size(),
                countByStatus("Ready"),
                countByStatus("Doing"),
                countByStatus("Review"),
                countByStatus("Blocked"),
                countByStatus("Done")
        );
    }

    private int countByStatus(String status) {
        return (int) tasks.stream()
                .filter(task -> task.status().equalsIgnoreCase(status))
                .count();
    }

    private OrchestrationTaskResponse toResponse(OrchestrationTask task) {
        return new OrchestrationTaskResponse(
                task.taskId(),
                task.parentTask(),
                task.title(),
                task.status(),
                task.owner(),
                task.goal(),
                task.scope(),
                task.allowedFiles(),
                task.forbiddenFiles(),
                task.contracts(),
                task.verificationCommands(),
                task.handoff(),
                task.nextStep(),
                task.tags()
        );
    }

    private List<OrchestrationTask> seedTasks() {
        return List.of(
                new OrchestrationTask(
                        "R4-0",
                        "S7 / MVP5",
                        "Upgrade Agent workflow into WikiForge Orchestration mode",
                        "Done",
                        "Main Orchestrator Agent",
                        "Create the shared workflow entry, task card template, and project Skill updates.",
                        "Docs and rules only.",
                        List.of("WORKFLOW.md", "AGENTS.md", "docs/**", ".github/ISSUE_TEMPLATE/**"),
                        List.of("backend/**", "frontend/**", "orchestration-ui/**", "deploy/**"),
                        List.of("Task cards must include owner, status, file boundary, verification, handoff."),
                        List.of("git diff --check", "skill quick validate"),
                        "Document files changed, validation output, and archive snapshot versions.",
                        "Proceed to R4-1 orchestration service and UI skeleton.",
                        List.of("docs", "workflow")
                ),
                new OrchestrationTask(
                        "R4-1",
                        "S7 / MVP5",
                        "Create Orchestration Service and independent UI skeleton",
                        "Doing",
                        "Main Orchestrator Agent",
                        "Provide a long-lived development control console for WikiForge tasks and agents.",
                        "Read-only service and UI. No command execution, no GitHub mutation.",
                        List.of("backend/wikiforge-orchestration-service/**", "orchestration-ui/**", "deploy/**", ".github/workflows/**", "docs/**"),
                        List.of("backend/wikiforge-core-service/src/main/java/**", "backend/wikiforge-worker-service/src/main/java/**", "data/**", ".env"),
                        List.of("REST responses use ApiResponse<T>.", "First API surface is health, overview, tasks, task detail."),
                        List.of("mvn test", "npm --prefix orchestration-ui run build", "docker compose -f deploy/docker-compose.yml config --quiet"),
                        "List service/UI endpoints, Docker/CI changes, and any tests not run.",
                        "Freeze MCP tools and permission boundary in R4-2.",
                        List.of("backend", "frontend", "devops")
                ),
                new OrchestrationTask(
                        "R4-2",
                        "S7 / MVP5",
                        "Freeze MCP tool list and permission boundary",
                        "Ready",
                        "Contract API Designer Agent",
                        "Define local HTTP Preview API and tool schemas before implementing tools.",
                        "Docs, data model, and architecture decisions.",
                        List.of("docs/current/**", "docs/superpowers/plans/**"),
                        List.of("backend/**/src/main/**", "frontend/**/src/**", "orchestration-ui/**"),
                        List.of("No local absolute path exposure.", "Use sourceUid, fileUid, noteUid, recordUid."),
                        List.of("git diff --check"),
                        "List frozen tool names, API paths, and permission notes.",
                        "Implement Source query tools in R4-3.",
                        List.of("mcp", "contract")
                ),
                new OrchestrationTask(
                        "R4-3",
                        "S7 / MVP5",
                        "Implement Source MCP Preview tools",
                        "Ready",
                        "Core Service Agent",
                        "Expose create_source, search_sources, and get_source through MCP Preview API.",
                        "Core Service API, service layer, and tests.",
                        List.of("backend/wikiforge-core-service/**"),
                        List.of("orchestration-ui/**", "docs/archive/**"),
                        List.of("Use frozen R4-2 contract.", "All calls must be logged."),
                        List.of("mvn -B -pl wikiforge-core-service -am test"),
                        "Report APIs, DTOs, tests, and remaining MCP gaps.",
                        "Implement Obsidian and personal record tools in R4-4.",
                        List.of("mcp", "core")
                )
        );
    }
}
