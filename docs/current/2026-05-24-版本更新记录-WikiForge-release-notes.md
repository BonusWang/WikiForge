# 2026-05-24 WikiForge 版本更新记录 Release Notes

## 1.0-v1-preview.1 - V1 在线资料与个人记录

发布日期：2026-05-24

本版本目标是把 WikiForge 从本地文件整理扩展到个人 LifeOS 收集入口，支持链接资料、个人记录和 Obsidian 归档的最小可用闭环。

### 更新内容

- 新增链接类 Source 创建入口：`POST /api/v1/link-sources`。
- 新增个人记录 REST API：创建、列表、详情、汇总、写入 Obsidian。
- `personal_records` 增加 `obsidian_vault_path`、`obsidian_uri`、`archived_at`。
- 新增个人记录写入 Obsidian Vault 的 Markdown 模板，路径为 `00_Inbox_收集箱/Personal_个人记录`。
- Web UI 新增 LifeOS 操作区，支持链接资料和个人记录录入、汇总、归档。
- V1 文档、开发者日志和归档索引更新到 2026-05-24。

### 验证结果

- 后端全量 Maven 测试通过：5 个模块合计 55 个测试，0 失败。
- 前端构建通过。
- Docker Compose 生产与开发配置通过。
- `git diff --check`：通过。
- 密钥和禁止路径扫描：通过，未发现真实 token、`node_modules`、`dist`、`target`、Vault 或 Raw Sources 被跟踪。

### 版本边界

本版本完成 V1 首版收集和归档闭环。真实飞书 / 腾讯文档授权读取、个人记录周期总结、向量库导出和 Agent 定时维护进入 V1.x / V2。

## 0.08-preview.4 - MVP 审核加固

发布日期：2026-05-23

本版本是 `0.08-preview.3` 发布后的审核加固版本，重点处理 MVP 无登录鉴权前提下的 Docker 默认暴露边界、旧分支配置漂移和文档状态同步问题。

### 更新内容

- Compose 生产与开发文件的端口映射默认改为仅绑定 `127.0.0.1`：
  - MySQL `3306`
  - Core Service `8080`
  - Worker Service `8081`
  - Orchestration Service `8090`
  - WikiForge UI `3000`
  - Orchestration UI `3001`
- `.env.example` 新增 `WIKIFORGE_PORT_BIND=127.0.0.1`。
- `.env.example` 和 Orchestration Compose 默认 `WIKIFORGE_ACTIVE_BRANCH` 改为 `main`。
- OpenClaw / Hermes 接入说明补充：
  - 默认本机绑定。
  - Docker 外部容器访问宿主机端口的限制。
  - 同一 Compose 网络内调用优先。
  - 如需对外开放，必须显式设置 `WIKIFORGE_PORT_BIND=0.0.0.0` 并配置访问控制。
- 新增 MVP 审核报告：
  - `docs/current/2026-05-23-MVP审核报告-WikiForge-mvp-audit-report.md`
- Roadmap、MCP 契约、开发者日志和归档索引同步到 R4-6 审核加固状态。

### 验证结果

- `git diff --check`：通过。
- `docker compose -f deploy/docker-compose.yml config --quiet`：通过。
- `docker compose -f deploy/docker-compose.dev.yml config --quiet`：通过。
- Compose 渲染检查：端口均默认绑定 `127.0.0.1`，Orchestration 当前分支默认值为 `main`。
- 后端 Maven 全量测试：通过，5 个模块合计 52 个测试，0 失败。
- `npm --prefix frontend run build`：通过，保留既有 Rollup 大 chunk / PURE 注释 warning。
- `npm --prefix orchestration-ui run build`：通过。
- 敏感信息扫描：未发现真实 MiniMax 密钥前缀、Bearer token 或常见 API Key 模式写入仓库。
- 禁止路径扫描：未发现 `node_modules`、`dist`、`target`、真实 `.env`、`data`、Vault 或 Raw Sources 被 Git 跟踪。

### 版本边界

这是 MVP5 HTTP Preview 的审核加固版本，不新增业务功能。

尚未实现：

- 正式登录鉴权和多用户权限。
- 完整 MCP stdio / SSE transport。
- V1 在线资料连接器。
- Agent 办公室视图。
- 向量库导出和混合检索。

## 0.08-preview.3 - R4-5 MCP 调用看板与 OpenClaw / Hermes 接入

发布日期：2026-05-23

