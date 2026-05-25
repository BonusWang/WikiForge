# WikiForge MVP0 代码重构执行计划

## 1. 计划定位

本计划是 MVP0 基座进入代码重构阶段的执行清单。

前序文档已经完成需求、架构、前端、后端、数据库、API、Obsidian LLM Wiki 与资源盘点。本计划开始执行代码层落地，但仍遵守“先减后加”原则：

- 不恢复 R/V 历史阶段。
- 不启用多 Agent 并行、workflow 编排或 Orchestration 辅助工程。
- 不新增独立 Gateway 服务。
- 不为向量、MCP、LifeOS、知识体检等延期能力预建入口、API 或表。
- 新增功能、服务、原子能力、API、表结构必须同步维护项目架构强约定。

## 2. 成功标准

MVP0 完成后，系统只呈现并支撑一条主流程：

```text
收纳入口
  -> 路径扫描 / 浏览器上传
  -> 资料仓库规整入库
  -> SourceFile 账本
  -> 正文抽取
  -> Obsidian LLM Wiki 写入
  -> index/log 更新
  -> 前端中文状态展示
```

验收标准：

| 维度 | 标准 |
| --- | --- |
| 前端 | 只保留“收纳 / 资料箱 / Wiki / 日志 / 设置”五入口，不出现高级能力主入口 |
| 后端 | Core 对前端提供 MVP0 API，Worker 只做内部扫描、复制、hash、抽取任务 |
| 数据库 | 主流程只依赖 `import_jobs`、`source_files`、`source_contents`、`wiki_ingest_runs`、`system_dictionaries` |
| 状态 | 用户可见状态统一使用中文码值和字典映射 |
| Obsidian | 只写入 Vault 内 `WikiForge/` 托管目录，资料仓库位于 `WikiForge/30_Resources_资源/` |
| 历史功能 | MCP、向量、LifeOS、知识体检、AI Review、Orchestration 均退出 MVP0 主流程 |

## 3. 执行任务

### Task 0：基线守护

目标：确保重构只基于 MVP0 新节点推进。

改动范围：

- `README.md`
- `WORKFLOW.md`
- `AGENTS.md`
- `docs/current/项目架构强约定-WikiForge-project-architecture-conventions.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/README.md`

执行内容：

- 确认根文档只指向 MVP0 基线和公共规则。
- 确认旧文档只在 `docs/archive/2026-05-24/` 作为参考材料。
- 确认任何新增能力必须先登记到架构台账。

验证命令：

```powershell
git diff --check
```

### Task 1：前端 MVP0 主壳拆分

目标：移除单体 Dashboard 主入口，建立轻应用层五入口结构。

新增目标目录：

```text
frontend/src/
  shell/
  pages/
    capture/
    inbox/
    wiki/
    logs/
    settings/
```

改动文件：

- `frontend/src/App.vue`
- `frontend/src/router/index.ts`
- `frontend/src/shell/AppShell.vue`
- `frontend/src/shell/MainNavigation.vue`
- `frontend/src/pages/capture/CapturePage.vue`
- `frontend/src/pages/inbox/SourceInboxPage.vue`
- `frontend/src/pages/wiki/WikiWorkspacePage.vue`
- `frontend/src/pages/logs/RunLogPage.vue`
- `frontend/src/pages/settings/SettingsPage.vue`

执行内容：

- 新建 AppShell，统一承载导航、顶部状态、主内容区。
- 路由改为五入口页面。
- 旧 `DashboardView.vue` 已退出代码主线并删除，后续迁移参考以资源盘点和归档文档为准。
- 前端文案和用户可见状态全部使用中文。
- 保持现有视觉格式中的布局密度、卡片边距、按钮风格和状态标签风格，但不继承旧大屏 Dashboard 信息结构。

验证命令：

```powershell
npm --prefix frontend run build
```

### Task 2：前端 API 与类型收敛

目标：前端只调用 Core API 的 MVP0 主流程契约。

改动范围：

- `frontend/src/api/`
- `frontend/src/types/`
- `frontend/src/pages/**`

执行内容：

- 保留并收敛路径扫描入口 `POST /api/v1/import-jobs/local`。
- 新增上传入口客户端封装 `POST /api/v1/upload-sources`。
- 新增单文件 Wiki ingest 客户端封装 `POST /api/v1/source-files/{fileUid}/wiki-ingest-runs`。
- 新增字典查询封装，用于中文状态码和样式映射。
- 前端主流程不再引用 MCP、向量导出、LifeOS、知识体检、AI Review、Orchestration API。

验证命令：

