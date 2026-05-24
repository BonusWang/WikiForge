# 2026-05-23 WikiForge MVP5 轻量 MCP 预览实施计划 MCP Preview Implementation Plan

## 版本信息

- 文档版本：v0.7
- 当前分支：`codex/r4-4-mcp-obsidian-personal-record`
- 父分支：`main`
- 当前阶段：S7 / R4 MVP5 轻量 MCP 预览，R4-4 Obsidian Note 和 Personal Record 工具已完成，下一节点为 R4-5 MCP 调用展示与 OpenClaw / Hermes 接入说明
- 当前测试门禁：T1 契约与单元测试

## 当前执行指针

> 注意：本小节使用 `(x)` 表示当前唯一执行指针，不表示完成状态；完成状态以“节点清单”的 `[x]` 为准。

- ( ) R4-0 升级 Symphony-inspired Agent 工作模式
- ( ) R4-1 Orchestration 辅助工程骨架
- ( ) R4-2 冻结 MCP 工具清单和权限边界
- ( ) R4-3 实现 `create_source` / `search_sources` / `get_source`
- ( ) R4-4 实现 `get_obsidian_note` / `create_personal_record`
- (x) R4-5 展示 MCP 调用日志、编写 OpenClaw / Hermes 接入说明

## 节点清单

| 完成 | 节点 | 状态 | 事项 | 测试门禁 |
| --- | --- | --- | --- | --- |
| [x] | R4-0 | Done | 升级 `WORKFLOW.md`、GitHub Issue 任务卡、AGENTS 和 Skill 规则 | T0 |
| [x] | R4-1 | Done | 完成 Orchestration Service 和独立 UI 骨架 | T3 |
| [x] | R4-2 | Done | 冻结 MCP 工具清单、权限边界和 Parallel Work Order | T0 |
| [x] | R4-3 | Done | 实现 Source 查询类工具和轻量调用入口 | T1 / T2 |
| [x] | R4-4 | Done | 实现 Obsidian Note 读取和个人记录写入 | T1 / T2 |
| [ ] | R4-5 | Doing | 展示调用日志并补充 OpenClaw / Hermes 本机接入说明 | T1 / T0 |

## R4-0 Symphony-inspired 工作模式升级

### 目标

先把 WikiForge 当前“文档计划 + Work Order”的协作模式升级为轻量 Symphony-inspired 模式，保证后续 Codex、OpenClaw、Hermes 或其他 Agent 接入时有统一任务入口。

### 交付物

- `WORKFLOW.md`
- `.github/ISSUE_TEMPLATE/wikiforge-agent-task.yml`
- `docs/process/2026-05-23-Symphony工作模式评估-WikiForge-symphony-workflow-review.md`
- `AGENTS.md` 工作流入口规则
- `docs/ai-skills/wikiforge-development/` Skill 和 references 更新
- 本 Work Order 当前指针更新

### 验收

```text
git diff --check: pass
Skill quick validate: pass
```

### 完成结果

- 新增根入口 `WORKFLOW.md`。
- 新增 `.github/ISSUE_TEMPLATE/wikiforge-agent-task.yml`。
- 新增 `docs/process/2026-05-23-Symphony工作模式评估-WikiForge-symphony-workflow-review.md`。
- 更新 `AGENTS.md`、`docs/README.md` 和 WikiForge Development Skill。
- 当前结论：采用轻量 Symphony-inspired 工作模式，不引入 Symphony 服务端。

## R4-2 契约冻结

冻结契约文档：

```text
docs/current/MCP接口契约-mcp-api-contract.md
```

### 范围选择

MVP5 先实现 WikiForge MCP 能力的轻量预览层：

- 对外暴露标准 MCP tool schema 形态：`name`、`description`、`inputSchema`。
- 第一轮提供 HTTP Preview API，便于 OpenClaw / Hermes / 外部 Agent 在本机调用和调试。
- 不在本轮引入完整 MCP Client、Marketplace、多用户权限、远程公网访问。
- 不直接暴露本地绝对路径，所有本地文件和 Obsidian 文件都通过 `sourceUid`、`fileUid`、`noteUid` 间接访问。
- 官方 MCP Java SDK 作为后续 transport 替换方向保留，MVP5 当前代码先冻结 tool schema 和调用日志契约，避免 SDK 版本兼容风险拖慢闭环。
- MVP5 HTTP Preview 暂归属 `wikiforge-core-service`，后续可拆出 `wikiforge-mcp-service`。
- R4-3 冻结 migration 编号为 `V20260523_006__create_mcp_preview_tables.sql`，主编排 Agent 串行创建。

### HTTP Preview API

MVP5 轻量入口：

```text
GET  /api/v1/mcp/tools
POST /api/v1/mcp/tools/{toolName}/call
GET  /api/v1/mcp/calls
```

响应仍使用 WikiForge 统一 `ApiResponse<T>`。

### 首批工具