本版本完成 MVP5 / R4-5：Web UI 可以只读查看 MCP 工具和调用日志，OpenClaw / Hermes 可以按本机接入说明通过 HTTP Preview 调用 WikiForge。

### 更新内容

- Dashboard 新增 `MCP Preview` 看板：
  - MCP tools 清单。
  - MCP calls 调用日志。
  - tool / status / callerType 过滤。
  - 调用状态、调用方、耗时和错误码展示。
- 前端新增 MCP API 封装：
  - `frontend/src/api/mcp/index.ts`
  - `frontend/src/types/mcp.ts`
- 后端补充 `GET /api/v1/mcp/calls` 查询端点集成测试。
- MCP calls 查询 `pageSize` 上限对齐为 100。
- 首版 UI 不开放真实“调用工具”按钮，避免误触发 `create_source` 或 `create_personal_record` 写入业务数据。
- 新增 OpenClaw / Hermes 接入说明：
  - `docs/current/2026-05-23-OpenClaw-Hermes接入说明-WikiForge-openclaw-hermes-mcp-integration.md`
  - 覆盖本机、Docker 外部容器、Compose 网络内三种 Base URL。
  - 提供 tools、calls、create_source、create_personal_record、search_sources、get_source、get_obsidian_note 的 PowerShell 示例。
- R4-5 Agent Team 状态、Roadmap、开发者日志、MCP 契约和归档索引已同步到完成态。

### 验证结果

- 后端 Maven 全量测试：通过，5 个模块合计 52 个测试，0 失败。
- `McpPreviewApiIntegrationTests`：8 个测试通过，0 失败。
- 前端 `npm --prefix frontend run build`：通过，保留既有 Rollup 大 chunk / PURE 注释 warning。
- `docker compose -f deploy/docker-compose.yml config --quiet`：通过。
- `docker compose -f deploy/docker-compose.dev.yml config --quiet`：通过。
- `git diff --check`：通过。
- 真实 MiniMax 密钥前缀和常见 Bearer/API Key 模式扫描：未发现写入仓库。
- 禁止路径检查：未发现 `node_modules`、`dist`、`target`、真实 `.env`、`data`、Vault 或 Raw Sources 被 Git 跟踪。

### 版本边界

这是 `0.08` 的第三个阶段性预览版本，也是 MVP5 当前 HTTP Preview 主线的完成版本。

尚未实现：

- 完整 MCP stdio / SSE transport。
- MCP Client / Marketplace。
- 多用户权限系统。
- Agent 办公室视图。
- 向量库导出和混合检索。

后续阶段进入 V1：在线资料连接器、个人记录持续整理、LifeOS 化记录和后续向量库准备。

## 0.08-preview.2 - R4-4 MCP Obsidian Note 与 Personal Record 预览

发布日期：2026-05-23

本版本在 `0.08-preview.1` 的 MCP HTTP Preview 基础上，完成 R4-4：外部 Agent 可以读取已登记 Obsidian Note，并写入个人记录草案。

### 更新内容

- 启用 MCP tools：
  - `get_obsidian_note`
  - `create_personal_record`
- `get_obsidian_note` 支持按已登记 `noteUid` 读取 Vault 相对路径、Obsidian URI、标题、状态和可选 Markdown。
- `get_obsidian_note` 返回前校验 `vaultPath` 必须是安全相对路径，即使 `includeMarkdown=false` 也不能暴露本机绝对路径。
- Markdown 读取前校验最终真实路径仍位于配置的 Obsidian Vault 根目录内，阻断路径穿越和符号链接逃逸。
- `create_personal_record` 支持 `expense`、`bill`、`email`、`relationship`、`event`、`note` 类型。
- 个人记录写入 `personal_records`，初始状态为 `pending`，本轮不触发 AI 总结、不自动写 Obsidian。
- MCP 调用日志继续写入 `mcp_tool_calls`，并对 `rawContent`、`structured`、`markdown` 做脱敏。
- 更新 Roadmap、MCP 契约、技术架构、数据模型、开发者日志和归档快照到 R4-4 完成态。
- 发布规则已调整：后续版本标签和 GitHub Release 不再等待用户确认，由 Agent 验证通过后直接发布。
- 新增 `agentteam/` 协作目录，R4-5 起采用“主 Agent + 前端 / 后端 / 测试专业 Agent”的文件夹式协作空间。
- R4-5 前端侦察结论已记录：首版优先展示 MCP tools 和 calls 日志，默认不开放真实工具调用。

