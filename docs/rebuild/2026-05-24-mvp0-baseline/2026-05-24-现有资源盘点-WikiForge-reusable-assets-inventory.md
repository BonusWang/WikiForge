# 2026-05-24 WikiForge MVP0 现有资源盘点 MVP0 Reusable Assets Inventory

## 1. 盘点口径

本文件用于 MVP0 重新开始前的资源留存和物理迁移规划。

状态定义：

| 状态 | 含义 |
| --- | --- |
| 复用 | 可直接复用，后续只调整包路径或少量接口 |
| 适配 | 逻辑有价值，但需要拆分、改名或重塑职责 |
| 冻结 | 已实现但退出主流程，后续单独规划 |
| 退役 | 不作为新架构目标，迁移完成后可删除 |

本轮只形成清单和目标路径，不移动代码。

## 2. 后端资源

| 资源 | 当前路径 | 状态 | 迁移目标 | 说明 |
| --- | --- | --- | --- | --- |
| 路径安全 | `wikiforge-common/.../PathSafety.java` | 复用 | `common.filesystem` 保留 | 路径绝对化和重叠校验可直接复用 |
| 错误码体系 | `wikiforge-common/.../ErrorCode.java` | 适配 | `common.error` 保留并补充上传/Wiki ingest 错误码 | 现有错误码可扩展，不重建 |
| API 包装 | `ApiResponse` / `PageResult` | 复用 | `common.web` 或现路径 | REST 统一响应继续使用 |
| ImportJob 账本 | `ImportJobService`、Repository、DTO | 适配 | `core.application.capture` / `domain.job` | 保留路径导入能力，拆出状态机和 Worker 调度 |
| SourceFile 账本 | `SourceFileRepository`、Entity、DTO | 适配 | `domain.source` / `infrastructure.persistence.source` | 作为收纳主账本继续保留 |
| LocalFileScanner | `worker.infrastructure.filesystem.LocalFileScanner` | 适配 | `LocalDirectoryScanner` + `RawSourceCopier` + `ContentHasher` | 当前类混合扫描、复制、hash、分类，需要拆原子能力 |
| LocalImportJobRunner | `worker.application.service.LocalImportJobRunner` | 适配 | `worker.application.ingest.LocalImportJobRunner` | 保留编排骨架，拆出原子依赖 |
| TextContentExtractor | `worker.application.service.TextContentExtractor` | 复用 | `worker.infrastructure.extractor.TextContentExtractor` | Markdown/TXT/PDF/DOCX 抽取可复用 |
| ObsidianVaultService | `core.application.service.ObsidianVaultService` | 适配 | `core.application.wiki` + `infrastructure.filesystem.obsidian` | 路径校验、原子写入和 URI 生成可复用，写入范围改为 `WikiForge/` 托管目录 |
| WikiCompileService | `core.application.service.WikiCompileService` | 适配 | `core.application.wiki.WikiIngestService` | 自动写入和 Wiki 页面账本可复用，语义需从 Compile 改为 Ingest |
| AiReviewService | `core.application.service.AiReviewService` | 冻结 | 无 MVP0 迁移目标 | 旧审核流退出主流程 |
| LinkSourceService | `core.application.service.LinkSourceService` | 冻结 | 后续连接器拓展文档 | 链接资料入口后续再并入统一入口 |
| PersonalRecordService | `core.application.service.PersonalRecordService` | 冻结 | 后续 LifeOS 原子能力文档 | 不进入本轮主线 |
| McpPreviewService | `core.application.service.McpPreviewService` | 冻结 | 后续 MCP 原子能力文档 | 保留实现，不展示主入口 |
| VectorExportService | `core.application.service.VectorExportService` | 冻结 | 后续 Vector 原子能力文档 | 后续真实向量库确认后再处理 |
| KnowledgeMaintenanceService | `core.application.service.KnowledgeMaintenanceService` | 冻结 | 后续 Maintenance 原子能力文档 | 体检保留，不作为第一版主流程 |
| Orchestration Service | `backend/wikiforge-orchestration-service` | 退役 | 后续清理出构建和文档入口 | 辅助开发工程多余，不承载知识库业务 |

## 3. 前端资源

