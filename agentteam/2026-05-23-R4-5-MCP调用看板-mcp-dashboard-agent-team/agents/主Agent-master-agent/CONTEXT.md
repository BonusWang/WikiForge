# 主Agent Context

本文档记录当前主 Agent 的续接上下文，用于防止会话压缩、重启或切换新会话后跑偏。

## 当前身份

- 角色：WikiForge R4-5 主Agent Master Agent。
- 职责：统筹协调任务、跟踪任务、调整计划、检查专业 Agent 提交内容、最终集成验证、更新正式文档、提交推送和发布。
- 工作方式：先读最新快照和规则，再看当前 Team 目录；不要凭旧会话记忆直接行动。

## 项目上下文

- 项目名称：知识熔炉 WikiForge。
- 本地仓库：`E:\github\WikiForge`。
- 当前分支：`codex/r4-5-mcp-dashboard-connectors`。
- 主干分支：`main`。
- 当前日期：2026-05-23。
- 当前阶段：MVP5 / R4-5，目标是 MCP 调用展示与 OpenClaw / Hermes 接入说明。
- Obsidian Vault 路径：`E:\WikiForgeVault`。

## 用户偏好和硬规则

- 用户希望 Agent 持续推进，不要无故停下来。
- 每一轮任务结束后自动进入下一轮，不等待用户确认，直到 MVP 整体全流程结束或遇到真实阻塞；每轮只需汇总结果。
- 日常开发提交和推送已授权，提交信息要简要说明改动点。
- 版本标签和 GitHub Release 已授权由 Agent 在验证通过后直接发布，用户后续再调整。
- 远程分支删除、重命名仍需用户确认。
- 文档使用中文 + EnglishName；`README.md`、`AGENTS.md`、`WORKFLOW.md`、`SKILL.md` 等约定文件名保持原名。
- 前端 `node_modules/`、`dist/`，后端 `target/`、本地 `.env`、Vault、Raw Sources、运行数据不提交。
- 不把 API Key、Token、Vault 真实内容、Raw Sources 原文写入文档或日志。

## 当前已完成决策

- R4-4 已完成：`get_obsidian_note` 和 `create_personal_record` 已启用。
- MCP 仍是 HTTP Preview，不升级为完整 MCP transport。
- `mcp_tool_calls` 记录调用日志，敏感输入输出只做脱敏、长度、hash 或标记。
- R4-5 Agent Team 已采用文件夹式协作：
  - 一个 Team 一个目录。
  - 每个 Agent 一个目录。
  - 每个 Agent 包含 `README.md`、`PROMPT.md`、`SKILL.md`、`WORKSPACE.md`、`STATUS.md`。
- 已吸收 `claude-agent-examples` 三点：
  - Todolist：落到 `任务计划-team-plan.md` 的复选框和单选当前指针。
  - Subagent：一次性只读侦察只回传 Handoff。
  - Agent Team：固定角色通过各自目录和 `STATUS.md` 异步协作。

## 当前专业 Agent 结论

前端侦察 Agent 已回报：

- R4-5 首版优先做 MCP tools 只读列表和 mcp calls 日志展示。
- 默认不开放工具调用表单，因为 `create_source` 和 `create_personal_record` 会真实写入业务数据。
- 首选落点是业务前端 `frontend/`，不要放到 `orchestration-ui/`。
- `orchestration-ui` 当前代理到 `localhost:8090`，属于辅助编排控制台；直接接 Core MCP API 会引入额外代理和服务边界问题。

后端侦察 Agent 已回报：

- 现有 `GET /api/v1/mcp/tools`、`POST /api/v1/mcp/tools/{toolName}/call`、`GET /api/v1/mcp/calls` 已存在。
- R4-5 后端最小缺口是补 `GET /api/v1/mcp/calls` 查询端点集成测试。
- 日志列表当前适合展示审计元信息；如果后续展示 input / output，必须只返回脱敏摘要。

测试侦察 Agent 已回报：

- R4-5 测试应按 T0 / T1 / T2 / T3 / T4 递进。
- 必须关注 tools 列表、calls 分页筛选、成功和失败日志、脱敏、UI 展示、OpenClaw / Hermes 接入说明。
- Docker 实启依赖本机环境，发布候选阶段再做完整健康检查。

## 当前文件状态

本轮已新增或修改：

- `agentteam/README.md`
- `agentteam/2026-05-23-R4-5-MCP调用看板-mcp-dashboard-agent-team/**`
- `AGENTS.md`
- `WORKFLOW.md`
- `docs/ai-skills/wikiforge-development/SKILL.md`
- `docs/ai-skills/wikiforge-development/references/multi-agent-collaboration.md`
- `docs/current/2026-05-23-项目整体计划-WikiForge-project-roadmap.md`
- `docs/current/2026-05-23-开发者日志-WikiForge-developer-log.md`
- `docs/current/2026-05-23-版本更新记录-WikiForge-release-notes.md`
- `docs/current/分支管理策略-branch-strategy.md`
- `docs/archive/2026-05-23/**` 对应快照
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/McpPreviewService.java`
- `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/McpPreviewApiIntegrationTests.java`
- `frontend/src/types/mcp.ts`
- `frontend/src/api/mcp/index.ts`
- `frontend/src/views/DashboardView.vue`
- `frontend/src/styles/main.css`
- `docs/current/2026-05-23-OpenClaw-Hermes接入说明-WikiForge-openclaw-hermes-mcp-integration.md`
- `docs/README.md`

## R4-5 当前进展

- R4-5-1 / R4-5-2 / R4-5-3 已提交并推送：`106bff9 feat: add mcp read-only dashboard`。
- R4-5-4 已完成：新增 OpenClaw / Hermes 本机接入说明。
- 接入方式定位：HTTP Tool / Custom Action / Connector / Bridge Script 调用 MCP HTTP Preview。
- 接入说明覆盖本机 `localhost:8080`、Docker 外部容器 `host.docker.internal:8080`、Compose 网络内 `wikiforge-core-service:8080`。
- 当前执行指针：R4-5-5 集成验证、归档、提交推送、合入 main、标签和发布。

## 下一步建议

1. R4-5-5 发布收口时执行 `git diff --check`、后端定向或全量测试、前端构建、Compose config、Git 卫生检查和敏感信息扫描。
2. 更新 Roadmap、开发者日志、归档索引和 Release Notes。
3. 提交并推送当前分支。
4. 合入 `main` 并创建 tag / GitHub Release。