### 验证结果

- 后端 Maven 多模块测试：通过。
- `McpPreviewApiIntegrationTests`：7 个测试通过，0 失败。
- 前端 `npm run build`：通过，保留既有 Rollup 大 chunk / PURE 注释 warning。
- `docker compose -f deploy/docker-compose.yml config --quiet`：通过。
- `docker compose -f deploy/docker-compose.dev.yml config --quiet`：通过。
- `git diff --check`：通过。
- 禁止路径检查：未发现 `node_modules`、`dist`、`target`、真实 `.env`、`data`、Vault 或 Raw Sources 进入 Git。
- 真实 MiniMax 密钥前缀扫描：未发现写入仓库。

### 版本边界

这是 `0.08` 的第二个阶段性预览版本，不是完整 MVP5 最终版。

尚未实现：

- MCP 调用日志在 Dashboard 中的完整展示。
- OpenClaw / Hermes 本机接入说明和样例。
- 完整 MCP transport、MCP Client、Marketplace、多用户权限系统。
- Agent 办公室视图和向量库导出。

下一阶段继续 R4-5：展示 MCP 调用日志，并补充 OpenClaw / Hermes 本机接入说明。

## 0.08-preview.1 - MVP5 Orchestration + MCP Preview 阶段预览

发布日期：2026-05-23

本版本是 WikiForge MVP5 的阶段性预览发布，核心目标是把长期开发编排辅助工程和轻量 MCP HTTP Preview 的基础能力交付出来，便于后续 OpenClaw、Hermes、Codex 或其他 Agent 接入调试。

### 更新内容

- 新增 WikiForge Orchestration 辅助工程：
  - `wikiforge-orchestration-service`
  - `orchestration-ui`
  - Orchestration Dockerfile、Compose 配置和 CI 构建入口
- 新增轻量 Symphony-inspired 工作流：
  - `WORKFLOW.md`
  - WikiForge Agent Task Issue 模板
  - 项目内 AI 开发 Skill 和多 Agent 协作规则
- 新增 MCP HTTP Preview 契约文档：
  - `GET /api/v1/mcp/tools`
  - `POST /api/v1/mcp/tools/{toolName}/call`
  - `GET /api/v1/mcp/calls`
- 新增 MCP Preview 数据表：
  - `mcp_tool_calls`
  - `personal_records`
- 已实现并启用 Source 类 MCP tools：
  - `search_sources`
  - `get_source`
  - `create_source`
- `get_obsidian_note` 和 `create_personal_record` 已进入工具清单，但在本预览版本中保持 disabled，进入下一开发节点 R4-4。
- MCP 调用日志已落库，`rawContent`、`markdown`、`structured` 等敏感字段不写入调用日志原文。
- 文档、路线图、开发者日志和归档快照已推进到 R4-3 完成态，当前下一节点为 R4-4。

### 验证结果

- 后端 Maven 多模块测试：通过。
- `McpPreviewApiIntegrationTests`：通过。
- `git diff --check`：通过。
- 真实 MiniMax 密钥前缀扫描：未发现写入仓库。
- `docker compose -f deploy/docker-compose.yml config --quiet`：通过。
- `docker compose -f deploy/docker-compose.dev.yml config --quiet`：通过。
- 暂存区禁止路径检查：未发现 `node_modules`、`dist`、`target`、`.env`、`data` 等禁止提交内容。

### 版本边界

这是 `0.08` 的阶段性预览版本，不是完整 MVP5 最终版。

尚未实现：

- `get_obsidian_note` 实际读取工具。
- `create_personal_record` 实际写入工具。
- MCP 接入说明和 OpenClaw / Hermes 本机配置样例。
- 完整 MCP transport、MCP Client、Marketplace、多用户权限系统。
- Agent 办公室视图和向量库导出。

下一阶段继续 R4-4：实现 Obsidian Note 读取和 Personal Record 写入。

## 0.04 - MVP2 Obsidian Source Note 闭环

发布日期：待发布

本版本目标是把 MVP1 已整理出的 Source File 沉淀为可读、可编辑、可打开的 Obsidian Source Note，完成“整理后归档”的第一条可用链路。

### 更新内容

- 新增 Core Service Obsidian API：
  - `POST /api/v1/obsidian/init`
  - `POST /api/v1/source-files/{fileUid}/obsidian-note/draft`
  - `POST /api/v1/source-files/{fileUid}/obsidian-note/write`
  - `GET /api/v1/obsidian/notes/{noteUid}/preview`