```powershell
npm --prefix frontend run build
```

### Task 3：后端最小 API 与 DTO 收敛

目标：Core API 对前端只暴露 MVP0 主流程能力。

改动范围：

- `backend/wikiforge-core-service/src/main/java/**/interfaces/`
- `backend/wikiforge-core-service/src/main/java/**/application/`
- `backend/wikiforge-common/**`

执行内容：

- 保留路径导入入口。
- 增加上传入口规划实现。
- 增加 SourceFile 详情和列表响应的中文状态字段。
- 增加 Wiki ingest run 创建与结果查询接口。
- 增加字典查询接口。
- 删除已明确不进入 MVP0 且无复用价值的历史 Controller / Service / DTO，保留 MVP0 主流程接口。

验证命令：

```powershell
mvn -f backend/pom.xml test
```

### Task 4：数据库最小主流程表落地

目标：让数据库结构与 MVP0 数据设计一致，历史表退出主流程。

改动范围：

- `backend/wikiforge-core-service/src/main/resources/db/migration/`
- `backend/wikiforge-core-service/src/main/java/**/domain/`
- `backend/wikiforge-core-service/src/main/java/**/infrastructure/`

执行内容：

- 确认 `import_jobs`、`source_files`、`source_contents` 字段可承载 MVP0。
- 新增 `wiki_ingest_runs`。
- 新增 `system_dictionaries`，写入中文状态码、中文名称、中文说明和 UI 样式键。
- MVP0 fresh schema 不再创建历史高级能力表；已有本地库如已执行旧迁移，单独重建或迁移。

验证命令：

```powershell
mvn -f backend/pom.xml test
```

### Task 5：Worker 原子能力拆分

目标：把扫描、复制、hash、类型识别、正文抽取拆成独立可测能力。

改动范围：

- `backend/wikiforge-worker-service/src/main/java/`
- `backend/wikiforge-common/src/main/java/`

执行内容：

- 复用 `PathSafety`。
- 复用 `LocalFileScanner`。
- 复用 `TextContentExtractor`。
- 明确 Worker 不直接访问 Core 数据库。
- Worker 只通过内部 API 回写任务进度和文件结果。

验证命令：

```powershell
mvn -f backend/pom.xml test
```

### Task 6：Obsidian LLM Wiki 写入闭环

目标：完成 SourceFile 到 Obsidian Wiki 的自动整理写入。

改动范围：

- `backend/wikiforge-core-service/src/main/java/**/application/`
- `backend/wikiforge-core-service/src/main/java/**/infrastructure/`

执行内容：

- 新建或收敛 Wiki ingest 用例。
- 写入来源页、index、log。
- 使用托管区块，避免覆盖用户手写内容。
- 写入结果记录到 `wiki_ingest_runs`。
- 失败时记录中文失败原因。

验证命令：

```powershell
mvn -f backend/pom.xml test
```

### Task 7：退役入口清理

目标：把历史能力从 MVP0 可见入口、构建路径和默认运行路径中清理出去。

改动范围：

- `frontend/src/router/index.ts`
- `frontend/src/api/`
- `backend/pom.xml`
- `deploy/`
- `orchestration-ui/`
- `backend/wikiforge-orchestration-service/`

执行内容：

- 前端不再暴露高级能力入口。
- 后端主构建默认不再启用 Orchestration 辅助工程。
- Docker Compose 默认不启动 Orchestration。
- MCP、向量、LifeOS、知识体检、AI Review、旧 Wiki Compile、旧 Source Note、Link Source 的代码入口、前端封装、迁移和测试从 MVP0 删除。
- Orchestration 服务源码、独立 UI、Dockerfile、`agentteam/` 工作区和 `.env.example` 旧变量从 MVP0 删除。

验证命令：

```powershell
docker compose config
mvn -f backend/pom.xml test
npm --prefix frontend run build
```

### Task 8：端到端验收

目标：形成用户可验收的 MVP0 闭环。

验收场景：

| 场景 | 预期 |
| --- | --- |
| 本地路径收纳 | 创建 import job，按策略入库资料仓库，生成 SourceFile |
| 浏览器上传 | 上传文件复制进入同一资料仓库流程，不覆盖同名文件 |
| 重复文件 | hash 去重，前端中文提示 |
| 正文抽取 | 生成或更新 SourceContent |
| Wiki ingest | 写入 Obsidian 来源页、index、log |
| 安全边界 | 输入路径不得与资料仓库重叠，Obsidian 写入不得逃逸 Vault |

验证命令：

```powershell
git diff --check
mvn -f backend/pom.xml test
npm --prefix frontend run build
docker compose config
```

