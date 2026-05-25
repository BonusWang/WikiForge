# 2026-05-24 WikiForge MVP0 API 契约设计 MVP0 API Contract

## 1. 设计目标

本契约固定 MVP0 前端、Core、Worker 之间的接口边界。

目标：

- 前端只调用 Core API。
- Worker 只接受 Core 内部调用。
- 所有用户可见状态使用中文码值和中文说明。
- API 不返回宿主机敏感绝对路径。
- 历史高级能力接口不进入 MVP0 主契约，当前 MVP0 代码不再暴露这些接口。

## 2. 通用响应

继续使用现有 `ApiResponse` 外壳：

```json
{
  "success": true,
  "data": {},
  "message": "ok",
  "code": null
}
```

失败响应：

```json
{
  "success": false,
  "data": null,
  "message": "该路径不存在，请检查后重试。",
  "code": "SOURCE_002"
}
```

规则：

- `code` 是技术错误码，前端优先展示 `message`。
- 业务状态不使用 `code` 字段，统一使用 `statusCode` 等业务字段。
- 用户可见状态值必须来自 `system_dictionaries`。

## 3. 通用状态字段

单一主状态使用：

```json
{
  "statusCode": "已收纳",
  "statusLabel": "已收纳",
  "statusDescription": "文件已复制到 Raw Sources",
  "statusColor": "success",
  "isTerminal": false
}
```

多个状态维度使用前缀：

```json
{
  "collectStatusCode": "已收纳",
  "collectStatusLabel": "已收纳",
  "extractStatusCode": "已抽取",
  "extractStatusLabel": "已抽取",
  "wikiStatusCode": "待整理到 Wiki",
  "wikiStatusLabel": "待整理到 Wiki"
}
```

前端不得直接展示英文枚举、英文状态或旧接口的 `pending` / `running` / `completed` 等值。

## 4. 分页格式

列表统一返回：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

规则：

- `page` 从 1 开始。
- 默认 `pageSize` 为 20。
- 最大 `pageSize` 为 100。
- 排序默认按 `createdAt` 倒序。

## 5. 对前端开放的 API

