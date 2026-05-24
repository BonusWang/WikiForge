# 2026-05-24 WikiForge 项目整体计划 Project Roadmap

## 版本信息

- 文档版本：v3.8
- 当前分支：`codex/ia-sidebar-import-cleanup`
- 当前工程阶段：R6-UI-2 / 路线与信息架构纠偏，最小 Wiki 编译闭环已落地
- 当前产品主线：本地文件 + 手工链接资料 -> 收集/解析 -> Source Note 溯源 -> AI 编译 -> Topic / Project Wiki 页面 -> 中等自动写入或审核队列

## 阅读规则

新参与的 AI 或开发者先读：

1. `AGENTS.md`
2. `docs/archive/2026-05-24/2026-05-24-归档索引-archive-index-v0.6.md`
3. 本文档
4. 当前执行节点对应的计划文档或 Work Order
5. 分支相关操作先读 `docs/current/分支管理策略-branch-strategy.md`

## Agent 进度识别规则 Progress Marking

为便于后续 Agent 快速识别当前进展，本文档同时使用复选框和单选框：

- 复选框 `- [x]` 表示阶段或节点已完成。
- 复选框 `- [ ]` 表示阶段或节点未完成。
- 单选框 `(x)` 表示当前唯一执行位置。
- 单选框 `( )` 表示候选但非当前执行位置。
- 完成任一阶段或节点后，必须同步更新本文档、对应自检清单、开发者日志和归档索引。

当前执行指针：

- ( ) S0 需求与架构基线
- ( ) S1 MVP0 工程骨架
- ( ) S2 MVP1 源文件归集
- ( ) S3 / R0 MVP2 Obsidian Source Note 内部开发基线
- ( ) S4 / R1 MVP2.1 可用性加固
- ( ) S5 / R2 MVP3 文档解析
- ( ) S6 / R3 MVP4 AI 辅助整理与审核
- ( ) S7 / R4 MVP5 Orchestration + MCP
- ( ) S8 / R5 V1 在线资料与个人记录
- (x) S9 / R6 V2 知识运行层

## 状态标记 Status

| 状态 | 含义 |
| --- | --- |
| Done | 已完成并验证 |
| In Progress | 当前正在执行 |
| Next | 下一批应执行 |
| Later | 后续阶段执行 |
| Blocked | 受外部条件阻塞 |
| Released | 已完成标签或 GitHub Release 发布 |

## 总体路线

WikiForge 采用“先整理，再提炼，再运行”的节奏。

```text
需求与架构基线
  -> MVP0 工程骨架
  -> MVP1 本地源文件归集
  -> MVP2 Obsidian Source Note
  -> MVP2.1 发布收束与可用性加固
  -> MVP3 文档解析与 Source Note 丰富化
  -> MVP4 AI 辅助整理与审核
  -> MVP5 Orchestration 辅助工程 + 轻量 MCP 预览
  -> V1 在线资料与个人记录
  -> V2 向量库与知识运行层
```

R6-UI-2 后的当前主线细化为：

```text
本地文件 + 手工链接资料
  -> 待整理资料
  -> Wiki 编译
  -> 自动追加 WikiForge Updates 或审核队列
  -> Obsidian Topic / Project Wiki 页面
```

## 阶段一览

| 完成 | 阶段 | 状态 | 目标 | 版本建议 | 验收重点 |
| --- | --- | --- | --- | --- | --- |
| [x] | S0 需求与架构基线 | Done | 明确产品定位、技术栈、服务边界、文档规则 | 0.01 | 需求、架构、数据模型、开发者日志完成 |
| [x] | S1 MVP0 工程骨架 | Done | 建立 Core / Worker / UI / MySQL / Docker / CI 基线 | 0.02 | 后端测试、前端构建、Compose config |
| [x] | S2 MVP1 源文件归集 | Done | 指定路径扫描、复制 Raw Sources、去重、任务状态、UI 列表 | 0.03 | Docker 端到端导入任务完成 |
| [x] | S3 MVP2 Obsidian Source Note | Done | Source File 生成 Source Note、写入 Vault、预览、打开 Obsidian | 0.04 | Docker 重建、Vault 写入、UI 可操作 |
| [x] | S4 MVP2.1 可用性加固 | Done | 让 MVP2 更像日常可用工具 | 0.05 | 可查看已写 Note、错误可见、重复写入策略清晰 |
| [x] | S5 MVP3 文档解析 | Done | 从文件元数据走向正文内容抽取 | 0.06 | Markdown / TXT / PDF / Word 基础解析完成 |
| [x] | S6 MVP4 AI 辅助整理 | Done | 接入国内模型，生成摘要、标签、分类建议和审核队列 | 0.07 | 人工审核后写入 Obsidian |
| [x] | S7 MVP5 Orchestration + MCP | Done | 先建立长期开发编排控制台，再让外部 Agent 调用 WikiForge | 0.08 | Orchestration UI 可看任务，MCP 工具可创建记录、查询 Source、读取 Note |
| [x] | S8 V1 在线资料与个人记录 | Done | 链接资料、邮件、账单、消费、人际关系等记录接入 | 1.0 | 多源记录进入统一整理系统 |
| [ ] | S9 V2 知识运行层 | In Progress | 向量库、混合检索、维护 Agent、办公室视图 | 2.0 | 知识可被 Agent 长期调用和维护 |

