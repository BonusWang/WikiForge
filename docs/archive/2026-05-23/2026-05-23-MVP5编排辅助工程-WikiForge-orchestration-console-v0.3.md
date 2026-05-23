# 2026-05-23 WikiForge MVP5 编排辅助工程 Orchestration Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or execute task-by-task with review checkpoints. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 WikiForge 自己的长期开发编排辅助工程，先提供只读任务控制台，再继续 MCP 开发。

**Architecture:** 新增 `wikiforge-orchestration-service` 作为独立 Spring Boot 服务，新增 `orchestration-ui` 作为独立 Vue / Vite 控制台。第一版只读展示任务、状态、验证命令和 Handoff，不自动执行命令。

**Tech Stack:** Java 21、Spring Boot 3.3.6、Vue 3、Vite、TypeScript、Docker Compose、GitHub Actions。

---

## 版本信息

- 文档版本：v0.3
- 当前分支：`codex/mvp5-mcp-preview`
- 当前阶段：S7 / R4 MVP5 Orchestration 辅助工程已完成，下一节点为 MCP 契约冻结
- 当前测试门禁：T0 文档与 Git 卫生

## 当前执行指针

> 注意：本小节使用 `(x)` 表示当前唯一执行指针，不表示完成状态；完成状态以“节点清单”的 `[x]` 为准。

- ( ) R4-1 创建 Orchestration Service 和独立 UI 骨架
- (x) R4-2 冻结 MCP 工具清单和权限边界
- ( ) R4-3 实现 Source 查询类 MCP Preview 工具
- ( ) R4-4 实现 Obsidian Note 读取和个人记录写入工具
- ( ) R4-5 调用日志、接入说明、文档归档和提交推送

## 节点清单

| 完成 | 节点 | 状态 | 事项 | 测试门禁 |
| --- | --- | --- | --- | --- |
| [x] | R4-0 | Done | 升级 `WORKFLOW.md`、GitHub Issue 任务卡、AGENTS 和 Skill 规则 | T0 |
| [x] | R4-1 | Done | 创建 Orchestration Service、独立 UI、Docker 和 CI 基线 | T3 |
| [ ] | R4-2 | Doing | 冻结 MCP 工具清单、权限边界和 Parallel Work Order | T0 |
| [ ] | R4-3 | Todo | 实现 Source 查询类工具和轻量调用入口 | T1 / T2 |
| [ ] | R4-4 | Todo | 实现 Obsidian Note 读取和个人记录写入 | T1 / T2 |
| [ ] | R4-5 | Todo | 补充调用日志、OpenClaw / Hermes 接入说明和归档 | T3 |

## R4-1 任务卡

```text
任务ID：R4-1
父任务：S7 / MVP5
当前状态：Done
Owner：Main Orchestrator Agent
目标：新增 WikiForge Orchestration Service 和独立 UI，形成长期开发编排控制台骨架。
范围：后端模块、前端 UI、Docker、CI、文档规则和归档。
允许修改文件：backend/wikiforge-orchestration-service、orchestration-ui、deploy、.github/workflows、docs、AGENTS.md、WORKFLOW.md
禁止修改文件：Core / Worker 业务逻辑、Flyway migration、真实环境密钥、本地 data / Vault / Raw Sources
输入契约：WORKFLOW.md、AGENTS.md、项目整体计划、Symphony 工作模式评估
输出契约：任务列表 API、任务详情 API、控制台页面、Docker/CI 构建入口
验收命令：mvn test、npm run build、docker compose config、git diff --check
文档更新：路线图、技术架构、架构决策、开发者日志、归档索引、Skill
风险：不能自动执行本机命令；不能把开发编排服务误做成知识库业务服务。
下一步：R4-2 冻结 MCP 工具清单和权限边界。
```

## 文件计划

### 后端服务

