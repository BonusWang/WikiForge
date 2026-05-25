# 2026-05-24 WikiForge MVP0 后端设计方案 MVP0 Backend Design

## 1. 设计目标

MVP0 后端只服务一条主流程：

```text
文件入口
  -> Raw Sources 收纳
  -> SourceFile / SourceContent 账本
  -> Obsidian LLM Wiki 写入
  -> index/log 更新
  -> UI 查看状态和结果
```

本设计不引入新业务服务，不恢复 Orchestration，不把 MCP、向量、LifeOS、知识体检塞回主流程。

## 2. 服务边界

MVP0 后端保留三个工程边界：

| 边界 | 职责 | 对外暴露 |
| --- | --- | --- |
| Core Service | 对外 API、业务用例、任务账本、数据库归属、Obsidian 写入结果登记 | Web 前端只调用 Core |
| Worker Service | 路径扫描、复制、hash、类型识别、正文抽取等耗时任务 | 只接受 Core 内部调用 |
| Common | 通用响应、错误码、路径安全等共享基础能力 | 不承载业务流程 |

当前不新增 Gateway。后续启用 Gateway 前，必须先更新项目架构强约定并补充服务准入设计。

## 3. Core Service 设计

Core 是 MVP0 唯一用户 API 入口，拥有业务数据和任务状态。

### 3.1 目标包结构

```text
com.wikiforge.core
  interfaces.web.capture
  interfaces.web.source
  interfaces.web.wiki
  interfaces.web.dictionary
  application.capture
  application.source
  application.wiki
  application.dictionary
  domain.job
  domain.source
  domain.wiki
  domain.dictionary
  infrastructure.persistence.job
  infrastructure.persistence.source
  infrastructure.persistence.wiki
  infrastructure.persistence.dictionary
  infrastructure.filesystem.obsidian
  infrastructure.integration.worker
  infrastructure.integration.model
```

### 3.2 Core 保留职责

- 创建和查询收纳任务。
- 接收浏览器上传。
- 维护 `import_jobs`、`source_files`、`source_contents`、`wiki_ingest_runs`、`system_dictionaries`。
- 调用 Worker 执行路径扫描和文件处理。
- 触发 Obsidian LLM Wiki 写入。
- 只写 Obsidian Vault 内 `WikiForge/` 托管目录。
- 返回中文状态码、中文状态名和中文说明。

### 3.3 Core 禁止职责

- 不直接扫描用户目录。
- 不把 Worker 内部 API 直接暴露给前端。
- 不把旧 `agent_runs` / `review_items` 复用为 Wiki ingest。
- 不承载 Orchestration 辅助开发工程。

## 4. Worker Service 设计

Worker 是原子能力执行端，只处理文件任务。

### 4.1 目标包结构

```text
com.wikiforge.worker
  interfaces.web.internal
  application.ingest
  application.upload
  domain.scan
  domain.file
  infrastructure.filesystem
  infrastructure.extractor
  infrastructure.integration.core
```

### 4.2 Worker 原子能力

| 原子能力 | 目标组件 | 说明 |
| --- | --- | --- |
| 路径校验 | `PathSafety` | 复用 Common，防止 Raw Sources 重叠和路径逃逸 |
| 目录扫描 | `LocalDirectoryScanner` | 只输出候选文件，不写数据库 |
| 文件复制 | `RawSourceCopier` | 复制到 Raw Sources，不覆盖已有文件 |
| hash 计算 | `ContentHasher` | 默认 SHA-256 |
| 类型识别 | `FileTypeClassifier` | 识别文档、图片、二进制、大文件 |
| 正文抽取 | `TextContentExtractor` | 抽取 Markdown / TXT / PDF / DOCX |
| 结果回写 | `CoreImportJobClient` | 回写 Core 内部 API |

### 4.3 Worker 禁止职责

- 不直接访问业务数据库。
- 不做用户查询接口。
- 不决定 Wiki 页面结构。
- 不写 Obsidian Vault。
- 不调用 LLM。

## 5. Common 设计

Common 只保留跨服务基础能力：

- `ApiResponse`：统一响应包装。
- `BusinessException` / `ErrorCode`：统一错误体系。
- `PathSafety`：路径绝对化、重叠校验、Vault 逃逸校验。
- 后续可加入分页模型和通用字典 DTO，但不能加入业务流程。

MCP、向量、Orchestration 等高级能力错误码在 MVP0 主流程中不新增使用点。

## 6. API 设计

详细请求、响应和错误格式以 MVP0 API 契约设计为准：

```text
docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-API契约设计-WikiForge-mvp0-api-contract.md
```

### 6.1 对前端开放

