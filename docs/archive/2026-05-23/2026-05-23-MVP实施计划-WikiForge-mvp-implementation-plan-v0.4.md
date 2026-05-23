# 2026-05-22 知识熔炉 WikiForge MVP 实施计划 v0.4

## 0. 架构评审后的执行调整

2026-05-23 架构评审后，MVP 执行顺序收敛为：

1. MVP 0：少服务微服务项目骨架 + CI/CD + Docker Compose + 基础配置 + Flyway 基线。
2. MVP 1：源文件归集。
3. MVP 2：Obsidian Source Note。
4. MVP 3：AI 辅助整理。
5. MVP 4：轻量 MCP 预览。

关键调整：

- MVP 0 必须同步建设 CI/CD、Docker、`.env.example`、健康检查和 Flyway 空库迁移校验。
- MVP 0 从单后端骨架调整为少服务微服务骨架，目标服务为 `wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui`。
- MVP 0 采用 MyBatis-Plus 3.5.x。
- MVP 0 不创建全部长期规划表，只创建 `system_settings` 和 `model_providers`。
- MVP 1 使用 `sources`、`source_files`、`import_jobs` 支撑路径扫描和源文件归集。
- MVP 2 使用 `obsidian_notes` 支撑 Source Note 写入和 Obsidian 打开。
- MVP 3 才引入 AI、`source_contents`、`agent_runs`、`agent_steps`、`review_items`。
- MVP 4 才引入 MCP SDK 和 MCP 相关表。
- 文件扫描安全、路径白名单、符号链接、hash、复制原子性必须在 MVP 1 编码前落实。

## 1. MVP 目标

MVP 目标是先解决用户最核心的现实痛点：

> 本地资料太乱，需要先把散落源文件归集整理起来，并打通最小 Obsidian 归档闭环。

MVP 不追求完整知识运行系统，不做复杂多 Agent、向量库、在线文档连接器、办公室等距视图和个人记录完整处理。

MVP 核心闭环：

```text
指定本地路径
  -> 扫描文件
  -> 复制归集到 WikiForge_RawSources
  -> 建立 MySQL 索引
  -> 选择少量文件进入处理
  -> 生成 Source Note Markdown 草案
  -> 人工审核
  -> 写入 Obsidian Vault
  -> Web UI 可查看状态并打开 Obsidian 文件
```

## 2. MVP 范围

### 2.1 必须实现

- 本地路径扫描。
- 源文件复制归集。
- 文件 hash 去重。
- 文件类型识别。
- MySQL 索引。
- Obsidian Vault 目录初始化。
- Source Note Markdown 生成。
- Markdown 预览。
- `obsidian://open` 打开链接。
- 基础 Web UI。
- 人工审核后归档。
- 单 LLM 多步骤 AI 辅助整理，放到 MVP 3，在 MVP 1/2 验收后启动。

### 2.2 暂不实现

- 飞书 / 腾讯文档自动读取。
- OpenClaw / Hermes 自动写入。
- 完整 MCP Server。
- 个人记录完整处理。
- 向量库。
- hybrid search。
- 办公室等距视图。
- Lint / Maintain Agent。
- 音视频多模态解析。
- 登录和权限系统。

### 2.3 只预留

- MCP tool schema。
- Personal Record 数据模型。
- Vector Export 数据模型。
- 办公室视图状态模型。
- 在线文档连接器接口。
- 代码仓库 Source Parser / 代码知识图谱能力，仅作为 V2+ 后续规划预留，不进入 MVP1/MVP2。

## 3. 实施阶段

### MVP 0：项目骨架

目标：系统可以本地启动。

后端：

- Spring Boot 3.x Maven monorepo 工程。
- `wikiforge-common` 公共模块。
- `wikiforge-core-service` 核心业务 API 服务。
- `wikiforge-worker-service` 文件扫描与归集任务服务。
- `wikiforge-gateway` 目录可预留，但 MVP 0/1 不强制运行。
- Java 21 LTS，兼容 Java 17。
- Maven 构建。
- MySQL 连接。
- Flyway 数据库迁移。
- MyBatis-Plus 3.5.x。
- Spring Validation。
- springdoc-openapi。
- Actuator 健康检查。

前端：

- Vue 3。
- Vite。
- TypeScript。
- Element Plus。
- Pinia。
- Vue Router。
- Axios。

配置：

- MySQL 连接配置。
- Obsidian Vault 路径。
- Raw Sources 路径。
- 模型供应商配置。
- 文件扫描允许根路径。
- `.env.example`。
- Docker Compose 配置。
- GitHub Actions CI 配置。

验收：