| 资源 | 当前路径 | 状态 | 迁移目标 | 说明 |
| --- | --- | --- | --- | --- |
| Dashboard 单页 | `frontend/src/views/DashboardView.vue` | 已退役 | 已迁移到 `shell/`、`pages/`、`features/` 并删除 | 不再作为代码资产保留，追溯时读取归档 |
| Import Jobs API | `frontend/src/api/import-jobs` | 适配 | `features/capture/api` 或保留 `api/import-jobs` | 路径导入继续使用 |
| Obsidian API | `frontend/src/api/obsidian` | 适配 | `features/llm-wiki/api` | 预览/打开能力复用，模板语义调整 |
| Wiki API | `frontend/src/api/wiki` | 适配 | `features/llm-wiki/api` | 从 compile 调整到 ingest |
| Review API | `frontend/src/api/review` | 冻结 | 无 MVP0 UI 入口 | 旧人工审核流冻结 |
| MCP API | `frontend/src/api/mcp` | 冻结 | 无 MVP0 UI 入口 | 不在主流程加载 |
| Vector API | `frontend/src/api/vector-exports` | 冻结 | 无 MVP0 UI 入口 | 不在主流程加载 |
| LifeOS API | `frontend/src/api/lifeos` | 冻结 | 后续 LifeOS 文档 | 不在主流程加载 |
| Knowledge Maintenance API | `frontend/src/api/knowledge-maintenance` | 冻结 | 无 MVP0 UI 入口 | 不在主流程加载 |
| Orchestration UI | `orchestration-ui` | 退役 | 后续清理出构建和文档入口 | 辅助开发工程多余 |
| 主样式 | `frontend/src/styles/main.css` | 适配 | `styles/base.css`、`styles/layout.css`、`styles/components.css` | 可保留视觉基调，按页面拆分 |
| 类型定义 | `frontend/src/types/*` | 适配 | 按 feature 拆分或保留统一 types | 优先避免大规模重命名 |

## 4. 数据库与迁移资源

| 资源 | 状态 | 处理策略 |
| --- | --- | --- |
| `import_jobs` | 复用 | MVP0 收纳任务账本 |
| `source_files` | 复用 | MVP0 Raw Sources 文件账本 |
| `source_contents` | 复用 | MVP0 正文抽取结果 |
| `wiki_ingest_runs` | 按需新增 | MVP0-4 Obsidian 写入结果账本，实施到 Wiki 写入节点时再新增 |
| `system_dictionaries` | 按需新增 | MVP0 中文状态字典，实施状态映射时再新增 |
| `sources` | 退役 | 与 MVP0 SourceFile 主账本语义重叠，后续清理 |
| `obsidian_notes` | 退役 | Source Note 历史语义退出，MVP0 改用 wiki ingest 结果 |
| `agent_runs` / `agent_steps` / `review_items` | 退役 | 不再复用为 Wiki ingest |
| `wiki_pages` / `wiki_integrations` | 暂缓 | MVP0 先不预建，确有页面注册需求时再设计 |
| `mcp_tool_calls` | 冻结 | MCP 不进入 MVP0 |
| `personal_records` | 冻结 | LifeOS 不进入 MVP0 |
| `vector_export_jobs` / `content_chunks` | 冻结 | 向量不进入 MVP0 |
| `knowledge_maintenance_*` | 冻结 | 知识体检不进入 MVP0 |
| Orchestration 相关表或配置 | 退役 | 后续确认依赖后清理 |

规则：

- MVP0 不继续背历史表结构包袱。
- 数据库清理单独成节点执行，先出迁移方案和回滚策略。
- 新表按需要新增，不提前为后续能力预建。
- 如果后续需要保留用户历史数据，必须先写数据迁移文档。

## 5. 测试资源

| 测试 | 状态 | 后续用途 |
| --- | --- | --- |
| `ImportJobApiIntegrationTests` | 复用 | 路径导入和 Raw Sources 回归 |
| `ObsidianApiIntegrationTests` | 适配 | LLM Wiki 写入和 Vault 安全回归 |
| `WikiCompileApiIntegrationTests` | 适配 | 改造为 Wiki ingest 回归 |
| `AiReviewApiIntegrationTests` | 冻结 | 旧审核流冻结验证 |
| `McpPreviewApiIntegrationTests` | 冻结 | MCP 高级能力冻结验证 |
| `VectorExportApiIntegrationTests` | 冻结 | 向量高级能力冻结验证 |
| `KnowledgeMaintenanceApiIntegrationTests` | 冻结 | 体检高级能力冻结验证 |
| `LocalFileScannerTests` | 适配 | 拆成扫描、复制、hash、分类测试 |
| `TextContentExtractorTests` | 复用 | 正文抽取回归 |

## 6. 物理迁移目标

### 6.1 后端目标

```text
backend/wikiforge-core-service/src/main/java/com/wikiforge/core/
  interfaces/web/capture/
  interfaces/web/wiki/
  interfaces/web/settings/
  application/capture/
  application/wiki/
  application/settings/
  domain/source/
  domain/wiki/
  domain/job/
  infrastructure/persistence/source/
  infrastructure/filesystem/obsidian/
  infrastructure/integration/model/

backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/
  application/ingest/
  application/upload/
  domain/scan/
  infrastructure/filesystem/
  infrastructure/extractor/
```

### 6.2 前端目标

```text
frontend/src/
  shell/
  pages/capture/
  pages/inbox/
  pages/wiki/
  pages/logs/
  pages/settings/
  features/capture/
  features/source-inbox/
  features/llm-wiki/
  features/obsidian-vault/
  features/runtime-log/
```

## 7. 迁移验证命令

后续每次物理迁移至少执行：

```powershell
git diff --check
.\mvnw -pl wikiforge-core-service,wikiforge-worker-service test
npm --prefix frontend run build
```

如果只迁移文档或图纸，仅执行：

```powershell
git diff --check
```
