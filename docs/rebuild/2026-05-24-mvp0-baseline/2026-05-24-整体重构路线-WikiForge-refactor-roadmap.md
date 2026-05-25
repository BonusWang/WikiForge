# 2026-05-24 WikiForge MVP0 路线图 MVP0 Roadmap

## 1. 路线原则

- 做减法，先把主流程跑通。
- 单线推进，不做多 Agent 并行。
- 先设计基座，后改代码。
- 先收纳和 Obsidian Wiki，后扩展能力。
- 不继续建设辅助开发工程服务。

## 2. 节点划分

| 节点 | 目标 | 交付物 | 验收 |
| --- | --- | --- | --- |
| MVP0-0 基座整理 | 建立当前事实来源 | PRD、四层架构、资源盘点、路线图、文档隔离 | `git diff --check` |
| MVP0-1 设计对齐 | 固定前端、后端、四层架构、数据流和最小数据库 | 设计确认稿 | 文档互链通过 |
| MVP0-2 前端主流程骨架 | 建立收纳、资料箱、Wiki、日志、设置 | 页面骨架和导航 | 前端构建通过 |
| MVP0-3 收纳链路 | 路径扫描、Raw Sources、SourceFile 账本、最小收纳表 | Core / Worker 最小链路 | 后端测试通过 |
| MVP0-4 Obsidian Wiki | Source page、Wiki page、index、log | Wiki ingest 最小闭环 | Obsidian 写入测试通过 |
| MVP0-5 上传入口 | 浏览器上传进入同一收纳流程 | 上传 API 和 UI | 上传回归通过 |
| MVP0-6 清理历史包袱 | 高级能力退主线，辅助工程退役，历史表结构退场 | 导航、模块、数据库清单收敛 | 主入口无历史能力 |

## 3. MVP0-0 基座整理

该节点已完成文档和基座整理。当前已进入代码落地阶段，后续执行记录以本目录 README 和 MVP0 代码重构执行计划为准。

交付文件：

- `docs/rebuild/2026-05-24-mvp0-baseline/README.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-现状基线-WikiForge-pre-rebuild-baseline.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构需求文档-WikiForge-rebuild-prd.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-四层架构设计-WikiForge-four-layer-architecture.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/diagrams/2026-05-24-WikiForge-four-layer-architecture.drawio`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-前端设计方案-WikiForge-mvp0-frontend-design.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-后端设计方案-WikiForge-mvp0-backend-design.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-数据库设计方案-WikiForge-mvp0-data-design.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-API契约设计-WikiForge-mvp0-api-contract.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-Obsidian-LLM-Wiki设计-WikiForge-mvp0-obsidian-llm-wiki-design.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-现有资源盘点-WikiForge-reusable-assets-inventory.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构路线-WikiForge-refactor-roadmap.md`

## 4. MVP0-1 设计对齐

需要与用户确认：

- 前端一级导航和页面职责。
- 后端 Core / Worker / Common 边界。
- 四层架构中每层承载的能力。
- API 最小集合。
- API 请求响应字段和错误格式。
- MVP0 最小数据库表集合。
- 中文状态字典和前端映射规则。
- Obsidian Vault 目录结构和写入规则。
- 哪些历史模块复用、冻结或退役。

验收：

```powershell
git diff --check
```

## 5. MVP0-2 前端主流程骨架

目标：

- 建立轻量 App Shell。
- 主导航只保留：收纳、资料箱、Wiki、日志、设置。
- 不展示 MCP、向量、LifeOS、知识体检、Orchestration。
- 旧 `DashboardView.vue` 已退出代码主线并删除。

验收：

```powershell
npm --prefix frontend run build
```

## 6. MVP0-3 收纳链路

目标：

- 保留并收敛 `POST /api/v1/import-jobs/local`。
- 路径扫描复制到 Raw Sources。
- hash 去重、类型识别、SourceFile 账本可用。
- MVP0 只使用 `import_jobs`、`source_files`、`source_contents` 作为收纳表。
- 状态展示使用 `system_dictionaries`，不让前端硬编码英文状态。
- Worker 原子能力拆成扫描、复制、hash、抽取。

验收：

```powershell
.\mvnw -pl wikiforge-core-service,wikiforge-worker-service test
```

## 7. MVP0-4 Obsidian LLM Wiki

目标：

- 建立 schema、index、log 规则。
- Vault 内只写 `WikiForge/` 托管目录。
- 生成 Source page 和 Wiki page。
- 记录写入状态和失败原因。
- 如确需持久化写入结果，只新增 `wiki_ingest_runs`，不复用 `agent_runs`。
- 当前 MVP0 先使用规则式 Markdown 写入；模型整理能力后续单独设计。

验收：

```powershell
.\mvnw -pl wikiforge-core-service test
```

## 8. MVP0-5 上传入口

目标：

- 新增 `POST /api/v1/upload-sources`。
- 上传文件进入 Raw Sources。
- 上传和路径扫描复用同一 SourceFile / Wiki ingest 流程。
- 2026-05-24 已接入最小实现：Core 负责 multipart 上传、hash 命名、Raw Sources 落盘和 SourceFile 登记；前端收纳页提供选择/拖入文件入口。

验收：

```powershell
.\mvnw -pl wikiforge-core-service,wikiforge-worker-service test
npm --prefix frontend run build
```

## 9. MVP0-6 清理历史包袱

目标：

- MCP、向量、LifeOS、知识体检不出现在主导航。
- Orchestration 辅助开发工程退役，源码、Dockerfile 和 `agentteam/` 工作区从主线移除。
- 旧 Work Order、开发者日志、历史 Roadmap 不再作为启动入口。
- 历史数据库表结构输出退役清单；删除或迁移前必须有备份和回滚方案。

验收：

```powershell
git diff --check
```