## 递进测试门禁 Progressive Test Gates

测试按阶段和节点递进，不按每个小功能碎片重复跑完整链路。每个 Work Order 先声明本节点需要到哪一层门禁，完成后再更新勾选状态。

当前测试层级单选：

- ( ) T0 文档与 Git 卫生：只改文档、规则、计划时使用。
- ( ) T1 契约与单元测试：改 DTO、Service、Mapper、基础逻辑时使用。
- (x) T2 构建验证：完成一个可构建代码切片时使用。
- ( ) T3 Docker 节点烟测：完成跨服务节点或发布候选时使用。
- ( ) T4 阶段级端到端验收：完成一个 MVP 阶段时使用。

递进顺序：

- [x] T0：`git diff --check`，确认文档、空白和 Git 卫生。
- [x] T1：后端 Maven 测试、前端类型/构建相关检查，按改动范围执行。
- [x] T2：后端/前端构建通过。
- [x] T3：Docker Compose config、重建、健康检查、节点级 API 烟测。
- [ ] T4：阶段发布前，按版本候选执行阶段级复核、提交、标签和推送。

执行规则：

- 文档-only 节点只需 T0。
- 单服务代码节点至少到 T1。
- 前后端联动节点至少到 T2。
- 涉及 Core / Worker / UI / Docker 的节点至少到 T3。
- 一个 MVP 阶段发布前才执行 T4，不为每个小功能重复做阶段级全量验收。

## 当前执行主线

### 发布规则更新 Release Automation

执行时间：2026-05-23

用户已更新规则：版本标签、GitHub Release 和阶段预览发布不再等待用户确认。后续 Agent 在节点验证通过、文档和归档同步后，应直接完成：

- 合入并推送 `main`。
- 创建语义清晰的 tag，例如 `0.08-preview.2`。
- 推送 tag。
- 如本机 GitHub CLI 权限可用，创建 GitHub Release；如果 CLI 或权限不可用，必须在最终说明中明确。

版本命名由 Agent 依据当前阶段选择，用户后续可以再调整。

### Agent Team 文件夹 Parallel Agent Team Workspace

执行时间：2026-05-23

主开发 Agent 继续负责当前主链路、高冲突文件、最终合并、验证、正式文档和发布。子节点只处理其他模块事项、只读审查或低冲突草案，通过 Agent Team 文件夹向主开发 Agent 交接：

```text
agentteam/{YYYY-MM-DD}-{task-id}-{team-name}/
  README.md
  任务计划-team-plan.md
  agents/
    {agent-name}/
      README.md
      PROMPT.md
      SKILL.md
      WORKSPACE.md
      STATUS.md
```

子节点不得直接修改 Roadmap、开发者日志、归档索引、发布说明和主 Work Order。主开发 Agent 读取各 Agent 的 `STATUS.md` 后统一集成。

本轮已建立：

```text
agentteam/2026-05-23-R4-5-MCP调用看板-mcp-dashboard-agent-team/
```

并包含：

- 主Agent Master Agent：统筹协调任务、跟踪任务、调整计划、检查专业 Agent 提交内容。
- 前端开发Agent Frontend Agent：MCP tools / calls 前端只读展示。
- 后端开发Agent Backend Agent：MCP Preview API 和调用日志后端缺口检查。
- 测试Agent Test Agent：R4-5 验证矩阵和风险审查。

前端侦察结论：R4-5 首版优先做 MCP tools 只读列表和 mcp calls 日志表，默认不开放工具调用，避免 UI 操作真实写入 Source 或 Personal Record。

### R0：MVP2 收束与发布准备

目标：把当前已经实现的 MVP2 从“代码完成”收束成“可复核、可发布、可继续开发”的稳定节点。

当前 R0 节点指针：

- ( ) R0-1 项目整体路线图
- ( ) R0-2 版本更新记录
- ( ) R0-3 Docker 端到端烟测
- ( ) R0-4 发布前自检清单
- ( ) R0-5 提交推送
- ( ) R0-6 版本标签和 GitHub Release 发布

| 节点 | 状态 | 事项 | 输出 | 验证 |
| --- | --- | --- | --- | --- |
| R0-1 | Done | 输出项目整体路线图，并加入 docs 入口和归档 | 本文档、归档快照、docs README 链接 | `git diff --check` |
| R0-2 | Done | 补充版本更新记录 0.04 草案 | `docs/current/2026-05-24-版本更新记录-WikiForge-release-notes.md` | 人工可读，边界清晰 |
| R0-3 | Done | 用 Docker 栈跑一次真实 MVP2 Source Note 端到端烟测 | 创建导入任务、写入 Source Note、预览 Markdown | Core / Worker / UI healthy，API 成功 |
| R0-4 | Done | 输出发布前自检清单 | `docs/current/2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist.md` | `git status --short` 未发现编译产物和本地运行数据 |
| R0-5 | Done | 提交并推送当前分支 | commit `a987780`，push `origin/codex/mvp2-obsidian-source-note` | 提交前 Git 卫生检查通过，远程分支已创建 |
| R0-6 | Released | 版本标签、GitHub Release 和正式发布定义 | tag/release 按验证结果直接执行 | 2026-05-23 起不再等待用户确认 |