| 方法 | 路径 | 用途 | MVP0 状态 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/import-jobs/local` | 创建本地路径收纳任务 | 保留并收敛 |
| `GET` | `/api/v1/import-jobs` | 查询收纳任务 | 保留 |
| `GET` | `/api/v1/source-files` | 查询资料箱 | 保留并收敛 |
| `POST` | `/api/v1/upload-sources` | 浏览器上传文件 | 已接入 |
| `POST` | `/api/v1/source-files/{fileUid}/wiki-ingest-runs` | 单文件整理到 Wiki | 已接入 |
| `GET` | `/api/v1/wiki-ingest-runs` | 查询 Wiki 写入记录 | 已接入 |
| `GET` | `/api/v1/dictionaries` | 查询状态字典 | 已接入 |

设置页当前只使用 Obsidian 初始化和状态查询接口；MVP0 不提供持久化 `/api/v1/settings` API。

### 6.2 Core 内部开放给 Worker

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `PATCH` | `/api/v1/internal/import-jobs/{jobUid}/status` | Worker 回写任务状态 |
| `POST` | `/api/v1/internal/import-jobs/{jobUid}/source-files:batch` | Worker 批量回写 SourceFile |
| `POST` | `/api/v1/internal/source-files/{fileUid}/content` | Worker 回写正文抽取结果 |

内部接口必须有内部令牌或等效校验，前端不得调用。

## 7. 状态设计

MVP0 状态码统一使用中文业务码值，由 `system_dictionaries` 维护。代码层可保留英文枚举名，但 API、数据库业务状态值和前端展示不得直接暴露英文状态。

### 7.1 API 返回格式

```json
{
  "statusCode": "已收纳",
  "statusLabel": "已收纳",
  "statusDescription": "文件已复制到 Raw Sources",
  "statusColor": "success"
}
```

### 7.2 收纳任务状态

| 中文码值 | 含义 |
| --- | --- |
| 已创建 | 任务已创建，尚未开始 |
| 执行中 | Worker 正在扫描或处理 |
| 已完成 | 全部文件处理完成 |
| 部分失败 | 部分文件失败，但主任务有成功结果 |
| 失败 | 任务整体失败 |

### 7.3 资料状态

| 中文码值 | 含义 |
| --- | --- |
| 已登记 | 已创建 SourceFile 账本 |
| 已收纳 | 文件已进入 Raw Sources |
| 重复文件 | hash 已存在，不重复复制 |
| 抽取中 | 正在抽取正文 |
| 已抽取 | 正文抽取完成 |
| 待整理到 Wiki | 可以触发 Wiki ingest |
| 已写入 Wiki | Obsidian 写入成功 |
| 失败 | 处理失败 |

### 7.4 Wiki 写入状态

| 中文码值 | 含义 |
| --- | --- |
| 已创建 | Wiki ingest run 已创建 |
| 写入中 | 正在生成或写入 Wiki 页面 |
| 已写入 | Source page、Wiki page、index/log 已写入 |
| 兜底写入 | LLM 不可用，已用规则式 Markdown 写入 |
| 失败 | 写入失败 |

## 8. 数据归属

| 数据 | 归属服务 | 说明 |
| --- | --- | --- |
| 收纳任务 | Core | `import_jobs` |
| 资料文件 | Core | `source_files` |
| 正文内容 | Core | `source_contents` |
| Wiki 写入运行结果 | Core | `wiki_ingest_runs` |
| 状态字典 | Core | `system_dictionaries` |
| Raw Sources 文件 | Worker 执行，Core 记账 | Worker 复制，Core 记录路径和 hash |
| Obsidian 文件 | Core | Core 写入 Vault 并记录结果 |

Worker 不拥有业务数据表。

## 9. 历史能力处理

| 能力 | MVP0 处理 |
| --- | --- |
| Orchestration Service / UI | 退役，源码、Dockerfile 和独立 UI 已删除 |
| Agent Review | 退役，不作为 Wiki ingest 承载 |
| MCP Preview | 退役，MVP0 代码已删除 |
| Vector Export | 退役，MVP0 代码已删除 |
| Personal Record / LifeOS | 退役，MVP0 代码已删除 |
| Knowledge Maintenance | 退役，MVP0 代码已删除 |
| 旧 Wiki Compile / Source Note / Link Source | 退役，MVP0 改用 Wiki ingest 和 Obsidian `WikiForge/` 托管目录 |

退役能力后续必须单独出需求、设计、API、数据表和验证方案，不能直接回到主导航或主流程。

## 10. 验收规则

后续代码重构必须满足：

- 前端只调用 Core。
- Worker 只接受 Core 内部调用。
- Worker 不直接访问数据库。
- 新 API 先登记到项目架构强约定。
- 新状态先进入 `system_dictionaries`，再进入 API 和前端。
- 新表先通过数据库设计准入，再写 migration。
- Obsidian 写入不得越过 Vault 内 `WikiForge/` 托管目录。
- Orchestration 不在 Maven 主构建、Compose 主流程和前端主入口中继续扩展；相关源码和 Dockerfile 已删除。

验证命令：

```powershell
.\mvnw -pl wikiforge-core-service,wikiforge-worker-service test
git diff --check
```
