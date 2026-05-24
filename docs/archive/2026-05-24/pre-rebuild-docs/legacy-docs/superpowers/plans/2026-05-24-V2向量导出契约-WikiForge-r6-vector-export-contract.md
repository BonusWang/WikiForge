# V2 向量导出契约 Vector Export Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 R6-1 向量导出契约，让 Source 正文和 Personal Record 先稳定导出为 JSONL chunks，后续再接入真实向量库和混合检索。

**Architecture:** 本轮只在 `wikiforge-core-service` 内新增导出任务和 chunk 账本，不引入独立 vector service。导出文件写入可配置本地目录，接口只返回相对路径，不暴露宿主机绝对路径。

**Tech Stack:** Java 21, Spring Boot 3, JdbcTemplate, Flyway, MySQL 8.4, Vue 3, Vite, TypeScript, Element Plus.

---

## 任务卡 Task Card

任务ID：R6-1  
父任务：R6 / V2 知识运行层  
当前状态：Doing  
Owner：主开发 Agent  
目标：可从已有资料生成可导入向量库的 JSONL chunk 文件  
范围：Core Service API、MySQL 表、Dashboard 入口、文档同步  
禁止范围：真实向量库、embedding 调用、hybrid search、办公室视图、定时总结  
验收命令：Maven 定向测试、前端构建、Docker Compose config、Git 卫生检查  
挂起事项：R6-2 到 R6-5 继续保留在 Roadmap，不在本切片实现

## 文件边界 File Boundary

- Create: `backend/wikiforge-core-service/src/main/resources/db/migration/V20260524_002__create_vector_export_tables.sql`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/CreateVectorExportRequest.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/VectorExportJobResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/VectorExportPageResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/VectorExportService.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/interfaces/web/VectorExportController.java`
- Create: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/VectorExportApiIntegrationTests.java`
- Create: `frontend/src/types/vectorExports.ts`
- Create: `frontend/src/api/vector-exports/index.ts`
- Modify: `backend/wikiforge-common/src/main/java/com/wikiforge/common/error/ErrorCode.java`
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/CoreRuntimeProperties.java`
- Modify: `backend/wikiforge-core-service/src/main/resources/application.yml`
- Modify: `.env.example`, `deploy/docker-compose.yml`
- Modify: `frontend/src/views/DashboardView.vue`
- Modify: `docs/current/*`, `docs/archive/2026-05-24/*`

## 执行步骤 Execution Steps

- [x] Step 1：自检当前阶段，确认 R5 已完成，R6-1 是下一可落地节点。
- [x] Step 2：新增 `vector_export_jobs` 和 `content_chunks` 表，记录导出任务和 chunk 账本。
- [x] Step 3：新增 Core API：`POST /api/v1/vector-exports`、`GET /api/v1/vector-exports`。
- [x] Step 4：从 `source_contents.raw_text` 和 `personal_records.raw_content` 生成 JSONL chunks。
- [x] Step 5：Dashboard 增加 Vector Export 区块，可创建导出任务并查看历史。
- [x] Step 6：更新需求、架构、数据模型、开发者日志和归档索引。
- [x] Step 7：运行验证并提交推送。

## 验证记录 Verification Record

- 后端定向测试：通过，8 个测试，0 失败。
- 后端全量测试：通过，5 个模块合计 58 个测试，0 失败。
- 前端构建：通过，保留既有 Rollup 大 chunk 和 VueUse PURE 注释 warning。
- Compose 配置：生产与开发配置均通过。
- Git 卫生：`git diff --check`、敏感信息扫描、禁止路径扫描通过。

## 测试门禁 Test Gate

本节点目标门禁：T2 构建验证。

- 后端定向测试：`mvn -s $settings -pl wikiforge-core-service -am "-Dtest=VectorExportApiIntegrationTests,MigrationSqlCompatibilityTests" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 前端构建：`npm --prefix frontend run build`
- Compose 配置：`docker compose -f deploy/docker-compose.yml config --quiet`
- Git 卫生：`git diff --check`

## 挂起事项 Suspended Items

- R6-2 真实向量库接入与 Hybrid Search：等待向量库选型和部署方式确认。
- R6-3 Lint / Maintain Agent：等待 chunk 账本和检索入口稳定。
- R6-4 奥恩工坊办公室视图：等待 Agent 状态模型和任务流更稳定。
- R6-5 定时总结和长期记忆：等待总结触发策略、隐私边界和存储策略进一步确认。