### R1：MVP2.1 可用性加固

目标：让 MVP2 从“能写入”变成“好使用、好检查、可重复操作”。

当前 R1 节点指针：

- ( ) R1-0 契约冻结
- ( ) R1-1 已写 Note 状态与读取已有 Note
- ( ) R1-2 Vault 状态面板
- ( ) R1-3 重复写入策略
- ( ) R1-4 MVP2.1 回归验证记录
- ( ) R1-5 文档归档和提交推送

| 节点 | 状态 | 事项 | 主要文件 | 验收 |
| --- | --- | --- | --- | --- |
| R1-1 | Done | Source Files 表格展示是否已有 Obsidian Note | Core API、UI 表格 | 已写入 Note 的文件有可见状态 |
| R1-2 | Done | 支持读取已存在 Note，不必重复写入 | Core Service、UI API | 点击 Source Note 可打开已有预览 |
| R1-3 | Done | 明确重复写入默认策略：已有则预览，不自动重复写入 | PRD、Service、UI | 系统有稳定默认策略 |
| R1-4 | Done | Vault 状态面板：路径、目录、最近写入、错误提示 | UI Dashboard | 用户能判断 Vault 是否可用 |
| R1-5 | Done | 增加集成测试和回归验证记录 | backend tests / docs | 一条命令可验证 MVP2.1 关键链路 |

### R2：MVP3 文档解析与 Source Note 丰富化

目标：从“文件元数据归档”升级到“能读取正文并生成更有用的 Source Note”。

| 节点 | 状态 | 事项 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- |
| R2-1 | Done | 冻结 `source_contents` 或等效正文存储契约 | 数据模型、Flyway | 文本内容、hash、解析状态有表结构 |
| R2-2 | Done | Worker 支持 Markdown / TXT 解析 | Worker Service | `.md/.txt` 正文可入库 |
| R2-3 | Done | Worker 支持 PDF 基础文本抽取 | Worker Service | 样例 PDF 可抽取文本或返回明确失败原因 |
| R2-4 | Done | Worker 支持 Word 基础文本抽取 | Worker Service | `.docx` 样例可抽取文本 |
| R2-5 | Done | Source Note 模板加入正文摘录和来源片段 | Core Service、UI | Markdown 不只包含元数据 |
| R2-6 | Done | 文档归档、验证和提交推送 | docs / Git | Maven、前端构建、Compose config 通过并推送分支 |

### R3：MVP4 AI 辅助整理与审核

目标：让系统开始“提炼”，但仍保留人工审核。

| 节点 | 状态 | 事项 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- |
| R3-1 | Done | 冻结模型调用契约，支持 DeepSeek / MiniMax / CC Switch 切换 | Core / Agent 设计 | 配置可切换 provider |
| R3-2 | Done | 新增 `agent_runs`、`agent_steps`、`review_items` 最小表 | 数据模型、Flyway | 每次 AI 输出有运行账本 |
| R3-3 | Done | 生成摘要、标签、分类建议、风险标记 | Agent / Core | AI 输出结构化保存 |
| R3-4 | Done | Web UI 增加审核队列 | UI | 用户可查看待审核草案 |
| R3-5 | Done | 审核通过后更新 Obsidian Source Note | Core Service / UI | 人工确认后才改写知识层 |

R3 本轮完成记录：

- 后端新增 `agent_runs`、`agent_steps`、`review_items` Flyway DDL、MyBatis 仓储、Service 和 API。
- `POST /api/v1/source-files/{fileUid}/ai-review-runs` 可基于 `source_contents.raw_text` 生成待审核草案。
- AI Provider 通过 `WIKIFORGE_MODEL_<PROVIDER>_API_KEY`、`WIKIFORGE_MODEL_<PROVIDER>_BASE_URL`、`WIKIFORGE_MODEL_<PROVIDER>_MODEL` 和兼容的 MiniMax 环境变量接入；密钥不进入仓库。
- 未配置或调用失败时使用本地规则兜底，保障 MVP4 主链路可用。
- Web UI Source Files 行内增加 `AI 整理`入口，Dashboard 增加 Provider / Model / Base URL 配置行、审核队列和草案查看抽屉。
- `POST /api/v1/review-items/{reviewUid}/approve` 已支持人工确认后写入 Obsidian Source Note，并将审核项标记为 `approved`。
- Web UI 审核详情抽屉已增加 `通过并写入 Obsidian` 操作，成功后刷新审核队列并打开写入后的 Source Note 预览。

### R4：MVP5 Orchestration 辅助工程 + 轻量 MCP 预览

