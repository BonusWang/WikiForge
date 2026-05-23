# R4-5 MCP 调用看板任务计划 Team Plan

## 目标

让用户可以在 Web UI 中看到 MCP 工具和调用记录，并为 OpenClaw / Hermes 接入 WikiForge MCP Preview API 提供最小可执行说明。

## 当前执行指针

- ( ) 主Agent：建立 Agent Team 目录、规则和计划
- ( ) 前端开发Agent：实现 MCP 工具与调用日志展示
- ( ) 后端开发Agent：检查 MCP 后端缺口并补强必要测试
- ( ) 主Agent：进入 OpenClaw / Hermes 本机接入说明
- (x) 主Agent：R4-5 发布收口完成

## 节点清单

- [x] R4-5-0 建立 Agent Team 目录和角色工作空间
- [x] R4-5-1 前端 MCP tools / calls 数据接入，只读展示优先
- [x] R4-5-2 前端 Dashboard 展示 MCP 工具、调用结果和日志
- [x] R4-5-3 后端补 `GET /api/v1/mcp/calls` 查询端点集成测试
- [x] R4-5-4 OpenClaw / Hermes 本机接入说明
- [x] R4-5-5 集成验证、文档归档、提交推送和发布

## 本轮前端侦察结论

前端 Agent 已完成只读侦察。R4-5 首版建议优先做：

- MCP tools 只读列表。
- MCP calls 日志表。
- 默认不开放工具调用表单。

原因：`create_source` 和 `create_personal_record` 会真实写入业务数据；首版调用看板更适合作为观察和审计入口。若后续需要调用面板，应做成开发态 JSON 参数面板，并明确提示会写入数据。

首选落点仍是业务前端 `frontend/`，不放到 `orchestration-ui/`。`orchestration-ui` 当前代理到 `localhost:8090`，属于辅助编排控制台；直接接 Core MCP API 会引入额外跨服务代理问题。

## 本轮后端侦察结论

后端 Agent 已完成只读侦察。现有 API 已覆盖 R4-5 基础展示：

- `GET /api/v1/mcp/tools`
- `POST /api/v1/mcp/tools/{toolName}/call`
- `GET /api/v1/mcp/calls`

最小缺口：

- `GET /api/v1/mcp/calls` 缺少专门集成测试。
- 日志列表当前主要返回审计元信息，不返回 input / output JSON 详情；R4-5 首版可先展示元信息。
- 如果后续要展示 input / output，必须只返回脱敏版本。

## 本轮测试侦察结论

测试 Agent 已完成只读侦察。R4-5 推荐验收重点：

- tools 列表返回 5 个冻结工具。
- calls 列表返回分页、筛选、排序和状态字段。
- 成功和失败调用都写入 `mcp_tool_calls`。
- UI 展示工具、最近调用、状态、错误码、耗时、调用方。
- 接入说明必须使用示例 Header，不写真实 token 或密钥。

## 文件所有权

| Agent | 允许修改 | 禁止修改 |
| --- | --- | --- |
| 主Agent | `AGENTS.md`、`WORKFLOW.md`、`docs/current/`、`docs/archive/`、`agentteam/`、集成时必要代码 | 不直接覆盖专业 Agent 的未集成改动 |
| 前端开发Agent | `frontend/src/api/**`、`frontend/src/types/**`、`frontend/src/views/DashboardView.vue`、`frontend/src/styles/**`、自己的 `STATUS.md` | `backend/**`、`docs/current/**`、`docs/archive/**` |
| 后端开发Agent | `backend/wikiforge-core-service/**` 中 MCP 相关文件、自己的 `STATUS.md` | `frontend/**`、正式文档和归档索引 |
| 测试Agent | 只读为主；必要时可修改自己的 `STATUS.md` 或测试报告 | 业务实现、正式文档和归档索引 |

## 冲突规则

- 共享 DTO、错误码、Flyway migration、CI、Docker Compose 属于高冲突串行区，默认只由主 Agent 修改。
- 专业 Agent 不直接改 Roadmap、开发者日志、归档索引和 Release Notes。
- 每个专业 Agent 完成后只更新自己目录下的 `STATUS.md`。
- 主 Agent 集成后统一更新正式项目计划和归档快照。

## 验证门禁

- 文档和计划：`git diff --check`
- 后端小改：`mvn -pl wikiforge-core-service test`
- 前端联动：`npm run build`
- 发布前：后端全量测试、前端构建、Compose config、Git 卫生和敏感信息扫描

## R4-5-1/2/3 验证记录

- RED：`McpPreviewApiIntegrationTests#listCallsReturnsFilteredAuditPageWithoutInputOrOutputPayloads` 先失败，原因是 calls 查询 `pageSize=100` 被通用上限压成 50。
- GREEN：MCP calls 查询改用独立上限 100，单测通过。
- 后端定向：`McpPreviewApiIntegrationTests` 8 个测试通过。
- 前端构建：`npm --prefix frontend run build` 通过，保留既有 Rollup 大 chunk / PURE 注释 warning。
- 浏览器验证：`http://127.0.0.1:3002/` 可看到 MCP Preview、工具清单、调用日志列；无“调用工具”入口；控制台无 error。

## R4-5-4 接入说明记录

- 新增 `docs/current/2026-05-23-OpenClaw-Hermes接入说明-WikiForge-openclaw-hermes-mcp-integration.md`。
- 文档说明 OpenClaw / Hermes 当前以 HTTP Tool / Custom Action / Connector / Bridge Script 调用 WikiForge MCP HTTP Preview。
- 文档覆盖本机、Docker 外部容器、Compose 网络内三种 Base URL。
- 文档提供 tools、calls、create_source、create_personal_record、search_sources、get_source、get_obsidian_note 的 PowerShell 示例。
- 文档不包含真实 token、真实密钥或用户私密信息。

## R4-5-5 发布候选验证记录

- 后端全量：`mvn -s <temp-settings> "-Dmaven.repo.local=E:\repository" test` 通过，5 个模块合计 52 个测试。
- 前端构建：`npm --prefix frontend run build` 通过。
- Compose 配置：`deploy/docker-compose.yml` 和 `deploy/docker-compose.dev.yml` 均通过 `config --quiet`。
- Git 卫生：`git diff --check` 通过。
- 敏感信息扫描：未发现 MiniMax 密钥前缀、Bearer token 或常见 API key 模式。
- 禁止路径扫描：未发现 `node_modules`、`dist`、`target`、真实 `.env`、`data`、Vault 或 Raw Sources 被 Git 跟踪。
