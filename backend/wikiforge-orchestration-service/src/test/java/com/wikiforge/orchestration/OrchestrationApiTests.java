package com.wikiforge.orchestration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrchestrationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsServiceName() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("wikiforge-orchestration-service"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void overviewReturnsCurrentWorkflowState() throws Exception {
        mockMvc.perform(get("/api/v1/orchestration/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("WikiForge Orchestration auxiliary workflow"))
                .andExpect(jsonPath("$.data.activeBranch").value("codex/mvp5-mcp-preview"))
                .andExpect(jsonPath("$.data.stats.total", greaterThanOrEqualTo(4)));
    }

    @Test
    void listTasksReturnsR4Tasks() throws Exception {
        mockMvc.perform(get("/api/v1/orchestration/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].taskId", hasItem("R4-1")))
                .andExpect(jsonPath("$.data[*].status", hasItem("Doing")));
    }

    @Test
    void getTaskReturnsTaskDetail() throws Exception {
        mockMvc.perform(get("/api/v1/orchestration/tasks/R4-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("R4-1"))
                .andExpect(jsonPath("$.data.title").value("Create Orchestration Service and independent UI skeleton"))
                .andExpect(jsonPath("$.data.verificationCommands[*]", hasItem("mvn test")));
    }
}