目标：先把长期开发编排控制台建立起来，再让 OpenClaw / Hermes / 外部 Agent 能调用 WikiForge 做记录和查询。

| 节点 | 状态 | 事项 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- |
| R4-0 | Done | 升级 Agent 工作流为 WikiForge Orchestration 模式 | `WORKFLOW.md`、AGENTS、Skill、Issue Template、process docs | 后续 Agent 能按任务卡和 Work Order 接入 |
| R4-1 | Done | 创建 Orchestration Service 和独立 UI 骨架 | `wikiforge-orchestration-service`、`orchestration-ui`、Docker、CI | 可查看任务清单、详情、状态统计 |
| R4-2 | Done | 冻结 MCP 工具清单和权限边界 | `docs/current/MCP接口契约-mcp-api-contract.md`、技术架构、数据模型、Work Order | 工具不越权，不直接暴露本地路径 |
| R4-3 | Done | 实现 `create_source` / `search_sources` / `get_source` | MCP Preview API | 外部客户端可调用 |
| R4-4 | Done | 实现 `get_obsidian_note` / `create_personal_record` | MCP Preview API | Agent 可读 Note、写个人记录 |
| R4-5 | Done | 展示 MCP 调用日志并编写 OpenClaw / Hermes 接入说明 | UI / MySQL / docs | 每次调用可追踪，本机可按说明接入 |

R4-3 本轮完成记录：

- Core Service 新增 MCP Preview API：`GET /api/v1/mcp/tools`、`POST /api/v1/mcp/tools/{toolName}/call`、`GET /api/v1/mcp/calls`。
- 已启用 `search_sources`、`get_source`、`create_source`。
- 已新增 `mcp_tool_calls`、`personal_records` migration 和 MCP 错误码。
- 工具调用日志已落库，`rawContent` 等敏感字段只记录长度和 hash。
- R4-3 阶段仅预置 `get_obsidian_note`、`create_personal_record` 工具入口；R4-4 已完成启用和安全验证。

R4-4 本轮完成记录：

- `get_obsidian_note` 已启用，只能通过已登记的 `noteUid` 读取 Obsidian Note。
- Note 读取只返回 Vault 相对路径、Obsidian URI、标题和可选 Markdown，不返回 `absolute_path`、Raw Sources 路径或本机绝对路径。
- Note Markdown 读取前会校验 Vault 根目录和相对路径，禁止绝对路径和路径逃逸。
- `create_personal_record` 已启用，可写入 `expense`、`bill`、`email`、`relationship`、`event`、`note` 类型的个人记录草案。
- 个人记录写入 `personal_records`，初始状态为 `pending`，本轮不做 AI 总结、不自动写 Obsidian。
- `mcp_tool_calls` 对 `rawContent`、`structured`、`markdown` 做脱敏日志，只保留长度、hash 或脱敏标记。
- 定向验证通过：`McpPreviewApiIntegrationTests` 覆盖工具启用、Note 安全读取、个人记录写入、日志脱敏、缺失 Note 错误和非法记录类型错误。

R4-5 当前执行指针：

- [x] R4-5-0 建立 Agent Team 目录、角色工作区和主 Agent 续接上下文。
- [x] R4-5-1 前端接入 MCP tools / calls API，首版只读展示。
- [x] R4-5-2 Dashboard 展示 MCP 工具清单、调用日志、过滤条件和分页。
- [x] R4-5-3 后端补 `GET /api/v1/mcp/calls` 查询端点集成测试，并将 calls 查询页大小上限对齐为 100。
- [x] R4-5-4 编写 OpenClaw / Hermes 本机接入说明和最小调用样例。
- [x] R4-5-5 R4-5 集成验证、文档归档、提交推送、合入 main、标签和发布。
- [x] R4-6 MVP 审核加固：默认端口仅本机绑定、旧分支配置修正、接入说明和发布状态同步。

R4-5 本轮完成记录：

- 前端新增 `frontend/src/api/mcp/` 和 `frontend/src/types/mcp.ts`，统一封装 MCP 工具清单和调用日志查询。
- Dashboard 新增 `MCP Preview` 只读看板，展示工具启用状态、工具说明、调用状态、调用方、耗时、错误信息、过滤条件和分页。
- 首版不提供真实“调用工具”按钮，避免 UI 误触发 `create_source` 或 `create_personal_record` 写入业务数据。
- 后端新增 calls 查询集成测试，覆盖按 `toolName`、`status`、`callerType` 过滤，并确认日志列表不返回 input / output payload 原文。
- 验证通过：`McpPreviewApiIntegrationTests` 8 个测试通过；前端 `npm run build` 通过；浏览器检查 `http://127.0.0.1:3002/` 可看到 MCP Preview、工具清单和调用日志，且无“调用工具”入口。
- 新增 `docs/current/2026-05-23-OpenClaw-Hermes接入说明-WikiForge-openclaw-hermes-mcp-integration.md`，说明本机、Docker 外部容器和 Compose 网络内三种 Base URL，以及 OpenClaw / Hermes 通过 HTTP Tool / Connector 调用 WikiForge MCP Preview 的方式。
- 接入说明提供 `GET /tools`、`create_personal_record`、`create_source`、`search_sources`、`get_source`、`get_obsidian_note`、`GET /calls` 的 PowerShell 示例；文档仅使用占位符，不写真实 token 或密钥。
- 发布候选验证通过：后端 Maven 全量测试 52 个测试通过；前端构建通过；两份 Docker Compose 配置通过；Git 空白、密钥和禁止路径扫描通过。
- 版本更新记录已新增 `0.08-preview.3`，本节点已合入 `main`，并已创建标签和 GitHub Release。
- GitHub Release: https://github.com/BonusWang/WikiForge/releases/tag/0.08-preview.3

