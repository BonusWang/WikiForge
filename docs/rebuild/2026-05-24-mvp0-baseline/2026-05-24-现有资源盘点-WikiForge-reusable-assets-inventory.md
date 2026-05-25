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

2026-05-25 起，明确无复用价值的历史高级能力已从 MVP0 代码、前端封装、迁移和测试中移除。

## 2. 后端资源

| 资源 | 当前路径 | 状态 | 迁移目标 | 说明 |
| --- | --- | --- | --- | --- |
| 路径安全 | `wikiforge-common/.../PathSafety.java` | 复用 | `common.filesystem` 保留 | 路径绝对化和重叠校验可直接复用 |
| 错误码体系 | `wikiforge-common/.../ErrorCode.java` | 适配 | `common.error` 保留并补充上传/Wiki ingest 错误码 | 现有错误码可扩展，不重建 |
| API 包装 | `ApiResponse` / `PageResult` | 复用 | `common.web` 或现路径 | REST 统一响应继续使用 |
| ImportJob 账本 | `ImportJobService`、Repository、DTO | 适配 | `core.application.capture` / `domain.job` | 保留路径导入能力，拆出状态机和 Worker 调度 |
| SourceFile 账本 | `SourceFileRepository`、Entity、DTO | 适配 | `domain.source` / `infrastructure.persistence.source` | 作为收纳主账本继续保留 |
| LocalFileScanner | `worker.infrastructure.filesystem.LocalFileScanner` | 适配 | `LocalDirectoryScanner` + `RawSourceFileCollector` + `ContentHasher` | 当前类混合扫描、入库、hash、分类，需要拆原子能力 |
| LocalImportJobRunner | `worker.application.service.LocalImportJobRunner` | 适配 | `worker.application.ingest.LocalImportJobRunner` | 保留编排骨架，拆出原子依赖 |
| TextContentExtractor | `worker.application.service.TextContentExtractor` | 复用 | `worker.infrastructure.extractor.TextContentExtractor` | Markdown/TXT/PDF/DOCX 抽取可复用 |
| ObsidianVaultService | `core.application.service.ObsidianVaultService` | 适配 | `core.application.wiki` + `infrastructure.filesystem.obsidian` | 路径校验、原子写入和 URI 生成可复用，写入范围改为 `WikiForge/` 托管目录 |
| WikiCompileService | `core.application.service.WikiCompileService` | 退役 | 已删除 | MVP0 改用 `WikiIngestRunService` 和 `wiki_ingest_runs` |
| AiReviewService | `core.application.service.AiReviewService` | 退役 | 已删除 | 旧审核流退出主流程 |
| LinkSourceService | `core.application.service.LinkSourceService` | 退役 | 已删除 | 链接资料入口不进入 MVP0 |
| PersonalRecordService | `core.application.service.PersonalRecordService` | 退役 | 已删除 | LifeOS 不进入 MVP0 |
| McpPreviewService | `core.application.service.McpPreviewService` | 退役 | 已删除 | MCP 不进入 MVP0 |
| VectorExportService | `core.application.service.VectorExportService` | 退役 | 已删除 | 向量不进入 MVP0 |
| KnowledgeMaintenanceService | `core.application.service.KnowledgeMaintenanceService` | 退役 | 已删除 | 知识体检不进入 MVP0 |
| Orchestration Service | `backend/wikiforge-orchestration-service` | 退役 | 已删除 | 辅助开发工程多余，不承载知识库业务 |

## 3. 前端资源