- Core Service 可启动。
- Worker Service 可启动。
- 前端可启动。
- MySQL 可连接。
- Core / Worker 的 `/actuator/health` 正常。
- 可在 Web UI 保存基础系统配置。
- `mvn test`、`mvn package` 可运行。
- `npm ci`、`npm run build` 可运行。
- Flyway 可以在空 MySQL 库上完成 migration。
- Docker Compose 可以启动 core-service、worker-service、frontend、mysql，并通过基础健康检查。

### MVP 1：源文件归集

目标：把散落文件复制到统一 Raw Sources 目录，并建立索引。

模块：

- Core Service：Import Job API、Source API、SourceFile 状态管理。
- Worker Service：Import Worker、Raw Source Organizer、File Hash Service、File Type Detector。

功能：

- 用户配置扫描路径。
- 用户通过 Core Service 创建路径扫描任务。
- Core Service 创建 ImportJob 并调用 Worker Service。
- Worker Service 扫描文件。
- Worker Service 识别文件类型和大小。
- Worker Service 计算 hash。
- Worker Service 复制文件到 `WikiForge_RawSources`。
- 重复文件不重复复制。
- Core Service 管理 `sources`、`source_files`、`import_jobs`。
- Web UI 展示扫描任务和文件列表。

Raw Sources 目录：

```text
WikiForge_RawSources/
  00_Inbox_待整理/
  01_Documents_文档/
  02_Images_图片/
  03_PDFs_PDF/
  04_WebClips_网页收藏/
  05_ProjectFiles_项目文件/
  06_Exports_平台导出/
  90_Unknown_待确认/
```

文件归集规则：

- 默认复制，不移动、不删除原文件。
- 同 hash 文件只复制一份，其他记录为 duplicate。
- 同名不同 hash 文件追加短 hash 后缀。
- 无法判断类型的文件进入 `90_Unknown_待确认`。
- 隐藏文件、系统文件、临时文件默认跳过。
- 大文件默认只建索引，不解析正文。

建议大文件阈值：

- MVP 默认 100 MB。
- 超过阈值：`raw_text_saved = false`，`raw_text_policy = skip_large_file`。

验收：

- 能扫描一个指定文件夹。
- 能复制 Word / PDF / Markdown / JPG 文件到 Raw Sources。
- 能识别重复文件。
- 能在 UI 中看到导入任务结果。
- 能查看原路径、新路径、hash、文件类型、处理状态。

### MVP 2：Obsidian Source Note

目标：把整理后的 Source 生成可读 Markdown，并写入 Obsidian Vault。

模块：

- Obsidian Writer。
- Obsidian Preview。
- Source Note Template Engine。

功能：

- 初始化 Obsidian Vault 目录。
- 为 Source 生成 Source Note 草案。
- 支持 Markdown 预览。
- 支持一键打开 Obsidian。
- 写入 `obsidian_notes`。
- 维护 `90_System_系统/Indexes_索引/index.md`。
- 维护 `90_System_系统/AgentLogs_Agent日志/log.md`。

Obsidian Vault 目录：

```text
WikiForge_Vault/
  00_Inbox_收集箱/
    Sources_来源/
    Review_审核/
  01_Projects_项目/
  02_Areas_领域/
  03_Resources_资源/
    Topics_主题/
    Entities_实体/
    References_参考/
  04_Archives_归档/
  05_Actions_行动/
  06_Secrets_敏感资产/
  07_Records_个人记录/
  90_System_系统/
    AgentLogs_Agent日志/
    Templates_模板/
    Indexes_索引/
    Schemas_规范/
```

MVP Source Note 最小 frontmatter：

```yaml
---
id: source_20260522_000001
type: source
title: ""
source_type: file
source_url: ""
source_platform: local
imported_from: path_scan
original_path: ""
managed_path: ""
content_hash: ""
status: pending_review
risk_level: low
priority: normal
projects: []
topics: []
tags: []
summary: ""
review_required: true
created_at: 2026-05-22
updated_at: 2026-05-22
---
```

MVP Source Note 正文模板：

```markdown
# 标题

## 摘要

待生成或人工补充。

## 来源

- 原始路径：
- 归集路径：
- 文件类型：
- 内容 Hash：

## 分类

- 项目：
- 主题：
- 标签：

## 关键内容

待生成或人工补充。

## 处理记录

- 导入时间：
- 处理状态：
- 审核状态：
```

验收：

- 能为 Source 生成 Markdown 草案。
- 能在 UI 内预览 Markdown。
- 能写入 Obsidian Vault。
- 能通过 `obsidian://open` 打开文件。
- 能在 index/log 中看到基础记录。

### MVP 3：AI 辅助整理

目标：用单 LLM 多步骤辅助摘要、分类和知识卡生成，但必须人工审核后入库。

模块：

- Model Provider Adapter。
- Agent Orchestrator。
- Review Service。