R4-6 本轮审核加固记录：

- 发现生产与开发 Compose 默认把 MySQL、Core、Worker、Orchestration、UI 端口绑定到所有网卡；当前 MVP 无登录鉴权，已调整为默认 `127.0.0.1` 绑定。
- 发现 `.env.example` 和 Orchestration Compose 默认仍指向 `codex/mvp5-mcp-preview` 历史阶段分支，已统一改为 `main`。
- 发现 OpenClaw / Hermes 接入说明对 Docker 外部容器访问宿主机的前提说明不足，已补充同一 Compose 网络优先和显式开放端口的安全条件。
- 发现已发布标签 `0.08-preview.3` 位于发布候选提交，后续 `main` 又追加了发布记录文档；不移动既有标签，本轮使用 `0.08-preview.4` 记录审核加固。

### R5：V1 在线资料与个人记录

目标：把 WikiForge 从“文件知识库”扩展为个人 LifeOS 底座。

R5 当前执行指针：

- [x] R5-0 契约冻结与计划优化。
- [x] R5-1 链接类 Source REST API：输入 URL、平台、标题、正文或备注后可创建 Source。
- [x] R5-2 个人记录 REST API：消费、账单、邮件、人际关系、事件和普通笔记可入库。
- [x] R5-3 个人记录 Obsidian 归档模板：记录可写入 `00_Inbox_收集箱/Personal_个人记录`。
- [x] R5-4 Web UI LifeOS 操作区：能录入链接资料、个人记录、查看汇总和触发归档。
- [x] R5-5 集成测试、构建验证、文档归档、提交推送。

| 节点 | 状态 | 事项 | 验收 |
| --- | --- | --- | --- |
| R5-0 | Done | V1 可执行范围和 Work Order | `docs/superpowers/plans/2026-05-24-V1在线资料与个人记录-WikiForge-v1-lifeos-work-order.md` |
| R5-1 | Done | 链接类 Source REST API | 输入飞书/腾讯/网页 URL 后可创建 Source 草案 |
| R5-2 | Done | 个人记录服务：消费、账单、邮件、人际关系、事件 | 结构化记录可入库、可筛选、可汇总 |
| R5-3 | Done | 个人记录 Obsidian 页面模板 | 可按类型和月份写入 Vault |
| R5-4 | Done | Web UI LifeOS 操作区 | 独立 UI 看板可录入和查看 |
| R5-5 | Done | V1 集成验证与文档归档 | 后端测试、前端构建、归档索引通过 |

V1 边界说明：

- 飞书、腾讯文档、网页收藏第一版先做“链接资料收集入口”和统一 Source 契约；真实授权抓取和连接器 Fetcher 进入 V1.x。
- 个人记录第一版先做私有化入库、可筛选、可归档到 Obsidian；周期总结和 Agent 定时迭代进入后续节点。
- MCP Preview 仍保留外部机器人写入能力，REST API 和 Web UI 使用同一张 `personal_records` 表。

R5 首版完成记录：

- Core Service 新增 `POST /api/v1/link-sources`，链接资料可进入 `sources/source_files/source_contents`。
- Core Service 新增 `POST /api/v1/personal-records`、列表、详情、汇总和写入 Obsidian API。
- `personal_records` 增加 `obsidian_vault_path`、`obsidian_uri`、`archived_at` 字段。
- Obsidian Vault 初始化目录增加 `00_Inbox_收集箱/Personal_个人记录`。
- Dashboard 新增 LifeOS 收集区：链接资料、个人记录、汇总、归档操作。
- 后端全量测试、前端构建、Docker Compose config、`git diff --check`、密钥和禁止路径扫描已通过。

### R6：V2 知识运行层

目标：让沉淀后的知识进入长期运行和复用。

| 节点 | 状态 | 事项 | 验收 |
| --- | --- | --- | --- |
| R6-1 | Deferred UI | 向量导出契约预研 | 后端预研保留，当前 Web UI 不展示入口 |
| R6-2 | Blocked | Hybrid Search：MySQL 条件 + 向量 + rerank | 等待向量库选型和部署方式确认 |
| R6-3 | Done | 知识库体检首版 | 可发现重复、空正文和长期未归档个人记录 |
| R6-3.1 | Done | 维护问题处理闭环 | 问题可标记已解决、忽略或重新打开 |
| R6-UI-1 | Done | Console 暗色开发者控制台主题 | Dashboard 符合 AI 技术发布会 / 代码编辑器 / Terminal Deck 风格 |
| R6-UI-2 | Done | Console 信息架构与导入体验纠偏 | 左侧菜单拆分页面，导入只填知识来源地址，向量导出不再暴露 |
| R6-4 | Later | 办公室视图 | Agent 状态和任务流可视化 |
| R6-5 | Later | 定时总结和长期记忆 | 知识可持续演进 |