| 工具 | 状态 | 输入 | 输出 | 权限边界 |
| --- | --- | --- | --- | --- |
| `search_sources` | MVP5 | `keyword`、`status`、`page`、`pageSize` | Source 摘要列表 | 不返回本地绝对路径 |
| `get_source` | MVP5 | `sourceUid` 或 `fileUid` | Source / SourceFile 元数据、Obsidian Note 摘要 | 不返回原始绝对路径 |
| `create_source` | MVP5 | `title`、`rawContent`、`sourceType`、`sourcePlatform` | 创建结果或待导入任务 | 第一轮可降级为创建记录草案，不做文件系统写入 |
| `get_obsidian_note` | MVP5 | `noteUid` | Vault 相对路径、Markdown 内容、Obsidian URI | 只允许读取 Vault 内已登记 Note |
| `create_personal_record` | MVP5 | `recordType`、`title`、`occurredAt`、`rawContent`、`structured` | 记录 UID 和状态 | 只做结构化存储，不做 AI 总结 |

详细 JSON Schema、输出字段、错误码和日志脱敏规则以 `docs/current/MCP接口契约-mcp-api-contract.md` 为准。

### 调用日志

所有工具调用写入 `mcp_tool_calls`，敏感输入需脱敏：

- `call_uid`
- `tool_name`
- `caller_type`
- `caller_id`
- `input_json`
- `output_json`
- `status`
- `error_code`
- `error_message`
- `duration_ms`
- `created_at`

`create_personal_record.rawContent`、`create_personal_record.structured`、`get_obsidian_note.markdown` 等敏感内容不写入调用日志原文。

### Headers

MVP5 不做完整登录，但记录调用来源：

```text
X-WikiForge-Caller-Type: user | agent | external_agent | system
X-WikiForge-Caller-Id: openclaw | hermes | local-dev
```

未传时默认：

```text
callerType = external_agent
callerId = local-mcp-preview
```

## R4-3 Source 类 MCP Preview 工具完成记录

R4-3 已完成 `search_sources`、`get_source`、`create_source` 三个 Source 类 MCP Preview 工具。

新增后端内容：

- Flyway migration：`V20260523_006__create_mcp_preview_tables.sql`。
- ErrorCode：MCP 工具不存在、未启用、输入非法、Source 不存在、Obsidian Note 不存在、路径暴露禁止、调用失败和个人记录类型非法。
- DTO：`McpToolDefinition`、`McpToolListResponse`、`McpToolCallRequest`、`McpToolCallResponse`、`McpToolCallPageResponse`。
- Service / Controller：`McpPreviewService`、`McpPreviewController`。
- 集成测试：`McpPreviewApiIntegrationTests`。

实现边界：

- `search_sources`、`get_source`、`create_source` 已启用。
- R4-3 阶段仅预置 `get_obsidian_note`、`create_personal_record` 工具入口；R4-4 已完成启用和安全验证。
- `create_source` 写入文本型 Source、伪 SourceFile、SourceContent 和导入任务记录，不读取本地路径，不抓取远程链接，不自动写 Obsidian，不自动调用 AI。
- `GET /api/v1/mcp/tools` 返回冻结工具的 `inputSchema`。
- 调用日志写入 `mcp_tool_calls`，`rawContent` 只记录长度和 SHA-256 摘要；disabled 工具调用也会记录 failed 日志。

验证记录：

```text
RED: McpPreviewApiIntegrationTests 初始返回 404，证明 MCP Preview API 未实现。
GREEN: mvn -B -s <temp-settings> -gs <temp-settings> "-Dmaven.repo.local=E:\repository" -pl wikiforge-core-service -am "-Dtest=McpPreviewApiIntegrationTests" "-Dsurefire.failIfNoSpecifiedTests=false" test: pass
```

## R4-4 Obsidian Note 和 Personal Record 工具完成记录

R4-4 已完成 `get_obsidian_note`、`create_personal_record` 两个 MCP Preview 工具。

实现边界：

- `get_obsidian_note` 已启用，按已登记 `noteUid` 读取 Obsidian Note。
- 返回结果只包含 Vault 相对路径、Obsidian URI、标题、状态和可选 Markdown，不返回本机绝对路径。
- `vaultPath` 在返回前必须校验为安全相对路径；即使 `includeMarkdown=false` 也不能绕过校验。
- Markdown 读取前会校验最终真实路径仍在配置的 Vault 根目录内，禁止路径穿越和符号链接逃逸。
- `create_personal_record` 已启用，支持消费、账单、邮件、人际关系、事件和普通笔记记录。
- 个人记录写入 `personal_records`，初始状态为 `pending`，本轮不做 AI 总结、不自动写 Obsidian。
- `mcp_tool_calls` 对 `rawContent`、`structured`、`markdown` 做脱敏日志，不记录原文。

验证记录：

