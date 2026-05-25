# 2026-05-24 WikiForge MVP0 四层架构设计 MVP0 Four-Layer Architecture

## 1. 设计目标

本设计将 WikiForge 从“功能堆叠的知识运行平台”收敛为 MVP0 的“个人私有知识库收纳与 LLM Wiki 自动整理工具”。

架构必须同时满足两件事：

- 产品上按四层能力拆分，让用户和后续开发者看清主流程。
- 工程上保留 DDD 分层约束，避免把四层业务概念机械复制成混乱包结构。

因此，代码仍遵守：

```text
interfaces -> application -> domain <- infrastructure
```

四层架构用于定义能力地图、任务流、UI 信息架构和原子能力边界。

## 2. 四层总览

| 层级 | 职责 | 前端表现 | 后端归属 |
| --- | --- | --- | --- |
| 轻应用层 | 面向用户的场景入口 | Capture、Inbox、Wiki、Log、Settings | REST Controller、DTO |
| 决策层 | 规则、策略、AI 判断 | 策略说明、状态解释、重跑入口 | Application Service / Policy |
| 指令执行层 | 编排任务和状态流转 | ImportJob、UploadJob、WikiIngestRun | Job Service、Runner、状态机 |
| 原子能力层 | 最小可复用能力 | 不直接暴露或只作为诊断展示 | Scanner、Copier、Hasher、Extractor、Writer |

## 3. 前端架构

### 3.1 目标目录

```text
frontend/src/
  shell/
    AppShell.vue
    MainNavigation.vue
  pages/
    capture/
      CapturePage.vue
    inbox/
      SourceInboxPage.vue
    wiki/
      WikiWorkspacePage.vue
    logs/
      RunLogPage.vue
    settings/
      SettingsPage.vue
  features/
    capture/
    source-inbox/
    llm-wiki/
    obsidian-vault/
    runtime-log/
  api/
  types/
  styles/
```

### 3.2 页面职责

- Capture：路径扫描、文件上传、任务创建。
- Source Inbox：导入任务、SourceFile 列表、重复文件、解析状态。
- Wiki Workspace：Wiki 写入结果、Source page、index/log、重跑入口。
- Run Log：ImportJob、WikiIngestRun、index/log 更新记录。
- Settings：MVP0 运行口径、Raw Sources、Obsidian Vault 托管目录、路径安全提示。

### 3.3 单页拆分原则

旧 `DashboardView.vue` 已退出代码主线并删除。后续如需追溯旧交互，只读取归档和资源盘点，不再恢复单体页面。

- Shell 和路由作为前端基座。
- 页面逻辑按五入口分别维护。
- 不恢复大 Dashboard 或混合功能单页。

## 4. 后端架构

### 4.1 Core Service

Core Service 仍是 MVP 对外业务 API 入口，负责：

- Source / SourceFile / SourceContent / ImportJob 账本。
- 上传入口。
- Wiki ingest 编排。
- Obsidian Vault 写入结果登记。

后续目标包边界：

```text
com.wikiforge.core
  interfaces.web.capture
  interfaces.web.wiki
  application.capture
  application.wiki
  domain.source
  domain.wiki
  domain.job
  infrastructure.persistence.source
  infrastructure.filesystem.obsidian
```

### 4.2 Worker Service

Worker Service 负责耗时文件任务，不承载 UI 查询：

- 本机路径扫描。
- Raw Sources 复制。
- hash 和文件类型识别。
- 文本抽取。
- 将结果回写 Core。

后续目标原子能力：

```text
worker.application.ingest.LocalImportJobRunner
worker.application.upload.UploadSourceRunner
worker.domain.scan.FileScanPolicy
worker.infrastructure.filesystem.LocalDirectoryScanner
worker.infrastructure.filesystem.RawSourceCopier
worker.infrastructure.filesystem.ContentHasher
worker.infrastructure.extractor.TextContentExtractor
```

### 4.3 非 MVP0 能力处理

以下能力不进入 MVP0 主流程：

- `McpPreviewService`：退役，MVP0 代码已删除。
- `VectorExportService`：退役，MVP0 代码已删除。
- `PersonalRecordService`：退役，MVP0 代码已删除。
- `KnowledgeMaintenanceService`：退役，MVP0 代码已删除。
- `AiReviewService`：退役，MVP0 代码已删除。
- `WikiCompileService` / 旧 Source Note / Link Source：退役，MVP0 代码已删除。
- `wikiforge-orchestration-service`：退役，源码已删除。
- `orchestration-ui`：退役，源码已删除。