R6 当前执行指针：

- [x] R6-1-0 自检当前阶段、冻结 R6-1 Work Order。
- [x] R6-1-1 新增 `vector_export_jobs`、`content_chunks` DDL。
- [x] R6-1-2 新增 `POST /api/v1/vector-exports`、`GET /api/v1/vector-exports`。
- [x] R6-1-3 从 `source_contents.raw_text` 和 `personal_records.raw_content` 生成 JSONL chunks。
- [x] R6-1-4 Dashboard 曾增加 `Vector Export 向量导出` 区块；R6-UI-2 已根据用户反馈从当前 Web UI 移除。
- [x] R6-1-5 更新需求、架构、数据模型、开发者日志、版本记录和归档索引。
- [x] R6-1-6 验证、提交推送、合入 main、标签和发布。
- [x] R6-3-0 确认 R6-2 Hybrid Search 因向量库选型阻塞，转入不依赖外部选型的知识库体检。
- [x] R6-3-1 新增 `knowledge_maintenance_runs`、`knowledge_maintenance_items` DDL。
- [x] R6-3-2 新增 `POST /api/v1/maintenance-runs`、`GET /api/v1/maintenance-runs`、`GET /api/v1/maintenance-items`。
- [x] R6-3-3 实现空正文、重复正文、未归档个人记录等体检规则；R6-UI-2 默认体检已移除向量导出相关规则。
- [x] R6-3-4 Dashboard 曾增加 `Maintenance 维护巡检` 区块；R6-UI-2 已更名为 `知识库体检 Knowledge Health` 页面。
- [x] R6-3-5 更新需求、架构、数据模型、开发者日志、版本记录和归档索引。
- [x] R6-3-6 验证、提交推送、合入 main、标签和发布。
- [x] ( ) R6-3.1-0 补充需求、技术架构、数据模型和 Work Order。
- [x] ( ) R6-3.1-1 新增维护问题处理字段和 PATCH API。
- [x] ( ) R6-3.1-2 Dashboard 增加已解决、忽略、重新打开操作。
- [x] ( ) R6-3.1-3 定向测试、前端构建、Compose config 和 Git 卫生验证。
- [x] ( ) R6-3.1-4 更新开发者日志、归档索引、提交并推送。
- [x] ( ) R6-UI-1-0 分析本地 `claude-agent-examples/ppt` 参考 HTML 视觉语言。
- [x] ( ) R6-UI-1-1 重构 `frontend/src/styles/main.css` 为暗色开发者控制台主题。
- [x] ( ) R6-UI-1-2 更新需求文档和技术架构中的前端视觉规范。
- [x] ( ) R6-UI-1-3 执行前端构建、页面 HTTP 检查和 Git whitespace 检查。
- [x] ( ) R6-UI-2-0 接收用户反馈并确认五个纠偏点。
- [x] ( ) R6-UI-2-1 Dashboard 增加左侧模块菜单和页面级拆分。
- [x] ( ) R6-UI-2-2 本地导入移除 `rawSourcesRoot` 必填输入，后端改为配置默认。
- [x] ( ) R6-UI-2-3 将 Vector Export 移出资料整理主流程，放入高级能力区。
- [x] ( ) R6-UI-2-4 `Maintenance` 更名为知识库体检并收敛默认检查项。
- [x] ( ) R6-UI-2-5 执行后端定向测试、前端构建和 Git 检查。

R6-1 完成记录：

- 新增 R6-1 Work Order：`docs/superpowers/plans/2026-05-24-V2向量导出契约-WikiForge-r6-vector-export-contract.md`。
- 新增 `vector_export_jobs` 和 `content_chunks`，`total_count` 代表 chunk 数量。
- `content_chunks.embedding_status` 首版固定为 `pending`，为后续真实向量库导入保留状态。
- 导出文件写入 `WIKIFORGE_VECTOR_EXPORT_ROOT`，API 只返回相对路径，不暴露宿主机绝对路径。
- R6-UI-2 已根据用户反馈将当前 Web UI 的向量导出移出主流程；该能力保留在高级能力区并作为后续内部管道或高级功能评估。
- 本轮不接真实向量库、不生成 embedding、不做 Hybrid Search、不做办公室视图和定时总结。
- 验证完成：后端全量 58 个测试、前端构建、生产/开发 Compose config、Git 卫生、密钥扫描和禁止路径扫描均通过。

R6-3 完成记录：

