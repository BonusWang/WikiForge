# 2026-05-23 WikiForge MCP 接口契约 MCP API Contract

## 版本信息

- 文档版本：v0.1
- 当前阶段：MVP5 / R4-2 契约冻结
- 当前分支：`codex/mvp5-mcp-preview`
- 适用范围：R4-3、R4-4、R4-5 的后端、前端、文档和测试实现

## 1. 契约目标

MVP5 先提供 WikiForge MCP 能力的 HTTP Preview。它用于 OpenClaw、Hermes、本机脚本和其他 Agent 在本机调用 WikiForge 的基础能力。

本轮冻结的是工具形态、权限边界、日志契约和后续并行任务边界，不引入完整 MCP transport、不做 MCP Marketplace、不做多用户权限系统。

## 2. 服务归属

MVP5 HTTP Preview 暂时归属 `wikiforge-core-service`：

- Core Service 提供 `/api/v1/mcp/**` 业务入口。
- Core Service 写入 `mcp_tool_calls` 和 `personal_records`。
- Orchestration Service 只管理开发编排任务，不承载 MCP 业务调用。
- 后续可拆出 `wikiforge-mcp-service`，但必须保持本文件定义的 tool schema 和调用日志兼容。

## 3. HTTP Preview API

所有接口返回 WikiForge 统一响应：

```json
{
  "success": true,
  "data": {},
  "message": "ok",
  "code": null
}
```

### 3.1 查询工具清单

```text
GET /api/v1/mcp/tools
```

响应数据：

```json
{
  "tools": [
    {
      "name": "search_sources",
      "description": "Search organized WikiForge sources without exposing local filesystem paths.",
      "enabled": true,
      "inputSchema": {},
      "outputSchema": {}
    }
  ]
}
```

### 3.2 调用工具

```text
POST /api/v1/mcp/tools/{toolName}/call
```

请求体：

```json
{
  "arguments": {}
}
```

响应数据：

```json
{
  "callUid": "mcp_call_20260523_xxxxxxxx",
  "toolName": "search_sources",
  "status": "completed",
  "result": {},
  "error": null,
  "durationMs": 15,
  "createdAt": "2026-05-23T20:00:00+08:00"
}
```

### 3.3 查询调用日志

```text
GET /api/v1/mcp/calls
```

查询参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `toolName` | string | 否 | 空 | 按工具名过滤 |
| `status` | string | 否 | 空 | `completed`、`failed` |
| `callerType` | string | 否 | 空 | 调用方类型 |
| `page` | integer | 否 | 1 | 从 1 开始 |
| `pageSize` | integer | 否 | 20 | 最大 100 |

响应数据：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

## 4. 调用方 Header

MVP5 不做登录，但记录调用来源。

```text
X-WikiForge-Caller-Type: user | agent | external_agent | system
X-WikiForge-Caller-Id: openclaw | hermes | local-dev
```

默认值：

```text
callerType = external_agent
callerId = local-mcp-preview
```

Header 只用于审计和日志展示，不作为权限凭证。

## 5. 通用权限边界

所有 MCP Preview 工具必须遵守以下边界：

- 不返回 `originalPath`、`managedPath`、`absolutePath`、Raw Sources 绝对路径或本机任意绝对路径。
- 只返回 `sourceUid`、`fileUid`、`noteUid`、Vault 相对路径和 `obsidianUri`。
- 不读取未登记的本地文件。
- 不支持删除、移动、重命名本地文件。
- 不支持任意路径扫描。
- 不自动读取飞书、腾讯文档、网页或邮箱；在线连接器放到 V1。
- 不自动调用 AI 总结；需要 AI 的能力仍走现有 Review 流程。
- 所有工具名必须在 allow list 中，MVP5 allow list 仅包含本文件的 5 个工具。
- 每次工具调用必须记录 `mcp_tool_calls`。
- 日志中的敏感字段必须脱敏，尤其是 `rawContent`、`markdown`、`structured`。

## 6. 首批工具清单

### 6.1 search_sources

用途：搜索已经进入 WikiForge 的 Source / SourceFile 摘要，不返回本地路径。

输入 Schema：

```json
{
  "type": "object",
  "properties": {
    "keyword": { "type": "string", "maxLength": 200 },
    "status": {
      "type": "string",
      "enum": ["pending", "organized", "processing", "pending_review", "archived", "rejected", "failed"]
    },
    "page": { "type": "integer", "minimum": 1, "default": 1 },
    "pageSize": { "type": "integer", "minimum": 1, "maximum": 50, "default": 20 }
  },
  "additionalProperties": false
}
```

输出字段：