退役能力后续必须重新出需求、API、表设计和验证方案，不能直接回到主流程。

## 5. 数据流

### 5.1 路径扫描流

```text
UI Capture
  -> Core: create local import job
  -> Worker: scan directory
  -> Worker atoms: scan / copy / hash / classify / extract
  -> Core: submit SourceFile batch
  -> Core: create Wiki ingest run
  -> Obsidian: write source/index/log
  -> UI: show status
```

### 5.2 上传流

```text
UI Capture
  -> Core: upload multipart file
  -> Core/Worker: store into Raw Sources
  -> Core: register SourceFile
  -> Core: create Wiki ingest run
  -> Obsidian: write source/index/log
```

### 5.3 LLM Wiki 写入流

```text
SourceFile + SourceContent
  -> Schema rules
  -> Rule-based managed block
  -> Source page
  -> index.md
  -> log.md
  -> WikiIngestRun result
```

Obsidian 写入只进入 Vault 内 `WikiForge/` 托管目录。来源页、`index.md` 和 `log.md` 的目录结构以 Obsidian LLM Wiki 设计文档为准。

## 6. API 规划

### 6.1 保留收敛

`POST /api/v1/import-jobs/local`

用途：路径扫描入口。

规划补充字段：

- `rawSourcesRoot`: 可选 Raw Sources 根目录；为空时使用运行配置
- `organizeMode`: 当前仅支持 `copy`

### 6.2 已接入主流程

`POST /api/v1/upload-sources`

用途：浏览器上传文件进入 Raw Sources 和 Wiki ingest 主流程。

`POST /api/v1/source-files/{fileUid}/wiki-ingest-runs`

用途：对单个 SourceFile 按 LLM Wiki 规则自动整理或重跑。

### 6.3 结果字段

Wiki ingest 运行结果至少包含：

- `sourcePagePath`
- `indexUpdated`
- `logEntryAppended`
- `writeStatus`
- `failureReason`

## 7. 数据模型原则

MVP0 数据库以最小可用为目标，不继承历史阶段的完整表集合。

### 7.1 MVP0 最小表集合

| 表 | 归属 | 用途 |
| --- | --- | --- |
| `import_jobs` | Core / capture | 路径扫描和上传任务账本 |
| `source_files` | Core / source | Raw Sources 文件账本、hash、类型、状态 |
| `source_contents` | Core / source | 文本抽取结果和抽取状态 |
| `wiki_ingest_runs` | Core / wiki | Obsidian LLM Wiki 写入运行结果 |
| `system_dictionaries` | Core / dictionary | 中文状态码、中文说明和颜色映射 |

### 7.2 暂不进入 MVP0 的表

- `agent_runs`、`agent_steps`、`review_items` 不再复用为 Wiki ingest，迁移文件已从 MVP0 移除。
- `obsidian_notes`、`wiki_pages`、`wiki_integrations` 不进入 MVP0 新库。
- `mcp_tool_calls`、`personal_records`、`vector_export_jobs`、`content_chunks`、`knowledge_maintenance_*` 不进入 MVP0 新库。
- Orchestration 相关配置退役，服务源码、独立 UI、Dockerfile 和 `agentteam/` 已删除。

### 7.3 建表规则

- 不为未来能力提前建表。
- 新表必须先说明所属四层能力、归属服务和生命周期。
- Worker 不直接暴露用户查询表，只回写 Core 需要的结果。
- MVP0 fresh schema 不创建历史高级能力表；已有本地库如已执行旧迁移，需单独重建或迁移。
- 用户可见状态必须由 `system_dictionaries` 映射为中文码值和中文说明。

## 8. 安全规则

- 输入路径与 Raw Sources 不得重叠。
- 上传文件不得覆盖已有文件。
- 文件名必须安全化。
- Obsidian 写入必须限制在 Vault 内。
- Obsidian 写入必须限制在 Vault 内 `WikiForge/` 托管目录。
- API 不返回非必要宿主机绝对路径。
- AI 失败时回退到规则式 Markdown，不阻断收纳。
