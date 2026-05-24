# V2 知识维护巡检 Knowledge Maintenance Lint Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 R6-3 首版知识维护巡检能力，让系统在不依赖真实向量库的情况下发现重复、空内容和未归档知识。

**Architecture:** 本轮仍在 `wikiforge-core-service` 内实现最小巡检账本和只读问题列表。巡检规则基于 MySQL 中已有 `source_contents`、`personal_records`、`content_chunks`、`vector_export_jobs`，不自动修改用户数据，不引入调度器和真实向量库。

**Tech Stack:** Java 21, Spring Boot 3, JdbcTemplate, Flyway, MySQL 8.4, Vue 3, Vite, TypeScript, Element Plus.

---

## 任务卡 Task Card

任务ID：R6-3  
父任务：R6 / V2 知识运行层  
当前状态：Doing  
Owner：主开发 Agent  
目标：用户可在 Dashboard 手动触发知识维护巡检，并看到可处理的问题列表。  
范围：Core Service API、MySQL 表、Dashboard 入口、文档同步。  
禁止范围：自动修复、自动重组 Obsidian、定时任务、真实向量库、Hybrid Search、办公室视图。  
验收命令：Maven 定向测试、前端构建、Docker Compose config、Git 卫生检查。  
挂起事项：R6-2 Hybrid Search 继续等待向量库选型；R6-4 / R6-5 继续保留在 Roadmap。

## 文件边界 File Boundary

- Create: `backend/wikiforge-core-service/src/main/resources/db/migration/V20260524_003__create_knowledge_maintenance_tables.sql`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/CreateKnowledgeMaintenanceRunRequest.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/KnowledgeMaintenanceItemResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/KnowledgeMaintenanceItemPageResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/KnowledgeMaintenanceRunResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/KnowledgeMaintenanceRunPageResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/KnowledgeMaintenanceService.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/interfaces/web/KnowledgeMaintenanceController.java`
- Create: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/KnowledgeMaintenanceApiIntegrationTests.java`
- Create: `frontend/src/types/knowledgeMaintenance.ts`
- Create: `frontend/src/api/knowledge-maintenance/index.ts`
- Modify: `backend/wikiforge-common/src/main/java/com/wikiforge/common/error/ErrorCode.java`
- Modify: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/MigrationSqlCompatibilityTests.java`
- Modify: `frontend/src/views/DashboardView.vue`
- Modify: `frontend/src/styles/main.css`
- Modify: `docs/current/*`, `docs/archive/2026-05-24/*`

## 巡检规则 Maintenance Rules

- `missing_source_content`：`source_contents.raw_text` 为空或全空白。
- `duplicate_source_content`：多个 Source Content 拥有相同 `text_hash`。
- `unarchived_personal_record`：Personal Record 尚未写入 Obsidian，且创建时间早于指定天数。
- `empty_vector_export`：已完成的 Vector Export job 的 `total_count` 为 0。
- `stale_vector_chunk`：`content_chunks.embedding_status = 'pending'`，且创建时间早于指定天数。

## 执行步骤 Execution Steps

- [x] Step 1：先写后端集成测试，验证触发巡检、列表查询和 issue 计数。
- [x] Step 2：运行定向测试，确认因缺少 API / 表而失败。
- [x] Step 3：新增 Flyway 表：`knowledge_maintenance_runs`、`knowledge_maintenance_items`。
- [x] Step 4：实现 Core API：`POST /api/v1/maintenance-runs`、`GET /api/v1/maintenance-runs`、`GET /api/v1/maintenance-items`。
- [x] Step 5：实现巡检规则，只记录问题，不自动改用户资料。
- [x] Step 6：Dashboard 增加 `Maintenance 维护巡检` 区块，可手动运行并查看问题列表。
- [x] Step 7：更新 Roadmap、数据模型、技术架构、开发者日志、版本记录和归档索引。
- [x] Step 8：运行验证并提交推送。

## 验证记录 Verification Record

- RED：定向 Maven 测试失败符合预期，维护巡检 API 返回 404，migration 文件不存在。
- GREEN：定向 Maven 测试通过，`KnowledgeMaintenanceApiIntegrationTests` 和 `MigrationSqlCompatibilityTests` 合计 10 个测试，0 失败。
- 后端全量测试：通过，5 个模块合计 62 个测试，0 失败。
- 前端构建：通过，保留既有 Rollup 大 chunk 和 VueUse PURE 注释 warning。
- Compose 配置：生产与开发配置均通过。
- Git 卫生：`git diff --check`、敏感信息扫描、禁止路径扫描通过。
- 浏览器/页面检查：`http://localhost:3000/` 返回 200 且包含 Vue app 挂载节点；当前会话缺少 Playwright 模块，未完成自动截图检查。

## 测试门禁 Test Gate

本节点目标门禁：T2 构建验证。

- 后端定向测试：`mvn -s %TEMP%\wikiforge-maven-settings.xml -gs %TEMP%\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\repository" -pl wikiforge-core-service -am "-Dtest=KnowledgeMaintenanceApiIntegrationTests,MigrationSqlCompatibilityTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 前端构建：`npm --prefix frontend run build`
- Compose 配置：`docker compose -f deploy/docker-compose.yml config --quiet`
- Git 卫生：`git diff --check`

## 挂起事项 Suspended Items

- R6-2 真实向量库接入与 Hybrid Search：等待向量库选型和部署方式确认。
- R6-4 奥恩工坊办公室视图：等待 Agent 状态模型和任务流更稳定。
- R6-5 定时总结和长期记忆：等待总结触发策略、隐私边界和存储策略进一步确认。