- 新增 `obsidian_notes` 表，用于记录 Source / Source File 与 Obsidian Markdown 文件之间的映射。
- 新增 Source Note Markdown 模板，包含 frontmatter、Source UID、Source File UID、原始路径、归档路径、hash 和后续处理占位。
- 写入 Vault 时采用服务端路径校验、Vault 内相对路径解析、临时文件写入和原子 rename。
- 生成 `obsidian://open` URI，并对 Vault 名和 Vault 内路径进行 URL encode。
- Web UI Dashboard 新增：
  - `初始化 Vault`
  - Source Files 表格 `Source Note` 操作
  - Source Note Markdown 编辑抽屉
  - 写入 Vault、读取预览、打开 Obsidian
- Docker Compose 支持通过 `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH` 将宿主机 Vault 挂载到容器内 `/data/wikiforge/obsidian-vault`。

### 验证结果

- 后端 Maven 多模块测试：通过。
- 前端 `npm run build`：通过，有 Vite / Rollup 非阻塞 warning。
- `docker compose -f deploy/docker-compose.yml config`：通过。
- Docker 镜像构建与 Compose 启动：通过。
- 容器健康检查：`mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui` 全部 healthy。
- `POST /api/v1/obsidian/init`：通过。
- Docker 端到端 Source Note 烟测：通过。
- 浏览器检查 `http://localhost:3000`：MVP2、初始化 Vault、Source Note 可见，console error 为空。
- 发布前自检清单：通过，未发现编译产物、本地 `.env`、运行数据或 Vault 内容被 Git 跟踪；本地已存在的 `node_modules`、`dist`、`target`、`data` 由 `.gitignore` 忽略。
- GitHub Actions CI：待推送后以远程 CI 结果为准。

端到端烟测结果：

```text
JobUid: job_20260523_0ae896c3e383
JobStatus: completed
SourceFileUid: file_e6a481e4083449579246366252a410a5
NoteUid: note_20260523_1cf541982149
Host note path: E:\WikiForgeVault\00_Inbox_收集箱\Sources_来源\roadmap-source-note.md-src_f916b6b2d202461a8d49bfa721532290.md
Preview contains title: true
```

### 版本边界

本版本完成的是 MVP2 的“Source Note 归档”闭环。

尚未实现：

- 文档正文解析。
- AI 摘要、分类、标签生成。
- 已写 Note 的列表化管理和重复写入策略 UI。
- MCP 服务。
- 向量库导入。
- 飞书 / 腾讯文档 / 邮件 / 账单 / 人际关系等连接器。

下一阶段建议进入 MVP2.1：补齐已写 Note 状态、重复写入策略、Vault 状态面板和一条命令式端到端验收脚本。

## 0.03 - MVP1 本地源文件归集整理闭环

发布日期：2026-05-23

本版本是 WikiForge 从工程骨架进入 MVP1 业务闭环的第一个版本，核心目标是把“指定本地路径 -> 扫描文件 -> 归集到 Raw Sources -> 写入 MySQL 索引 -> Web UI 查看状态”跑通。

### 更新内容

- 完成 Core / Worker / UI 的本地源文件导入链路：
  - UI 创建本地导入任务。
  - Core 校验路径、创建 `import_jobs` 并派发 Worker。
  - Worker 扫描本地目录、按类型复制到 Raw Sources。
  - Worker 回调 Core 更新任务状态并提交 `source_files` 明细。
- 新增 MVP1 数据表：
  - `import_jobs`
  - `sources`
  - `source_files`
- 支持本地路径安全规则：
  - 限制扫描根目录。
  - 校验 Raw Sources 根目录必须与配置一致。
  - 禁止输入目录与 Raw Sources 目录重叠。
  - MVP1 默认不跟随 symlink / junction。
- 支持基础文件归类：
  - `01_Documents_文档`
  - `02_Images_图片`
  - `03_PDFs_PDF`
  - `90_Unknown_待确认`