- 新增 R6-3 Work Order：`docs/superpowers/plans/2026-05-24-V2知识维护巡检-WikiForge-r6-maintenance-lint-agent.md`。
- 新增 `knowledge_maintenance_runs` 和 `knowledge_maintenance_items`，记录手动巡检运行与发现的问题。
- Core Service 新增维护巡检 API：`POST /api/v1/maintenance-runs`、`GET /api/v1/maintenance-runs`、`GET /api/v1/maintenance-items`。
- 当前用户可见规则覆盖 `missing_source_content`、`duplicate_source_content`、`unarchived_personal_record`。
- Dashboard 页面已在 R6-UI-2 更名为 `知识库体检 Knowledge Health`，可手动运行、查看运行记录、按 runUid / issueType / status 筛选问题。
- 本轮不做自动修复、不做定时任务、不接真实向量库、不做办公室视图。
- 验证完成：后端全量 62 个测试、前端构建、生产/开发 Compose config、Git 卫生、密钥扫描和禁止路径扫描均通过。

R6-3.1 当前执行记录：

- 新增 R6-3.1 Work Order：`docs/superpowers/plans/2026-05-24-V2知识维护处理闭环-WikiForge-r6-maintenance-issue-workflow.md`。
- 本轮已把维护问题从只读列表升级为人工可处理队列。
- 冻结新增 API：`PATCH /api/v1/maintenance-items/{itemUid}/status`。
- 状态流转：`open -> resolved / ignored -> open`；重新打开以恢复 `open` 表示。
- 本轮不做自动修复、不做完整事件历史表、不接真实向量库、不做办公室视图。
- 验证完成：后端定向 13 个测试通过；后端全量 65 个测试通过；前端构建通过；生产/开发 Compose config 通过；Git 卫生、密钥扫描和禁止路径扫描通过；Vite preview `http://127.0.0.1:3003/` 返回 200。

R6-UI-1 完成记录：

- 参考 `E:\github\claude-agent-examples\ppt\*.html` 的 AI 技术发布会 / 代码编辑器 / Terminal Deck 风格。
- 主 UI 主题切换为深黑蓝背景、霓虹绿色主强调、紫色和橙色辅助状态。
- 正文字体约束为 IBM Plex Sans，代码、标签、状态、数字、路径和 UID 约束为 JetBrains Mono。
- Element Plus 继续作为组件基础，但通过 `frontend/src/styles/main.css` 统一覆盖为暗色卡片、细边框、Terminal 输入框、mono badge 和暗色表格。
- 已更新 PRD 和技术架构，作为后续前端页面设计规范。
- 验证完成：前端构建通过；`http://127.0.0.1:3000/` 返回 200；Git whitespace 检查通过。

R6-UI-2 当前执行记录：

- [x] 将 Dashboard 从单页堆叠改为左侧菜单工作台，按“模块 -> 功能 -> 页面”拆分为系统概览、文件导入、LifeOS 收集、审核队列、MCP Preview 和知识库体检。
- [x] 本地文件导入入口只要求填写“知识来源地址 sourcePath”，`rawSourcesRoot` 改为后台配置默认值，不再要求用户手工填写归集目标。
- [x] 后端支持将配置中的相对 Raw Sources 路径解析为绝对路径，避免本地 jar 默认配置导致导入接口误报路径非法。
- [x] 当前 Web UI 将 Vector Export / 向量导出移出主流程，改放高级能力区；后续向量库接入确认前，不作为资料整理主线展示。
- [x] `Maintenance 维护巡检` 重新命名和解释为 `知识库体检 Knowledge Health`，当前仅检查空正文、重复正文和长期未归档个人记录。
- [x] 导入任务列表和任务详情状态 badge 增加 pending、running、completed、failed、cancelled 的视觉区分。
- [x] 验证前端构建、后端定向测试、Git whitespace、密钥扫描和禁止路径扫描。
- 验证完成：后端定向 13 个测试通过；前端构建通过；`git diff --check`、密钥扫描和禁止路径扫描通过；`http://127.0.0.1:3000/` 返回 200；重新 package 并重启本地 Core Service 后 `http://127.0.0.1:8080/actuator/health` 返回 `UP`；不带 `rawSourcesRoot` 的导入请求已进入来源路径校验；当前本地未安装 Playwright，未做自动截图检查。

## 近期三轮执行计划

### 第 1 轮：MVP2 发布收束

1. 完成 R0-1 路线图文档。Done
2. 完成 R0-2 版本更新记录 0.04 草案。Done
3. 完成 R0-3 Docker 端到端烟测。Done
4. 输出 R0-4 发布前自检清单。Done
5. 执行 R0-5 提交和推送。Done
6. R0-6 版本标签、GitHub Release 和正式发布定义由用户确认，作为外部发布宣导事项，不阻塞第 2 轮开发。

## 执行记录

### R0-3 Docker 端到端烟测

执行时间：2026-05-23

```text
JobUid: job_20260523_0ae896c3e383
JobStatus: completed
SourceFileUid: file_e6a481e4083449579246366252a410a5
SourceUid: src_f916b6b2d202461a8d49bfa721532290
NoteUid: note_20260523_1cf541982149
Host note path: E:\WikiForgeVault\00_Inbox_收集箱\Sources_来源\roadmap-source-note.md-src_f916b6b2d202461a8d49bfa721532290.md
Preview contains title: true
```