| 字段 | 说明 |
| --- | --- |
| `sourceUid` | Source UID |
| `fileUid` | SourceFile UID，可能为空 |
| `title` | Source 标题或文件名 |
| `sourceType` | `file`、`text`、`link`、`note` 等 |
| `sourcePlatform` | `local`、`manual` 等 |
| `status` | Source 状态 |
| `fileName` | 文件名 |
| `fileExt` | 文件扩展名 |
| `mimeType` | MIME 类型 |
| `fileSize` | 文件大小 |
| `parseStatus` | 解析状态 |
| `organizeStatus` | 归集状态 |
| `obsidianNoteUid` | 关联 Note UID |
| `obsidianNoteTitle` | 关联 Note 标题 |
| `createdAt` | 创建时间 |

### 6.2 get_source

用途：读取单个 Source / SourceFile 的安全摘要。

输入 Schema：

```json
{
  "type": "object",
  "properties": {
    "sourceUid": { "type": "string", "pattern": "^src_[A-Za-z0-9_\\-]+$" },
    "fileUid": { "type": "string", "pattern": "^file_[A-Za-z0-9_\\-]+$" },
    "includeContentExcerpt": { "type": "boolean", "default": true }
  },
  "oneOf": [
    { "required": ["sourceUid"] },
    { "required": ["fileUid"] }
  ],
  "additionalProperties": false
}
```

输出结构：

```json
{
  "source": {},
  "sourceFile": {},
  "content": {
    "contentUid": "content_xxx",
    "parseStatus": "completed",
    "charCount": 1000,
    "excerpt": "safe excerpt, max 1000 chars"
  },
  "obsidianNote": {
    "noteUid": "note_xxx",
    "title": "title",
    "vaultPath": "00_Inbox_收集箱/Sources_来源/demo.md",
    "obsidianUri": "obsidian://open?...",
    "status": "written"
  }
}
```

禁止输出：`originalPath`、`managedPath`、`absolutePath`。

### 6.3 create_source

用途：让外部 Agent 写入一段文本型 Source 草案。MVP5 不写文件系统，不触发在线连接器抓取。

输入 Schema：

```json
{
  "type": "object",
  "required": ["title", "rawContent"],
  "properties": {
    "title": { "type": "string", "minLength": 1, "maxLength": 512 },
    "rawContent": { "type": "string", "minLength": 1, "maxLength": 100000 },
    "sourceType": {
      "type": "string",
      "enum": ["text", "note", "link", "manual"],
      "default": "text"
    },
    "sourcePlatform": { "type": "string", "maxLength": 128, "default": "manual" },
    "sourceUrl": { "type": "string", "maxLength": 2048 },
    "processingIntent": {
      "type": "string",
      "enum": ["organize_only", "extract_and_review"],
      "default": "organize_only"
    }
  },
  "additionalProperties": false
}
```

输出字段：

| 字段 | 说明 |
| --- | --- |
| `sourceUid` | 新建 Source UID |
| `status` | 初始状态，默认 `pending` |
| `sourceType` | 实际保存类型 |
| `sourcePlatform` | 实际保存平台 |
| `createdAt` | 创建时间 |

边界：

- 不接受本地路径参数。
- 不从 `sourceUrl` 抓取远程内容。
- 不自动写 Obsidian。
- 不自动调用 AI。

### 6.4 get_obsidian_note

用途：读取已登记的 Obsidian Note。

输入 Schema：

```json
{
  "type": "object",
  "required": ["noteUid"],
  "properties": {
    "noteUid": { "type": "string", "pattern": "^note_[A-Za-z0-9_\\-]+$" },
    "includeMarkdown": { "type": "boolean", "default": true }
  },
  "additionalProperties": false
}
```

输出字段：

| 字段 | 说明 |
| --- | --- |
| `noteUid` | Note UID |
| `sourceUid` | 关联 Source UID |
| `fileUid` | 关联 SourceFile UID |
| `title` | Note 标题 |
| `vaultName` | Vault 名称 |
| `vaultPath` | Vault 内相对路径 |
| `obsidianUri` | 可打开 Obsidian 的 URI |
| `status` | Note 状态 |
| `markdown` | `includeMarkdown=true` 时返回 |

禁止输出：`absolutePath`。

### 6.5 create_personal_record

用途：让 OpenClaw / Hermes / 外部 Agent 写入消费、账单、邮件、人际关系或个人事件记录。MVP5 只做结构化存储，不做总结。

输入 Schema：

```json
{
  "type": "object",
  "required": ["recordType", "title", "rawContent"],
  "properties": {
    "recordType": {
      "type": "string",
      "enum": ["expense", "bill", "email", "relationship", "event", "note"]
    },
    "title": { "type": "string", "minLength": 1, "maxLength": 512 },
    "occurredAt": { "type": "string", "format": "date-time" },
    "rawContent": { "type": "string", "minLength": 1, "maxLength": 100000 },
    "structured": { "type": "object" },
    "sourceChannel": { "type": "string", "maxLength": 128, "default": "mcp" },
    "sourceRef": { "type": "string", "maxLength": 2048 },
    "sensitivityLevel": {
      "type": "string",
      "enum": ["low", "medium", "high"],
      "default": "medium"
    }
  },
  "additionalProperties": false
}
```