## 4. 暂不处理清单

以下内容不进入 MVP0 代码重构主线：

- 真实向量检索。
- Hybrid Search。
- 完整 MCP Server。
- 在线文档 OAuth 抓取。
- LifeOS 个人记录。
- 知识体检 / 维护巡检。
- 多 Agent 团队协作。
- workflow / orchestration 编排。
- 独立 Gateway 服务。

如后续恢复，必须单独出需求、设计、API、数据表、验证方案，并登记到项目架构强约定。

## 5. 当前执行顺序

当前按以下顺序推进：

1. Task 0：基线守护。
2. Task 1：前端 MVP0 主壳拆分。
3. Task 2：前端 API 与类型收敛。
4. Task 3-6：后端、数据库、Worker、Obsidian 闭环。
5. Task 7-8：退役入口清理与端到端验收。

如果执行中发现现有代码与文档设计冲突，以 MVP0 基线文档为准，并同步更新本计划和项目架构强约定。

## 6. 2026-05-24 执行记录

已完成：

- Task 0 基线守护：旧文档物理归档，当前入口指向 MVP0 基座。
- Task 1 前端 MVP0 主壳拆分：五入口已落地，旧 Dashboard 不再作为根路由。
- Task 2 前端 API 与类型收敛：新增上传、资料详情、Wiki ingest、字典封装；设置页保留为五入口之一，不提供持久化设置 API。
- Task 3 后端最小 API 与 DTO 收敛：收纳任务和资料响应补齐中文状态字段。
- Task 4 数据库最小主流程表落地：新增 `system_dictionaries`、`wiki_ingest_runs`。
- Task 6 Obsidian LLM Wiki 写入闭环：Wiki ingest 已写入来源页、index 和 log 的最小规则版本。
- Task 7 退役入口清理：Orchestration 退出默认 Maven、Compose 和 CI 路径。
- Task 8 上传入口验收项：浏览器上传已进入资料仓库，并登记 SourceFile 账本。

仍保留为后续加固：

- 暂无。后续按新需求单独登记，不从历史阶段回灌能力。

## 7. 2026-05-25 执行记录

已完成：

- Task 5 Worker 原子能力继续细拆：新增 `RawSourceFileCollector`、`FileContentHasher`、`FileTypeDetector`，`LocalFileScanner` 保留递归扫描和跳过规则，文件复制/移动/引用登记、hash、类型识别改为独立可测能力；正文抽取继续复用 `TextContentExtractor`。
- Task 8 端到端人工验收：使用本机真实路径 `E:\github\WikiForge\data\imports\mvp0-e2e-clean-20260525-114206`、真实资料仓库 `E:\WikiForgeVault\WikiForge\30_Resources_资源` 和真实 Obsidian Vault `E:\WikiForgeVault` 跑通路径导入、重复文件、浏览器上传、正文抽取、Wiki ingest、index/log 更新和资料仓库重叠拦截。
- 历史高级能力物理清理：删除 AI Review、MCP Preview、Vector Export、LifeOS、Knowledge Maintenance、旧 Wiki Compile、旧 Source Note、Link Source 的后端代码、前端 API/类型、Flyway 迁移和专项集成测试；Obsidian API 收敛为 init/status，Wiki 写入状态改由 `wiki_ingest_runs` 驱动。
- Orchestration 辅助工程物理清理：删除 `backend/wikiforge-orchestration-service/`、`orchestration-ui/`、`deploy/docker/orchestration-*.Dockerfile`、`agentteam/`、Orchestration 错误码和 `.env.example` 中的旧辅助工程/向量/模型变量。
- 历史 `sources` 账本物理清理：fresh schema 不再创建 `sources`，`source_files` 成为唯一资料文件账本，`source_contents` 只通过 `source_file_id` 关联正文抽取结果。
- 设置持久化预建能力清理：删除未接通的 `/settings` 前端 API 封装、`system_settings` / `model_providers` 预建迁移；设置页保留为五入口之一，但 MVP0 不提供持久化设置 API。
- 当前能力参数清理：删除尚无执行语义的 Wiki 写入请求参数、上传写回模式和路径收纳意图参数暴露口径；Wiki ingest 当前固定为来源页、index、log 规则式写入。
- 当前能力状态清理：删除未产生的备用 Wiki 写入状态和原因字段；规则式写入成功记为“已写入”，失败统一记录 `failureReason`。

验证命令：

```powershell
git diff --check
mvn -f backend/pom.xml test
npm --prefix frontend run build
docker compose -f deploy/docker-compose.yml config
```