结论：MVP2 从导入任务到 Source Note 写入、预览的主链路通过。PowerShell JSON 输出中中文路径显示为乱码，但宿主机 Vault 内实际目录和文件名为正常中文。

### R0-4 发布前自检清单

执行时间：2026-05-23

输出文件：

```text
docs/current/2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist.md
```

自检结论：

- `git diff --check` 通过。
- Docker Compose 当前 `mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui` 均 healthy。
- R0-3 烟测写入的 Source Note 在 `E:\WikiForgeVault` 中存在。
- 当前 `git status --short` 未发现 `node_modules`、`dist`、`.vite`、`target`、真实 `.env`、`data`、Vault 或 Raw Sources 内容。
- 广义 `.env` 模式会命中 `.env.example`，这是允许提交的配置模板。
- `git check-ignore` 确认本地 `frontend/node_modules`、`frontend/dist`、`backend/**/target`、`data` 和 `.env` 已被 `.gitignore` 忽略。
- `git ls-files --error-unmatch` 确认上述禁止路径未被 Git 跟踪。

结论：R0-4 完成。R0-5 提交和推送已完成；标签 `0.04`、GitHub Release 和正式发布定义仍需用户确认。

### R0-5 提交推送

执行时间：2026-05-23

结果：

```text
Commit: a987780 feat: add obsidian source note mvp2
Remote branch: origin/codex/mvp2-obsidian-source-note
Pull request URL: https://github.com/BonusWang/WikiForge/pull/new/codex/mvp2-obsidian-source-note
```

说明：

- 本次只推送开发分支。
- 未创建标签 `0.04`。
- 未创建 GitHub Release。
- 版本发布定义等待用户确认，但不阻塞后续开发。

### 第 2 轮：MVP2.1 可用性加固

当前分支：`codex/mvp2.1-usability-hardening`

执行规则：版本标签和 GitHub Release 属于对外宣导，由用户确认；内部开发分支、提交推送和下一工作流递进不等待该外部发布事项。

1. 冻结“已写 Note 状态”和“重复写入策略”契约。
2. 后端补充按 `fileUid` 查询最近 Note。
3. 前端展示已写入状态和打开/预览入口。
4. 增加集成测试和回归验证记录。
5. 更新文档和归档。

本轮已完成：

- 新增 Vault 状态查询 API 和 Dashboard 状态面板。
- Source Files 列表展示 Obsidian Note 写入状态。
- 已写入文件默认预览已有 Note，不自动重复写入。
- 后端集成测试覆盖 Vault 状态、最近 Note 查询和列表状态字段。
- 前端构建通过，后端 Maven 测试通过。

### 第 3 轮：MVP3 文档解析

1. 冻结正文存储表。
2. 先实现 Markdown / TXT。
3. 再实现 PDF / Word。
4. Source Note 增加正文摘录。
5. 再考虑 AI 摘要，不提前耦合。

本轮已完成：

- 新增 `source_contents` 正文存储表、领域模型和 MyBatis 仓储。
- Worker 支持 `.md/.txt` UTF-8 文本抽取，Markdown 会去除文件开头 YAML frontmatter。
- Worker 支持 PDFBox 提取 PDF 文本。
- Worker 支持 Apache POI 提取 `.docx` 段落文本。
- Core 内部批量提交 Source File 时可同步保存解析正文。
- Source Note 草案增加 `正文摘录 Content Excerpt`。
- 后端 Maven 多模块测试、前端构建、Docker Compose config 已通过。

下一轮继续：

- R3-1：冻结模型调用契约，支持 DeepSeek / MiniMax / CC Switch 切换。
- R3-2：新增 Agent 运行账本和审核队列最小表。

## 并行开发规则

- 进入 R1 及以后，每个开发切片都要先创建具体 Work Order。
- 涉及 DDL、共享 DTO、错误码、Docker Compose、CI 的任务串行处理。
- Core、Worker、UI 可在契约冻结后并行。
- 主编排 Agent 负责最终集成、验证、文档和归档。

## 当前不可提前做的事

- 不在 MVP2.1 前引入大模型总结。
- 不在 R6-1 导出契约稳定前引入真实向量库和 Hybrid Search。
- 不在 MCP 工具清单冻结前实现 MCP 服务。
- 不在个人记录契约冻结前写账单、邮件、人际关系表。
- 不把 `E:\WikiForgeVault`、Raw Sources、运行数据或 `.env` 提交到 Git。

## 发布与版本建议

| 标签 | 内容 |
| --- | --- |
| `0.04` | MVP2 Obsidian Source Note 闭环 |
| `0.05` | MVP2.1 可用性加固 |
| `0.06` | MVP3 文档解析 |
| `0.07` | MVP4 AI 辅助整理 |
| `0.08` | MVP5 Orchestration 辅助工程 + 轻量 MCP |
| `1.0` | 在线资料 + 个人记录可用版 |
| `2.0` | 向量库 + 知识运行层 |