| 资源 | 当前路径 | 状态 | 迁移目标 | 说明 |
| --- | --- | --- | --- | --- |
| Dashboard 单页 | `frontend/src/views/DashboardView.vue` | 已退役 | 已迁移到 `shell/`、`pages/`、`features/` 并删除 | 不再作为代码资产保留，追溯时读取归档 |
| Import Jobs API | `frontend/src/api/import-jobs` | 适配 | `features/capture/api` 或保留 `api/import-jobs` | 路径导入继续使用 |
| Obsidian API | `frontend/src/api/obsidian` | 适配 | 保留 init/status | 已删除旧 Source Note 草稿、写入和预览封装 |
| Wiki API | `frontend/src/api/wiki` | 退役 | 已删除 | 旧 compile/pages/integrations API 退出，改用 `api/wiki-ingest-runs` |
| Review API | `frontend/src/api/review` | 退役 | 已删除 | 旧人工审核流退出 |
| MCP API | `frontend/src/api/mcp` | 退役 | 已删除 | MCP 不进入 MVP0 |
| Vector API | `frontend/src/api/vector-exports` | 退役 | 已删除 | 向量不进入 MVP0 |
| LifeOS API | `frontend/src/api/lifeos` | 退役 | 已删除 | LifeOS 不进入 MVP0 |
| Knowledge Maintenance API | `frontend/src/api/knowledge-maintenance` | 退役 | 已删除 | 知识体检不进入 MVP0 |
| Orchestration UI | `orchestration-ui` | 退役 | 已删除 | 辅助开发工程多余 |
| 主样式 | `frontend/src/styles/main.css` | 适配 | `styles/base.css`、`styles/layout.css`、`styles/components.css` | 可保留视觉基调，按页面拆分 |
| 类型定义 | `frontend/src/types/*` | 适配 | 按 feature 拆分或保留统一 types | 优先避免大规模重命名 |

## 4. 数据库与迁移资源

| 资源 | 状态 | 处理策略 |
| --- | --- | --- |
| `import_jobs` | 复用 | MVP0 收纳任务账本 |
| `source_files` | 复用 | MVP0 资料仓库文件账本 |
| `source_contents` | 复用 | MVP0 正文抽取结果 |
| `wiki_ingest_runs` | 已落地 | MVP0 Obsidian 写入结果账本 |
| `system_dictionaries` | 已落地 | MVP0 中文状态字典 |
| `sources` | 退役 | 与 MVP0 SourceFile 主账本语义重叠，迁移和代码依赖已移除 |
| `system_settings` / `model_providers` | 退役 | 设置持久化和模型配置不进入 MVP0，预建迁移已移除 |
| `obsidian_notes` | 退役 | Source Note 历史语义退出，MVP0 改用 wiki ingest 结果 |
| `agent_runs` / `agent_steps` / `review_items` | 退役 | 不再复用为 Wiki ingest |
| `wiki_pages` / `wiki_integrations` | 退役 | 迁移文件已移除，确有页面注册需求时重新设计 |
| `mcp_tool_calls` | 退役 | 迁移文件已移除 |
| `personal_records` | 退役 | 迁移文件已移除 |
| `vector_export_jobs` / `content_chunks` | 退役 | 迁移文件已移除 |
| `knowledge_maintenance_*` | 退役 | 迁移文件已移除 |
| Orchestration 相关配置 | 退役 | 服务源码、独立 UI、Dockerfile、`agentteam/` 和 `.env.example` 变量已删除 |

规则：

- MVP0 不继续背历史表结构包袱。
- 数据库清理单独成节点执行，先出迁移方案和回滚策略。
- 新表按需要新增，不提前为后续能力预建。
- 如果后续需要保留用户历史数据，必须先写数据迁移文档。

## 5. 测试资源

| 测试 | 状态 | 后续用途 |
| --- | --- | --- |
| `ImportJobApiIntegrationTests` | 复用 | 路径导入和资料仓库回归 |
| `ObsidianApiIntegrationTests` | 适配 | LLM Wiki 写入和 Vault 安全回归 |
| `WikiCompileApiIntegrationTests` | 退役 | 已删除 |
| `AiReviewApiIntegrationTests` | 退役 | 已删除 |
| `McpPreviewApiIntegrationTests` | 退役 | 已删除 |
| `VectorExportApiIntegrationTests` | 退役 | 已删除 |
| `KnowledgeMaintenanceApiIntegrationTests` | 退役 | 已删除 |
| `LocalFileScannerTests` | 适配 | 拆成扫描、复制、hash、分类测试 |
| `TextContentExtractorTests` | 复用 | 正文抽取回归 |

## 6. 物理迁移目标

### 6.1 后端目标

```text
backend/wikiforge-core-service/src/main/java/com/wikiforge/core/
  interfaces/web/capture/
  interfaces/web/wiki/
  application/capture/
  application/wiki/
  domain/source/
  domain/wiki/
  domain/job/
  infrastructure/persistence/source/
  infrastructure/filesystem/obsidian/

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