```text
GREEN: mvn -B -s <temp-settings> -gs <temp-settings> "-Dmaven.repo.local=E:\repository" -pl wikiforge-core-service -am "-Dtest=McpPreviewApiIntegrationTests" "-Dsurefire.failIfNoSpecifiedTests=false" test: pass, Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

## Parallel Work Order

### PWO-R4-2 Docs / Contract

- 任务ID：PWO-R4-2
- 父任务：R4-2
- 专家角色：Contract API Designer Agent
- 目标服务或文档：docs
- 允许修改文件：`docs/current/`、`docs/superpowers/plans/`
- 禁止修改文件：后端源码、前端源码、Flyway migration
- 依赖任务：无
- 输入契约：MVP5 路线图、架构决策、数据模型
- 输出契约：本 Work Order 和 R4 工具清单
- 验证命令：`git diff --check`
- 是否可并行：否，契约冻结必须先串行
- 合并顺序：第一
- Handoff 要求：列出冻结的工具名、API path 和权限边界

### PWO-R4-3A Serial / Migration And Common Contract

- 任务ID：PWO-R4-3A
- 父任务：R4-3 / R4-5
- 专家角色：Main Orchestrator Agent
- 目标服务或文档：`wikiforge-common`、Core migration、Core DTO 契约
- 允许修改文件：`backend/wikiforge-common/`、`backend/wikiforge-core-service/src/main/resources/db/migration/`、Core MCP DTO 包
- 禁止修改文件：frontend、docs/archive、Worker Service
- 依赖任务：PWO-R4-2
- 输入契约：`docs/current/MCP接口契约-mcp-api-contract.md`
- 输出契约：错误码、`mcp_tool_calls`、`personal_records`、基础 request / response DTO
- 验证命令：`mvn -B -pl wikiforge-core-service -am -Dtest=McpPreviewApiIntegrationTests test`
- 是否可并行：否，高冲突串行区
- 合并顺序：第二
- Handoff 要求：说明 migration 编号、错误码和 DTO 名称

### PWO-R4-3B Core / Source MCP Preview

- 任务ID：PWO-R4-3B
- 父任务：R4-3 / R4-5
- 专家角色：Core Service Agent
- 目标服务或文档：`wikiforge-core-service`
- 允许修改文件：Core Service `application`、`domain`、`infrastructure`、`interfaces`、Core integration tests
- 禁止修改文件：frontend、docs/archive、Worker Service
- 依赖任务：PWO-R4-3A
- 输入契约：`search_sources`、`get_source`、`create_source`
- 输出契约：工具列表、Source 工具调用、调用日志
- 验证命令：`mvn -B -pl wikiforge-core-service -am -Dtest=McpPreviewApiIntegrationTests test`
- 是否可并行：可与 UI Agent 并行，但依赖 PWO-R4-3A 完成
- 合并顺序：第三
- Handoff 要求：说明实际新增 DTO、表、接口和测试

### PWO-R4-4 Core / Obsidian And Personal Record

- 任务ID：PWO-R4-4
- 父任务：R4-4 / R4-5
- 专家角色：Core Service Agent
- 目标服务或文档：`wikiforge-core-service`
- 允许修改文件：Core Service Obsidian MCP adapter、Personal Record repository/service、Core integration tests
- 禁止修改文件：frontend、docs/archive、Worker Service
- 依赖任务：PWO-R4-3A
- 输入契约：`get_obsidian_note`、`create_personal_record`
- 输出契约：Obsidian Note 读取工具、个人记录写入工具、调用日志
- 验证命令：`mvn -B -pl wikiforge-core-service -am -Dtest=McpPreviewApiIntegrationTests test`
- 是否可并行：可与 PWO-R4-3B 串后并行，但共享测试类需主编排合并
- 合并顺序：第四
- Handoff 要求：说明 personal_records 字段映射和敏感日志脱敏结果

### PWO-R4-5 UI / Dashboard

- 任务ID：PWO-R4-5
- 父任务：R4-3 / R4-5
- 专家角色：UI Agent
- 目标服务或文档：`frontend`
- 允许修改文件：`frontend/src/api/`、`frontend/src/types/`、`frontend/src/views/DashboardView.vue`、`frontend/src/styles/main.css`
- 禁止修改文件：backend、docs/archive
- 依赖任务：PWO-R4-3A API 契约，后端工具可用后做联调
- 输入契约：MCP tools 和 calls API
- 输出契约：Dashboard 展示 MCP 工具清单和最近调用
- 验证命令：`npm run build`
- 是否可并行：可与 Core 工具实现并行做 UI 骨架
- 合并顺序：第五
- Handoff 要求：说明新增前端类型和交互入口

### PWO-R4-6 Docs / Archive

- 任务ID：PWO-R4-6
- 父任务：R4-5
- 专家角色：Docs Agent
- 目标服务或文档：docs
- 允许修改文件：路线图、开发者日志、技术架构、数据模型、归档索引
- 禁止修改文件：代码
- 依赖任务：PWO-R4-3、PWO-R4-4 验证完成
- 输入契约：实现结果和验证结果
- 输出契约：当前文档和 2026-05-23 归档快照
- 验证命令：`git diff --check`
- 是否可并行：最终收口时串行
- 合并顺序：最后
- Handoff 要求：说明文档版本号和归档版本号

## 验证命令

```text
git diff --check
mvn -B -s <settings> -gs <settings> "-Dmaven.repo.local=E:\repository" -pl wikiforge-core-service -am "-Dtest=McpPreviewApiIntegrationTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
npm run build
docker compose -f deploy/docker-compose.yml config --quiet
docker compose -f deploy/docker-compose.dev.yml config --quiet
```
