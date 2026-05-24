# V2 知识维护处理闭环 Knowledge Maintenance Issue Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 R6-3 维护巡检从“发现问题”升级为“可处理、可追踪、可重新打开”的维护问题队列。

**Architecture:** 本轮继续由 `wikiforge-core-service` 承担最小闭环，不新增独立 Maintain Service。数据库在 `knowledge_maintenance_items` 上追加处理字段，Web UI 在现有 Dashboard 维护巡检区块内增加操作，不自动修复用户资料。

**Tech Stack:** Java 21, Spring Boot, MySQL, Flyway, JdbcTemplate, Vue 3, TypeScript, Element Plus.

---

## Issue 风格任务卡

任务ID：R6-3.1
父任务：R6-3 / V2 知识维护巡检
当前状态：Done
Owner：主开发 Agent
目标：用户可以在维护巡检问题列表中标记已解决、忽略或重新打开问题，并记录处理备注。
范围：Core API、Flyway migration、Dashboard 维护巡检区块、PRD/架构/数据模型/Roadmap/开发者日志/归档索引。
允许修改文件：`backend/wikiforge-core-service/**`、`frontend/src/api/knowledge-maintenance/**`、`frontend/src/types/knowledgeMaintenance.ts`、`frontend/src/views/DashboardView.vue`、本 Work Order 和本轮相关文档。
禁止修改文件：`E:\WikiForgeVault`、Raw Sources、真实 `.env`、前端编译产物、后端 `target`、R6-2 向量库选型实现、R6-4 办公室视图。
输入契约：已有 `knowledge_maintenance_runs`、`knowledge_maintenance_items`、`GET /api/v1/maintenance-items`。
输出契约：新增 `PATCH /api/v1/maintenance-items/{itemUid}/status`。
验收命令：后端定向测试、前端构建、Compose config、Git 卫生扫描。
文档更新：PRD、技术架构、数据模型、Roadmap、开发者日志、版本更新记录、归档索引。
风险：首版只保存最新一次处理信息，不做完整处理历史表；后续如需审计链路再新增 `knowledge_maintenance_item_events`。
下一步：R6-3.2 可增加修复建议模板或 Maintain Agent 自动草案；R6-4 办公室视图继续挂起。

## 冻结契约 Contract

### API

```text
PATCH /api/v1/maintenance-items/{itemUid}/status
```

请求体：

```json
{
  "status": "resolved",
  "resolutionNote": "已确认该问题不影响当前资料整理",
  "resolvedBy": "web-ui"
}
```

状态值：

- `open`：重新打开，清空本轮处理字段。
- `resolved`：已解决，写入处理备注、处理人和处理时间。
- `ignored`：忽略，写入处理备注、处理人和处理时间。

响应体：复用 `KnowledgeMaintenanceItemResponse`，新增字段：

```json
{
  "resolutionNote": "已确认该问题不影响当前资料整理",
  "resolvedBy": "web-ui",
  "resolvedAt": "2026-05-24T10:00:00+08:00"
}
```

### 数据模型

新增 migration：

```text
V20260524_004__extend_maintenance_items_workflow.sql
```

追加字段：

```sql
ALTER TABLE knowledge_maintenance_items
    ADD COLUMN resolution_note TEXT NULL AFTER status,
    ADD COLUMN resolved_by VARCHAR(128) NULL AFTER resolution_note,
    ADD COLUMN resolved_at DATETIME NULL AFTER resolved_by,
    ADD KEY idx_maintenance_items_resolved_at (resolved_at);
```

## 任务清单

### Task 1: 文档和契约冻结

**Files:**

- Modify: `docs/current/需求文档-knowledge-base-prd.md`
- Modify: `docs/current/技术架构-technical-architecture.md`
- Modify: `docs/current/数据模型-data-model.md`
- Modify: `docs/current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md`
- Modify: `docs/current/2026-05-24-开发者日志-WikiForge-developer-log.md`
- Modify: `docs/current/2026-05-24-版本更新记录-WikiForge-release-notes.md`
- Modify: `docs/archive/2026-05-24/2026-05-24-归档索引-archive-index-v0.4.md`

- [x] Step 1: 补充 R6-3.1 范围、状态流转和不做自动修复的边界。
- [x] Step 2: Roadmap 增加 R6-3.1 指针，并把当前阶段改为 Doing。
- [x] Step 3: 归档索引升级到 v0.4，指向本 Work Order。
- [x] Step 4: 运行 `git diff --check`。

### Task 2: 后端 TDD

**Files:**

- Modify: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/KnowledgeMaintenanceApiIntegrationTests.java`
- Modify: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/MigrationSqlCompatibilityTests.java`

- [x] Step 1: 写失败测试，验证 `PATCH /api/v1/maintenance-items/{itemUid}/status` 可以 `resolved -> open`，并校验处理备注、处理人和处理时间。
- [x] Step 2: 写失败测试，验证非法状态返回 `MAINTENANCE_001`，不存在的问题返回 `MAINTENANCE_004`。
- [x] Step 3: 运行后端定向测试，预期因 API 和 migration 尚未实现而失败。

### Task 3: 后端实现

**Files:**

- Create: `backend/wikiforge-core-service/src/main/resources/db/migration/V20260524_004__extend_maintenance_items_workflow.sql`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/UpdateKnowledgeMaintenanceItemStatusRequest.java`
- Modify: `backend/wikiforge-common/src/main/java/com/wikiforge/common/error/ErrorCode.java`
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/KnowledgeMaintenanceItemResponse.java`
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/KnowledgeMaintenanceService.java`
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/interfaces/web/KnowledgeMaintenanceController.java`

- [x] Step 1: 增加 migration 和 migration 兼容性测试。
- [x] Step 2: 增加 DTO 和错误码 `MAINTENANCE_ITEM_NOT_FOUND`。
- [x] Step 3: Service 增加状态校验、更新和读取逻辑。
- [x] Step 4: Controller 暴露 PATCH API。
- [x] Step 5: 运行后端定向测试，预期通过。

### Task 4: 前端处理操作

**Files:**

- Modify: `frontend/src/types/knowledgeMaintenance.ts`
- Modify: `frontend/src/api/knowledge-maintenance/index.ts`
- Modify: `frontend/src/views/DashboardView.vue`

- [x] Step 1: 前端类型补充处理字段和更新请求。
- [x] Step 2: API 封装 `updateKnowledgeMaintenanceItemStatus`。
- [x] Step 3: Dashboard 维护问题表格增加 `已解决`、`忽略`、`重新打开` 操作。
- [x] Step 4: 操作后刷新维护问题列表，并展示成功或失败消息。
- [x] Step 5: 运行 `npm --prefix frontend run build`。

### Task 5: 验证和收口

**Files:**

- Modify: `docs/current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md`
- Modify: `docs/current/2026-05-24-开发者日志-WikiForge-developer-log.md`
- Modify: `docs/current/2026-05-24-版本更新记录-WikiForge-release-notes.md`
- Modify: `docs/archive/2026-05-24/2026-05-24-归档索引-archive-index-v0.4.md`

- [x] Step 1: 后端定向测试通过。
- [x] Step 2: 前端构建通过。
- [x] Step 3: Docker Compose config 通过。
- [x] Step 4: Git 卫生、密钥和禁止路径扫描通过。
- [x] Step 5: 更新 Work Order 勾选状态、Roadmap、开发者日志和归档索引。
- [x] Step 6: 提交并推送任务分支。