- 支持按内容 hash 识别单次导入中的重复文件，重复文件不重复复制，记录为 `duplicate`。
- 修复本地与 Docker 烟测中发现的问题：
  - MySQL 8 `recursive` 保留字导致 Flyway migration 失败，数据库字段改为 `recursive_scan`。
  - MyBatis Plus 自动别名触发 MySQL 保留字问题，持久化实体改为 `recursiveScan`。
  - Worker 默认 HTTP request factory 不支持 `PATCH` 回调，改为 `JdkClientHttpRequestFactory`。
  - Core 服务重启后 `jobUid` 自增序号碰撞，改为 `job_yyyyMMdd_<12位uuid>`。
  - UI 容器 healthcheck 使用 `127.0.0.1`，避免容器内 `localhost` IPv6 解析导致误判 unhealthy。
- 补充 MVP1 契约文档、开发者日志和归档索引。

### 验证结果

- 后端 Maven 多模块测试：通过。
- 后端 Maven 打包：通过。
- 前端 `npm run build`：通过，有 Vite / Rollup 非阻塞 warning。
- `docker compose -f deploy/docker-compose.yml config`：通过。
- `docker compose -f deploy/docker-compose.dev.yml config`：通过。
- Docker 镜像构建：通过。
- Docker Compose 启动：通过。
- 容器健康检查：`mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui` 全部 healthy。
- 容器级端到端导入：通过。

容器级端到端验收结果：

```text
Entry: http://localhost:3000/api/v1/import-jobs/local
InputPath: /data/wikiforge/imports/test-input
RawSourcesRoot: /data/wikiforge/raw-sources
Status: completed
TotalCount: 5
SuccessCount: 4
SkippedCount: 0
FailedCount: 0
SourceFileTotal: 5
```

### 版本边界

本版本完成的是 MVP1 的“源文件收集整理”闭环，不代表完整知识库已经完成。

尚未实现：

- 文档正文解析和内容抽取。
- Source Note Markdown 草案生成。
- Obsidian Vault 自动写入。
- 在线文档连接器。
- MCP 服务。
- 向量库导入。
- 多 Agent 知识提炼流水线。

下一阶段建议进入 MVP1.1：在已归集的 `source_files` 基础上，选择少量 Markdown / Word / PDF 样例，生成可人工审核的 Obsidian Source Note 草案。

## 0.02 - MVP0 工程骨架与 Agent 协作基线

发布日期：2026-05-23

本版本是 WikiForge 从需求和架构文档阶段进入可开发工程基线阶段的第一个小版本。

### 更新内容

- 完成 MVP0 少服务微服务工程骨架：
  - `wikiforge-common`
  - `wikiforge-core-service`
  - `wikiforge-worker-service`
  - `wikiforge-ui`
- 建立 Java Maven monorepo 后端结构，保留 Core / Worker 服务边界。
- 新增 Core Service 健康检查、Flyway MVP0 初始化 migration、MyBatis-Plus 基础配置。
- 新增 Worker Service 健康检查骨架，为后续文件扫描和整理任务预留服务入口。
- 新增 Vue 3 + Vite + TypeScript 前端骨架和独立 UI 看板入口。
- 新增 Docker Compose 发布结构：
  - MySQL
  - Core Service
  - Worker Service
  - UI
- 新增 GitHub Actions CI：
  - 后端多模块测试与打包
  - 前端构建
  - Docker 镜像构建校验
- 补充 WikiForge 项目内 AI 开发 Skill，约束后续架构、代码、CI/CD、Docker、Agent、MCP 和多人协作开发。
- 补充并行开发规则：
  - 主编排 Agent 负责任务拆解、专家选择、文件边界和最终集成。
  - 高冲突文件串行修改。
  - 子 Agent 输出 Handoff Packet。
- 补充 Git 提交规则，明确 `node_modules/`、`dist/`、`.vite/`、`target/`、`.env`、运行数据和本地知识库数据不提交。

### 验证结果

- 后端 Maven 多模块测试：通过。
- 后端 Maven 打包：通过。
- 前端 `npm run build`：通过，有 Vite / Rollup 非阻塞 warning。
- `docker compose -f deploy/docker-compose.yml config`：通过。
- `docker compose -f deploy/docker-compose.dev.yml config`：通过。
- Docker 镜像实构建：受本机 Docker Desktop Linux engine 未启动影响，暂未完成。

### 版本边界

本版本仍属于 MVP0 工程基线，不包含 MVP1 业务闭环。

尚未实现：

- 本地路径扫描。
- Raw Sources 归集复制。
- 文件解析。
- Source Note 草案生成。
- Obsidian Vault 写入。
- MCP 服务。
- 向量库导入。

下一阶段应先冻结 MVP1 API、DTO、DDL、状态枚举和路径安全策略，再进入文件收集整理闭环开发。