- Create: `backend/wikiforge-orchestration-service/pom.xml`
- Create: `backend/wikiforge-orchestration-service/src/main/java/com/wikiforge/orchestration/WikiForgeOrchestrationApplication.java`
- Create: `backend/wikiforge-orchestration-service/src/main/java/com/wikiforge/orchestration/domain/model/OrchestrationTask.java`
- Create: `backend/wikiforge-orchestration-service/src/main/java/com/wikiforge/orchestration/application/service/OrchestrationTaskService.java`
- Create: `backend/wikiforge-orchestration-service/src/main/java/com/wikiforge/orchestration/application/dto/*.java`
- Create: `backend/wikiforge-orchestration-service/src/main/java/com/wikiforge/orchestration/interfaces/web/*.java`
- Create: `backend/wikiforge-orchestration-service/src/main/resources/application.yml`
- Create: `backend/wikiforge-orchestration-service/src/test/java/com/wikiforge/orchestration/OrchestrationApiTests.java`
- Modify: `backend/pom.xml`

### 独立 UI

- Create: `orchestration-ui/package.json`
- Create: `orchestration-ui/package-lock.json`
- Create: `orchestration-ui/index.html`
- Create: `orchestration-ui/nginx.conf`
- Create: `orchestration-ui/src/*`

### 工程化

- Create: `deploy/docker/orchestration-service.Dockerfile`
- Create: `deploy/docker/orchestration-ui.Dockerfile`
- Modify: `deploy/docker-compose.yml`
- Modify: `deploy/docker-compose.dev.yml`
- Modify: `.github/workflows/ci.yml`
- Modify: `.env.example`

## 验证命令

```text
git diff --check
mvn -B "-Dmaven.repo.local=E:\repository" test
npm --prefix orchestration-ui run build
docker compose -f deploy/docker-compose.yml config --quiet
docker compose -f deploy/docker-compose.dev.yml config --quiet
```

## R4-1 完成定义

- [x] `wikiforge-orchestration-service` 可启动并提供 `/api/health`。
- [x] `GET /api/v1/orchestration/overview` 返回当前模式、任务统计和来源。
- [x] `GET /api/v1/orchestration/tasks` 返回 R4 任务清单。
- [x] `GET /api/v1/orchestration/tasks/{taskId}` 返回任务详情。
- [x] `orchestration-ui` 可构建并展示任务看板。
- [x] Docker Compose 包含 Orchestration Service 和 Orchestration UI。
- [x] CI 包含 Orchestration UI 构建和两个新镜像构建。
- [x] 文档、Skill、开发日志和归档索引已同步。

## R4-1 完成记录

### 交付内容

- 新增后端模块 `backend/wikiforge-orchestration-service`。
- 新增独立前端 `orchestration-ui`。
- 新增 Dockerfile：`orchestration-service.Dockerfile`、`orchestration-ui.Dockerfile`。
- 更新 Docker Compose、CI、`.env.example`、`.dockerignore`。
- 本地启动验证：
  - `http://localhost:8090/api/health`
  - `http://localhost:3001/`
  - `http://localhost:3001/api/v1/orchestration/tasks`

### 验证结果

```text
git diff --check: pass
Skill quick validate: pass
mvn -B -s <temp-settings> -gs <temp-settings> "-Dmaven.repo.local=E:\repository" test: pass
mvn -B -s <temp-settings> -gs <temp-settings> "-Dmaven.repo.local=E:\repository" -DskipTests package: pass
npm --prefix orchestration-ui install: pass
npm --prefix orchestration-ui run build: pass
docker compose -f deploy/docker-compose.yml config --quiet: pass
docker compose -f deploy/docker-compose.dev.yml config --quiet: pass
docker build orchestration-service: pass
docker build orchestration-ui: pass
curl http://localhost:8090/api/health: pass
curl http://localhost:3001/: pass
curl http://localhost:3001/api/v1/orchestration/tasks: pass
```

### 已知边界

- Orchestration 第一版只读，不自动执行命令，不自动同步 GitHub Issue。
- Core / Worker 镜像完整重建曾因 Docker build 超过 10 分钟被主动停止；本地 Maven 全量测试和打包已通过，Compose 配置已通过，新 Orchestration 两个镜像已构建通过。