轻量 Agent 步骤：

```text
Normalize
  -> Classify
  -> Summarize
  -> Generate Source Note
  -> Review Decision
```

功能：

- 支持 DeepSeek / MiniMax / OpenAI-compatible Provider。
- 支持模型配置。
- 对文本类文件生成摘要。
- 生成项目、主题、标签建议。
- 生成 Source Note Markdown 草案。
- 写入 `agent_runs` 和 `agent_steps`。
- 进入 `review_items`。
- 用户确认后写入 Obsidian。

AI 输出要求：

- 优先 JSON 结构化输出。
- 输出失败进入 failed 状态。
- 低置信度进入人工审核。
- 不直接覆盖已确认 Obsidian 页面。

验收：

- 能对一个 Markdown 或 Word 文档生成摘要。
- 能生成标签/分类建议。
- 能生成 Source Note 草案。
- 能在审核页批准或驳回。
- 审核通过后写入 Obsidian。

### MVP 4：轻量 MCP 预览版

目标：为后续 OpenClaw / Hermes 接入打基础，不阻塞 MVP 0-3。

功能：

- MCP Server 基础框架。
- `create_source`
- `search_sources`
- `get_source`
- `create_personal_record` 简化存储。
- MCP 调用日志。

验收：

- 外部 MCP Client 能调用基础工具。
- 调用记录写入 `mcp_tool_calls`。

### MVP 5+：代码知识图谱与代码类资料治理

目标：在基础资料整理、Obsidian Source Note、AI 辅助整理和轻量 MCP 跑通后，再把代码仓库作为一种 Source 纳入 WikiForge。

参考项目：

- colbymchenry/codegraph：https://github.com/colbymchenry/codegraph

后续能力：

- 指定本地代码仓库路径并建立代码类 Source。
- 使用 tree-sitter 或 Java 生态可维护方案解析文件、类、函数、调用、导入、继承等结构。
- 将代码实体和关系写入 MySQL 关系层，后续可同步到图谱或向量库。
- 生成代码仓库 Source Note、项目结构摘要和关键模块说明。
- 通过 WikiForge MCP 暴露 `search_code_sources`、`get_code_symbol`、`get_code_relationships`、`get_project_structure` 等工具。
- 为 Codex / Claude / Cursor / Hermes 等开发 Agent 提供可查询的项目上下文。

阶段边界：

- 不替代 MVP1 的源文件归集。
- 不阻塞 MVP2 的 Obsidian Source Note。
- 不把 codegraph 直接作为核心依赖；优先参考其 MCP 工具设计、tree-sitter 解析思路和 nodes / relationships 建模方式。

## 4. MVP 页面清单

### 4.1 系统设置页

字段：

- Obsidian Vault 路径。
- Raw Sources 路径。
- 允许扫描路径。
- MySQL 连接状态。
- 模型供应商配置。
- 默认模型。

操作：

- 保存配置。
- 测试路径。
- 初始化 Vault。
- 初始化 Raw Sources。
- 测试模型连接。

### 4.2 导入任务页

字段：

- 任务 ID。
- 扫描路径。
- 导入模式。
- 状态。
- 总文件数。
- 成功数。
- 跳过数。
- 失败数。
- 开始时间。
- 完成时间。

操作：

- 创建扫描任务。
- 查看任务详情。
- 重试失败任务。

### 4.3 文件列表页

字段：

- 标题 / 文件名。
- 文件类型。
- 原路径。
- 归集路径。
- hash。
- 文件大小。
- 去重状态。
- 处理意图。
- 状态。

筛选：

- 文件类型。
- 状态。
- 是否重复。
- 是否已生成 Source Note。
- 扫描任务。

操作：

- 查看详情。
- 进入 AI 辅助整理。
- 生成 Source Note。
- 打开归集文件所在目录。

### 4.4 Source 详情页

展示：

- 元数据。
- 原路径 / 归集路径。
- 摘要。
- 标签 / 主题 / 项目建议。
- 关联 Obsidian Note。
- Agent 处理日志。

操作：

- 生成 / 重新生成草案。
- 进入审核。
- 打开 Obsidian。

### 4.5 审核队列页

字段：

- Source。
- 审核类型。
- 摘要。
- Markdown 草案。
- Agent 建议。
- 置信度。
- 审核原因。

操作：

- 批准。
- 驳回。
- 修改后批准。

### 4.6 Markdown 预览页

功能：

- 渲染 Obsidian Markdown。
- 展示 frontmatter。
- 展示文件路径。
- 一键打开 Obsidian。

## 5. MVP API 清单

### 5.1 Settings API

- `GET /api/settings`
- `PUT /api/settings`
- `POST /api/settings/test-path`
- `POST /api/settings/init-vault`
- `POST /api/settings/init-raw-sources`
- `POST /api/settings/test-model`