| 方法 | 路径 | 页面入口 | 用途 | MVP0 状态 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/import-jobs/local` | 收纳 | 创建本地路径收纳任务 | 保留并收敛 |
| `GET` | `/api/v1/import-jobs` | 日志 / 收纳 | 查询收纳任务列表 | 保留并收敛 |
| `GET` | `/api/v1/import-jobs/{jobUid}` | 日志 | 查询收纳任务详情 | 保留并收敛 |
| `POST` | `/api/v1/upload-sources` | 收纳 | 浏览器上传文件 | 已接入 |
| `GET` | `/api/v1/source-files` | 资料箱 | 查询资料列表 | 保留并收敛 |
| `GET` | `/api/v1/source-files/{fileUid}` | 资料箱 | 查询资料详情 | 已接入 |
| `POST` | `/api/v1/source-files/{fileUid}/wiki-ingest-runs` | Wiki | 整理单个资料到 Wiki | 已接入 |
| `GET` | `/api/v1/wiki-ingest-runs` | Wiki / 日志 | 查询 Wiki 写入记录 | 已接入 |
| `GET` | `/api/v1/wiki-ingest-runs/{runUid}` | Wiki / 日志 | 查询 Wiki 写入详情 | 已接入 |
| `GET` | `/api/v1/dictionaries` | 全局 | 查询中文字典 | 已接入 |
| `POST` | `/api/v1/obsidian/init` | 设置 | 初始化 Vault 托管目录 | 保留并收敛 |
| `GET` | `/api/v1/obsidian/status` | 设置 | 查询 Vault 状态 | 保留并收敛 |

设置页在 MVP0 只保留运行口径展示、Vault 初始化和 Vault 状态查询，不提供持久化 `/settings` API。

历史高级能力接口集合已退出 MVP0 代码，不进入 MVP0 主契约。恢复任一历史接口必须重新提交需求、API 契约、数据归属和验证方案。

## 6. 收纳接口

### 6.1 创建本地路径收纳任务

`POST /api/v1/import-jobs/local`

请求：

```json
{
  "inputPath": "E:\\资料\\待读",
  "recursive": true,
  "maxCopyFileSizeMb": 100,
  "processingIntent": "仅收纳",
  "wikiWritebackMode": "自动"
}
```

字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `inputPath` | 是 | 用户输入的本机目录 |
| `recursive` | 否 | 是否递归扫描，默认 true |
| `maxCopyFileSizeMb` | 否 | 单文件复制上限 |
| `processingIntent` | 否 | 中文码值：仅收纳 / 收纳并整理 |
| `wikiWritebackMode` | 否 | 中文码值：自动 / 关闭 |

响应：

```json
{
  "jobUid": "job_xxx",
  "importType": "本地路径",
  "inputPathMasked": "E:\\资料\\待读",
  "statusCode": "已创建",
  "statusLabel": "已创建",
  "statusDescription": "收纳任务已创建，等待执行",
  "totalCount": 0,
  "successCount": 0,
  "duplicateCount": 0,
  "failedCount": 0,
  "createdAt": "2026-05-24T20:00:00+08:00"
}
```

安全规则：

- `inputPath` 与 Raw Sources 不得重叠。
- 返回给前端的路径必须脱敏或只返回用户已输入路径。
- 不返回系统内部临时路径。

### 6.2 查询收纳任务列表

`GET /api/v1/import-jobs?statusCode=执行中&page=1&pageSize=20`

响应 `data`：

```json
{
  "items": [
    {
      "jobUid": "job_xxx",
      "importType": "本地路径",
      "inputPathMasked": "E:\\资料\\待读",
      "statusCode": "执行中",
      "statusLabel": "执行中",
      "totalCount": 12,
      "successCount": 8,
      "duplicateCount": 2,
      "failedCount": 0,
      "createdAt": "2026-05-24T20:00:00+08:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

### 6.3 查询收纳任务详情

`GET /api/v1/import-jobs/{jobUid}`

响应在列表字段基础上增加：

- `startedAt`
- `completedAt`
- `failureReason`
- `recentSourceFiles`

## 7. 上传接口

### 7.1 浏览器上传文件

`POST /api/v1/upload-sources`

请求类型：`multipart/form-data`

字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `files` | 是 | 一个或多个文件 |
| `wikiWritebackMode` | 否 | 中文码值：自动 / 关闭 |

响应：

```json
{
  "jobUid": "job_xxx",
  "importType": "浏览器上传",
  "statusCode": "已完成",
  "statusLabel": "已完成",
  "statusDescription": "收纳任务已完成",
  "uploadedCount": 3,
  "createdAt": "2026-05-24T20:00:00+08:00"
}
```

规则：

- 上传文件进入同一 Raw Sources 流程。
- 同名文件不得覆盖，最终命名由 hash 和安全文件名决定。
- Core 完成上传落盘和 SourceFile 登记后返回 `已完成`。
- 上传资料的正文抽取状态初始为 `待抽取`，后续沿用资料箱和 Wiki ingest 主流程继续处理。
- 单文件大小和总大小限制由设置页展示。

## 8. 资料箱接口

### 8.1 查询资料列表

`GET /api/v1/source-files?collectStatusCode=已收纳&wikiStatusCode=待整理到 Wiki&page=1&pageSize=20`

响应 `data`：

```json
{
  "items": [
    {
      "fileUid": "sf_xxx",
      "jobUid": "job_xxx",
      "fileName": "资料.pdf",
      "fileExt": "pdf",
      "fileSizeBytes": 204800,
      "contentHash": "sha256_xxx",
      "rawSourceRelativePath": "2026/05/sf_xxx-资料.pdf",
      "collectStatusCode": "已收纳",
      "collectStatusLabel": "已收纳",
      "extractStatusCode": "已抽取",
      "extractStatusLabel": "已抽取",
      "wikiStatusCode": "待整理到 Wiki",
      "wikiStatusLabel": "待整理到 Wiki",
      "createdAt": "2026-05-24T20:00:00+08:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

### 8.2 查询资料详情

`GET /api/v1/source-files/{fileUid}`

详情增加：

- `originalName`
- `originalPathMasked`
- `mimeType`
- `duplicateOfFileUid`
- `extractFailureReason`
- `wikiFailureReason`
- `latestWikiIngestRun`

## 9. Wiki ingest 接口

### 9.1 创建 Wiki 写入运行

`POST /api/v1/source-files/{fileUid}/wiki-ingest-runs`

请求：

```json
{
  "writeMode": "自动",
  "targetTopic": "待分类",
  "targetProject": "待归档项目",
  "forceRewriteManagedBlock": false
}
```

字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `writeMode` | 否 | 中文码值：自动 / 兜底 / 仅预览 |
| `targetTopic` | 否 | 指定主题页，默认由规则判断 |
| `targetProject` | 否 | 指定项目页，默认不强制 |
| `forceRewriteManagedBlock` | 否 | 是否覆盖已有托管区块 |

响应：

```json
{
  "runUid": "wir_xxx",
  "fileUid": "sf_xxx",
  "statusCode": "写入中",
  "statusLabel": "写入中",
  "sourcePagePath": "WikiForge/10_来源/2026/05/sf_xxx-资料.md",
  "wikiPagePaths": [],
  "indexUpdated": false,
  "logEntryAppended": false,
  "fallbackReason": null,
  "createdAt": "2026-05-24T20:00:00+08:00"
}
```

### 9.2 查询 Wiki 写入记录

`GET /api/v1/wiki-ingest-runs?statusCode=已写入&page=1&pageSize=20`

响应字段：

- `runUid`
- `fileUid`
- `fileName`
- `statusCode`
- `statusLabel`
- `sourcePagePath`
- `wikiPagePaths`
- `indexUpdated`
- `logEntryAppended`
- `writeStatusCode`
- `writeStatusLabel`
- `fallbackReason`
- `failureReason`
- `createdAt`
- `completedAt`

### 9.3 查询 Wiki 写入详情

`GET /api/v1/wiki-ingest-runs/{runUid}`

详情增加：

- `managedBlockPreview`
- `logEntryPreview`
- `obsidianUri`
- `retryable`

## 10. 字典接口

### 10.1 查询字典

`GET /api/v1/dictionaries?dictType=资料状态`

响应：

```json
{
  "items": [
    {
      "dictType": "资料状态",
      "dictCode": "已收纳",
      "labelZh": "已收纳",
      "descriptionZh": "文件已复制到 Raw Sources",
      "sortOrder": 20,
      "colorToken": "success",
      "isTerminal": false,
      "isSuccess": true
    }
  ]
}
```

初始字典类型：

- 收纳任务状态。
- 资料状态。
- Wiki 写入状态。
- 写入模式。
- 处理意图。

## 11. Obsidian 接口

### 11.1 初始化 Vault 托管目录

`POST /api/v1/obsidian/init`

响应：

```json
{
  "vaultName": "WikiForgeVault",
  "managedRoot": "WikiForge/",
  "createdPaths": [
    "WikiForge/index.md",
    "WikiForge/log.md",
    "WikiForge/00_规则/",
    "WikiForge/10_来源/",
    "WikiForge/20_主题/",
    "WikiForge/30_项目/",
    "WikiForge/90_系统/"
  ]
}
```

### 11.2 查询 Vault 状态

`GET /api/v1/obsidian/status`

响应：

```json
{
  "vaultName": "WikiForgeVault",
  "vaultPathMasked": "E:\\WikiForgeVault",
  "managedRoot": "WikiForge/",
  "exists": true,
  "writable": true,
  "managedRootExists": true,
  "lastWriteAt": "2026-05-24T20:00:00+08:00",
  "failureReason": null
}
```

## 12. Core 内部 API

内部 API 只允许 Worker 调用，必须带内部令牌。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `PATCH` | `/api/v1/internal/import-jobs/{jobUid}/status` | 回写收纳任务状态 |
| `POST` | `/api/v1/internal/import-jobs/{jobUid}/source-files:batch` | 批量回写 SourceFile |
| `POST` | `/api/v1/internal/source-files/{fileUid}/content` | 回写正文抽取结果 |

状态回写请求：

```json
{
  "statusCode": "执行中",
  "totalCount": 12,
  "successCount": 8,
  "duplicateCount": 2,
  "failedCount": 0,
  "failureReason": null
}
```

SourceFile 批量回写请求：

```json
{
  "files": [
    {
      "fileUid": "sf_xxx",
      "jobUid": "job_xxx",
      "originalName": "资料.pdf",
      "originalPathMasked": "E:\\资料\\资料.pdf",
      "rawSourceRelativePath": "2026/05/sf_xxx-资料.pdf",
      "contentHash": "sha256_xxx",
      "fileSizeBytes": 204800,
      "fileType": "pdf",
      "collectStatusCode": "已收纳",
      "extractStatusCode": "待抽取",
      "wikiStatusCode": "待整理到 Wiki"
    }
  ]
}
```

正文回写请求：

```json
{
  "contentUid": "sc_xxx",
  "extractStatusCode": "已抽取",
  "plainText": "抽取后的正文",
  "metadata": {
    "pageCount": 10,
    "extractor": "pdf"
  },
  "failureReason": null
}
```

## 13. 错误码范围

MVP0 主流程只新增或使用以下错误码前缀：

| 前缀 | 用途 |
| --- | --- |
| `SOURCE_` | 路径、资料、Raw Sources 相关错误 |
| `IMPORT_` | 收纳任务错误 |
| `UPLOAD_` | 上传错误 |
| `WIKI_` | Wiki ingest 和页面写入错误 |
| `OBSIDIAN_` | Vault 和 Markdown 文件写入错误 |
| `DICT_` | 字典错误 |
| `SETTINGS_` | 设置错误 |
| `WORKER_` | Worker 内部调用错误 |
| `COMMON_` | 通用校验错误 |

AI Review、MCP、向量、LifeOS、知识体检、旧 Wiki Compile、旧 Source Note、Link Source、Orchestration 错误码不进入 MVP0 主流程新增使用点。

## 14. 验收规则

后续实现必须满足：

- 前端主流程只调用本契约列出的 Core API。
- 用户可见状态全部为中文码值和中文说明。
- API 不返回非必要宿主机敏感绝对路径。
- Worker 内部 API 不暴露给前端。
- Wiki 写入路径只指向 Vault 内 `WikiForge/` 托管目录。
- 高级能力接口不出现在 MVP0 导航和主流程调用中。

验证命令：

```powershell
.\mvnw -pl wikiforge-core-service,wikiforge-worker-service test
npm --prefix frontend run build
git diff --check
```