输出字段：

| 字段 | 说明 |
| --- | --- |
| `recordUid` | Personal Record UID |
| `recordType` | 记录类型 |
| `status` | 初始状态，默认 `pending` |
| `createdAt` | 创建时间 |

边界：

- 不触发 AI 总结。
- 不写入 Obsidian 日记或关系页。
- 不把 `rawContent` 原文写入 `mcp_tool_calls.input_json`，只写入长度、摘要 hash 和字段名。

## 7. 调用日志契约

MVP5 冻结 Flyway 编号：

```text
V20260523_006__create_mcp_preview_tables.sql
```

该 migration 由主编排 Agent 串行创建，避免与后续并行实现冲突。

MVP5 最小表：

- `mcp_tool_calls`
- `personal_records`

`mcp_servers` 作为完整 MCP Server / Client 配置表后续再引入；HTTP Preview 使用固定内置 server，不需要动态 server 配置表。

### 7.1 mcp_tool_calls

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint pk | 主键 |
| `call_uid` | varchar(64) unique | 调用 ID |
| `tool_name` | varchar(128) | 工具名 |
| `caller_type` | varchar(64) | 调用方类型 |
| `caller_id` | varchar(128) | 调用方 ID |
| `input_json` | json | 脱敏后的输入 |
| `output_json` | json | 脱敏后的输出摘要 |
| `status` | varchar(64) | `completed`、`failed` |
| `error_code` | varchar(64) | 错误码 |
| `error_message` | text | 错误信息 |
| `duration_ms` | bigint | 执行耗时 |
| `created_at` | datetime | 创建时间 |

### 7.2 personal_records

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint pk | 主键 |
| `record_uid` | varchar(64) unique | 记录 ID |
| `record_type` | varchar(64) | 记录类型 |
| `title` | varchar(512) | 标题 |
| `occurred_at` | datetime null | 发生时间 |
| `source_channel` | varchar(128) | `mcp`、`openclaw`、`hermes` 等 |
| `source_ref` | text | 来源引用 |
| `raw_content` | longtext | 原始内容 |
| `structured_json` | json | 结构化字段 |
| `status` | varchar(64) | `pending`、`classified`、`summarized`、`archived`、`failed` |
| `sensitivity_level` | varchar(32) | `low`、`medium`、`high` |
| `created_by` | varchar(128) | 调用方 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

## 8. 错误码冻结

MVP5 预留以下错误码：

| 错误码 | 说明 |
| --- | --- |
| `MCP_TOOL_NOT_FOUND` | 工具不存在 |
| `MCP_TOOL_DISABLED` | 工具未启用 |
| `MCP_INVALID_INPUT` | 输入参数不符合 schema |
| `MCP_SOURCE_NOT_FOUND` | Source / SourceFile 不存在 |
| `MCP_OBSIDIAN_NOTE_NOT_FOUND` | Obsidian Note 不存在 |
| `MCP_FORBIDDEN_PATH_EXPOSURE` | 工具试图暴露本地绝对路径 |
| `MCP_CALL_FAILED` | 工具执行失败 |
| `PERSONAL_RECORD_INVALID_TYPE` | 个人记录类型不合法 |

代码实现时可映射到 `ErrorCode` 枚举，接口响应仍使用统一 `ApiResponse`。

## 9. 并行实现边界

R4-2 完成后允许进入 R4-3 / R4-4 并行开发，但必须遵守以下顺序：

1. 主编排 Agent 串行创建 Flyway migration、错误码和基础 DTO。
2. Core Agent 实现 `search_sources`、`get_source`、`create_source`。
3. Core Agent 或 Record Agent 实现 `get_obsidian_note`、`create_personal_record`。
4. UI Agent 基于冻结 API 展示工具清单和调用日志。
5. Docs/Test Agent 最后更新归档、接入说明和验证记录。

高冲突文件只允许主编排 Agent 修改：

- `backend/pom.xml`
- `backend/wikiforge-common/`
- `backend/wikiforge-core-service/src/main/resources/db/migration/`
- `.github/workflows/ci.yml`
- `deploy/docker-compose*.yml`
- `docs/archive/2026-05-23/*归档索引*`

## 10. R4-2 完成定义

- 本文档存在并被 `docs/README.md` 引用。
- MVP5 计划文档引用本文档作为冻结契约。
- 技术架构、数据模型、架构决策与本文档一致。
- Roadmap 标记 R4-2 Done，当前指针移动到 R4-3。
- 开发者日志和归档索引更新。
- `git diff --check` 通过。