### 5.2 Import Job API

- `POST /api/import-jobs`
- `GET /api/import-jobs`
- `GET /api/import-jobs/{id}`
- `POST /api/import-jobs/{id}/start`
- `POST /api/import-jobs/{id}/retry-failed`

### 5.3 Source API

- `GET /api/sources`
- `GET /api/sources/{id}`
- `PUT /api/sources/{id}`
- `POST /api/sources/{id}/generate-note-draft`
- `POST /api/sources/{id}/run-ai-organize`

### 5.4 Obsidian API

- `POST /api/obsidian/init`
- `GET /api/obsidian/notes/{id}`
- `GET /api/obsidian/notes/{id}/preview`
- `POST /api/obsidian/notes/{id}/write`
- `GET /api/obsidian/notes/{id}/open-uri`

### 5.5 Review API

- `GET /api/review-items`
- `GET /api/review-items/{id}`
- `POST /api/review-items/{id}/approve`
- `POST /api/review-items/{id}/reject`
- `POST /api/review-items/{id}/approve-with-edits`

### 5.6 Agent Run API

- `GET /api/agent-runs`
- `GET /api/agent-runs/{id}`
- `GET /api/agent-runs/{id}/steps`

## 6. MVP 数据表

MVP 必需：

- `sources`
- `source_files`
- `import_jobs`
- `obsidian_notes`
- `agent_runs`
- `agent_steps`
- `review_items`
- `model_providers`
- `system_settings`

MVP 预留：

- `content_chunks`
- `embedding_jobs`
- `mcp_servers`
- `mcp_tool_calls`
- `personal_records`
- `agent_office_status`

## 7. 错误处理策略

### 文件扫描失败

- 记录失败路径。
- 不中断整个任务。
- 增加 `failed_count`。

### 文件复制失败

- Source/File 状态为 failed。
- 记录错误信息。
- 支持重试。

### 重复文件

- 不重复复制。
- `organize_status = duplicate`。
- 记录 `duplicate_of_file_id`。

### 文档解析失败

- 保留文件索引。
- `parse_status = failed` 或 `partial`。
- 允许后续手动处理。

### AI 调用失败

- 记录 agent step failed。
- 支持重试。
- 不影响源文件归集结果。

### Obsidian 写入失败

- 不修改 Source 为 archived。
- 保留草案。
- 进入审核/失败状态。

## 8. 安全边界

- 文件扫描路径必须由用户显式配置。
- 不扫描系统盘根目录。
- 不扫描隐藏目录，除非用户显式开启。
- 防止路径穿越。
- 默认复制，不移动、不删除源文件。
- API Key 不写入代码仓库。
- 无登录模式下默认绑定 localhost。
- 敏感文件只标记，不默认提炼明文内容。

## 9. 验收用例

### 用例 1：扫描普通文件夹

给定一个包含 Word、PDF、Markdown、JPG 的文件夹，系统能扫描并创建导入任务。

验收：

- import job completed。
- 文件记录写入 source_files。
- Source 记录写入 sources。

### 用例 2：重复文件去重

给定两个路径不同但内容相同的文件，系统只复制一份。

验收：

- 一个文件 copied。
- 一个文件 duplicate。
- duplicate 记录指向原文件。

### 用例 3：生成 Source Note

选择一个已归集文件，生成 Source Note 草案。

验收：

- Markdown 包含 frontmatter。
- UI 可预览。
- 审核通过后写入 Obsidian。

### 用例 4：打开 Obsidian

对已写入 Note 点击打开。

验收：

- 返回合法 `obsidian://open` URI。
- 本地 Obsidian 可打开对应文件。

### 用例 5：AI 辅助整理

选择一个 Markdown 或 Word 文档，运行 AI 辅助整理。

验收：

- 生成摘要。
- 生成标签建议。
- 生成草案。
- agent_runs / agent_steps 有记录。
- review_items 有待审核记录。

## 10. 开发顺序建议

1. 后端 Maven monorepo 骨架。
2. `wikiforge-common`、`wikiforge-core-service`、`wikiforge-worker-service` 模块拆分。
3. MySQL + Flyway 初始化。
4. system_settings / model_providers。
5. Raw Sources 和 Vault 路径配置。
6. Core / Worker 基础健康检查和 Docker Compose。
7. Core Import Job API。
8. Worker 文件扫描。
9. Worker 文件复制归集 + hash 去重。
10. Source/File 列表 UI。
11. Obsidian Vault 初始化。
12. Source Note 模板和预览。
13. Review 队列。
14. Model Provider Adapter。
15. AI 辅助整理。
16. Obsidian 写入和打开。
17. MVP 验收测试。
