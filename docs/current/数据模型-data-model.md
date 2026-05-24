# 知识熔炉 WikiForge 数据模型 v1.0

## 0. 架构评审后的 MVP 数据模型收敛结论

本章是 2026-05-23 架构评审后的执行结论，用于指导 MVP 0/1/2 的实际 Flyway DDL。

长期数据模型仍保留 LLM Wiki + GBrain 的完整演进方向，但 MVP DDL 必须收敛，避免一次性创建大量暂不使用的表和字段。

### 0.1 MVP Flyway 分段

Flyway 不在第一个 migration 中创建全部长期规划表。

阶段建议：

| 阶段 | 建表范围 | 服务归属 | 说明 |
| --- | --- | --- | --- |
| MVP 0 | `system_settings`、`model_providers` | Core Service | 工程骨架、配置、模型供应商预留 |
| MVP 1 | `sources`、`source_files`、`import_jobs` | Core Service 管理，Worker Service 执行任务并回写结果 | 本地路径扫描、复制归集、hash 去重、索引 |
| MVP 2 | `obsidian_notes` | Core Service | Vault 初始化、Source Note 写入、Markdown 预览和打开 |
| MVP 3 | `source_contents` | Core Service 管理，Worker Service 解析并回写 | Markdown / TXT / PDF / Word 正文抽取、hash、解析状态和 Source Note 摘录 |
| MVP 4 | `agent_runs`、`agent_steps`、`review_items` | Core Service，后续可拆 Agent Service | AI 辅助整理和审核队列 |
| MVP 5 | `mcp_tool_calls`、`personal_records` | Core Service，后续可拆 MCP / Record Service | 轻量 MCP HTTP Preview、调用日志、个人记录最小写入 |
| V1 | `personal_records` 扩展归档字段，复用 `sources/source_files/source_contents` | Core Service，后续可拆 Link / Record Service | 链接资料收集、个人记录 REST API、个人记录 Obsidian 归档 |
| V2 / R6-1 | `vector_export_jobs`、`content_chunks` | Core Service，后续可拆 Vector Service | JSONL chunk 导出契约、embedding 状态预留 |
| V2 / R6-3.1 | `knowledge_maintenance_runs`、`knowledge_maintenance_items` | Core Service，后续可拆 Agent / Maintain Service | 知识库体检运行账本、问题列表和人工处理闭环 |
| V2 后续 | `mcp_servers`、`embedding_jobs`、办公室视图相关表 | MCP / Vector / Agent Service | 完整 MCP 配置、真实向量库、混合检索和运行层 |

### 0.1.1 服务归属原则

MVP 采用少服务微服务模式：

- Core Service 是数据控制平面，负责 Source、ImportJob、ObsidianNote、ReviewItem 等用户可见状态。
- Worker Service 是任务执行平面，负责扫描、复制、hash、解析等耗时操作。
- MVP 阶段可以共享 MySQL 实例，但每张表必须声明服务归属。
- Worker Service 不直接提供 UI 查询表；用户查询必须通过 Core Service。
- 后续新表必须先补充服务归属，再进入 Flyway migration。

### 0.2 sources 表收敛

MVP 阶段 `sources` 是高频列表查询表，不直接承载大文本正文。

执行规则：

- `raw_text` 不进入 MVP 1/2 的核心读写路径。
- MVP 1/2 只保存文件路径、hash、摘要、状态和基础元数据。
- MVP 3 如需要保存解析文本，使用独立 `source_contents` 表。
- AI 处理时优先从 `source_files.managed_path` 读取原文件内容。
- 在线文档连接器字段不进入 MVP 1 的 Flyway DDL，V1 再通过 `ADD COLUMN` 补充。

### 0.3 Source 与 Source File 关系

MVP 阶段采用：

```text
1 Source = 1 Source File
```

执行规则：

- `source_files.source_id` 添加唯一约束。
- 多附件、多链接、多版本资料放到后续阶段扩展。
- `sources.raw_original_path` 和 `sources.raw_managed_path` 是 MVP 列表展示和快速查询冗余字段。
- `source_files.original_path` 和 `source_files.managed_path` 是文件级真实记录，以 `source_files` 为准。

### 0.4 状态枚举

`sources.status` 在 MVP 中统一为：

```text
pending
organized
processing
pending_review
archived
rejected
failed
```

含义：

- `pending`：已发现或已创建，尚未归集。
- `organized`：文件已归集，可生成 Source Note。
- `processing`：AI 或后续处理流程中。
- `pending_review`：等待人工审核或确认。
- `archived`：已写入 Obsidian。
- `rejected`：用户拒绝归档。
- `failed`：处理失败。

### 0.5 MVP 必要索引

MVP DDL 至少包含：

- `sources.source_uid` unique
- `sources.content_hash`
- `sources.status, sources.collected_at`
- `source_files.source_id` unique
- `source_files.content_hash`
- `source_files.import_job_id`
- `source_files.organize_status`
- `import_jobs.job_uid` unique
- `import_jobs.status, import_jobs.created_at`
- `obsidian_notes.source_id`
- `obsidian_notes.vault_path`

### 0.6 暂不进入 MVP 0/1 的长期能力

以下能力只保留文档设计，不进入 MVP 0/1 的实际业务代码：

- 在线文档连接器字段。
- MCP Server / Client 表。
- 向量库分块与 embedding 表。
- 个人记录表。
- 办公室视图状态表。
- 复杂项目、主题、实体多对多关联。

## 1. 设计目标

数据模型需要支持：

- 多来源资料管理。
- Agent 流水线状态追踪。
- Obsidian 文件映射。
- Obsidian 文件预览和打开。
- HTML 预览和报告 artifact。
- 后续批量导入向量库所需的内容分块和 embedding 状态。
- 办公室视图 Agent 状态。
- MCP Server / Client 配置和工具调用日志。
- 个人记录对象，包括消费、账单、邮件、人际关系和个人事件。
- 项目、主题、实体、标签多维关联。
- 审核队列。
- 指定路径导入和文件去重。
- 源文件归集整理，记录原路径、新路径、复制状态和整理状态。
- 在线文档链接采集，记录平台、连接器、权限状态和读取结果。
- 后续多 Agent、私有部署和团队化扩展。

数据库第一版使用 MySQL。

MySQL 定位为控制平面、索引库、Agent 流程账本和轻量内容缓存，不作为最终知识正文主库。

实现层面主要面向 Java 技术栈，表结构应便于 MyBatis-Plus / MyBatis 映射和维护。字段命名使用 snake_case，业务层对象可映射为 Java camelCase。

## 2. 核心实体关系

```text
sources
  |-- source_files
  |-- source_contents
  |-- source_tags
  |-- source_projects
  |-- source_topics
  |-- source_entities
  |-- source_actions
  |-- agent_runs
  |-- review_items
  |-- obsidian_notes
  |-- artifacts
  |-- content_chunks
  |-- embedding_jobs
  |-- mcp_servers
  |-- mcp_tool_calls
  |-- personal_records

projects
topics
entities
tags
actions
import_jobs
model_providers
agent_office_status
```

## 3. sources

保存每条原始资料的主记录。

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| source_uid | varchar(64) unique | 全局 Source ID |
| title | varchar(512) | 标题 |
| source_type | varchar(64) | webpage、video、pdf、word、markdown、image、chat、file |
| source_platform | varchar(128) | 知乎、B站、飞书、腾讯文档、微信、本地等 |
| source_url | text | 原始链接 |
| connector_name | varchar(128) | openclaw、hermes、native 等 |
| connector_status | varchar(64) | pending、success、permission_denied、invalid_url、connector_error、empty_content |
| connector_trace_id | varchar(128) | 连接器调用追踪 ID |
| local_path | text | 本地文件路径 |
| raw_original_path | text | 源文件原始路径 |
| raw_managed_path | text | 归集后的 Raw Sources 路径 |
| raw_organize_status | varchar(64) | pending、copied、duplicate、need_confirm、failed |
| processing_intent | varchar(64) | organize_only、extract_later、process_now |
| author | varchar(255) | 作者 |
| language | varchar(32) | 语言 |
| content_hash | varchar(128) | 内容哈希，用于去重 |
| raw_text | longtext | 分级保存的解析文本，可为空 |
| raw_text_saved | boolean | 是否保存了解析全文 |
| raw_text_policy | varchar(64) | save、skip_large_file、skip_sensitive、skip_binary、metadata_only |
| summary_short | text | 一句话摘要 |
| summary_structured | json | 结构化摘要 |
| status | varchar(64) | pending、processing、pending_review、archived、rejected、failed |
| risk_level | varchar(32) | low、medium、high |
| priority | varchar(32) | low、normal、high |
| confidence | decimal(5,4) | Agent 总体置信度 |
| review_required | boolean | 是否需要审核 |
| review_reason | text | 审核原因 |
| collected_at | datetime | 收集时间 |
| source_created_at | datetime null | 原资料创建时间 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

索引建议：

- `idx_sources_status`
- `idx_sources_type`
- `idx_sources_platform`
- `idx_sources_hash`
- `idx_sources_collected_at`

正文保存策略：

- 普通文本、网页、Markdown、小型 Word 可保存 `raw_text`。
- PDF、大文件、图片、敏感资料可不保存 `raw_text`，只保存路径、摘要、hash 和元数据。
- `raw_text_saved` 和 `raw_text_policy` 用于解释是否保存全文以及原因。

## 4. source_files

保存文件导入信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| source_id | bigint fk | 关联 sources |
| import_job_id | bigint fk | 关联导入任务 |
| file_name | varchar(512) | 文件名 |
| file_ext | varchar(32) | 扩展名 |
| file_path | text | 本地路径 |
| original_path | text | 原始路径 |
| managed_path | text | 归集后的规范路径 |
| file_size | bigint | 文件大小 |
| mime_type | varchar(128) | MIME 类型 |
| content_hash | varchar(128) | 文件哈希 |
| parser_name | varchar(128) | 使用的解析器 |
| parse_status | varchar(64) | pending、success、failed、partial |
| organize_status | varchar(64) | pending、copied、duplicate、need_confirm、failed |
| duplicate_of_file_id | bigint null | 重复文件 |
| parse_error | text | 解析错误 |
| created_at | datetime | 创建时间 |

## 4.1 source_contents

保存 Worker 从源文件中抽取出的正文内容。MVP3 支持 Markdown / TXT / PDF / Word 基础文本抽取，图片、扫描件和 OCR 放到后续阶段。

服务归属：

- Core Service：表结构、持久化、查询和 Source Note 草案读取。
- Worker Service：文件解析并通过内部批量提交接口回写正文字段。

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| content_uid | varchar(64) unique | 正文内容 ID |
| source_id | bigint fk | 关联 Source |
| source_file_id | bigint fk unique | 关联 Source File，MVP3 每个文件一条正文记录 |
| parser_name | varchar(128) | `markdown-text`、`plain-text`、`pdfbox-text`、`poi-docx-text` |
| content_type | varchar(64) | MVP3 默认为 `plain_text` |
| raw_text | longtext | 抽取后的纯文本正文 |
| text_hash | varchar(128) | 正文文本 hash |
| char_count | int | 正文字符数 |
| raw_text_saved | boolean | 是否保存正文 |
| parse_status | varchar(64) | pending、success、failed、partial |
| parse_error | text | 解析错误 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

索引：

- `uk_source_contents_content_uid`
- `uk_source_contents_source_file`
- `idx_source_contents_source`
- `idx_source_contents_parse_status`

## 5. import_jobs

保存导入任务，包括指定路径扫描。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| job_uid | varchar(64) unique | 导入任务 ID |
| import_type | varchar(64) | manual、upload、path_scan、url |
| input_path | text | 扫描路径 |
| input_url | text | 输入链接 |
| source_platform | varchar(128) | 在线来源平台 |
| connector_name | varchar(128) | 使用的连接器 |
| connector_status | varchar(64) | 读取状态 |
| organize_mode | varchar(64) | copy、move、index_only |
| status | varchar(64) | pending、running、completed、failed |
| total_count | int | 总文件数 |
| success_count | int | 成功数 |
| skipped_count | int | 跳过数 |
| failed_count | int | 失败数 |
| started_at | datetime | 开始时间 |
| finished_at | datetime | 完成时间 |
| created_at | datetime | 创建时间 |

## 6. projects

项目知识空间。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| name | varchar(255) | 项目名 |
| slug | varchar(255) unique | 唯一标识 |
| description | text | 描述 |
| status | varchar(64) | active、paused、archived |
| obsidian_path | text | Obsidian 项目页路径 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 7. topics

长期主题。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| name | varchar(255) | 主题名 |
| slug | varchar(255) unique | 唯一标识 |
| description | text | 描述 |
| parent_id | bigint null | 父主题 |
| obsidian_path | text | Obsidian 主题页路径 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 8. entities

实体库，包括人、公司、产品、工具、技术、概念。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| name | varchar(255) | 实体名 |
| entity_type | varchar(64) | person、company、product、tool、technology、concept |
| aliases | json | 别名 |
| description | text | 描述 |
| obsidian_path | text | Obsidian 实体页路径 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 9. tags

标签库。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| name | varchar(128) | 标签名 |
| slug | varchar(128) unique | 唯一标识 |
| tag_type | varchar(64) | system、user、agent |
| created_at | datetime | 创建时间 |

## 10. actions

行动项。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| title | varchar(512) | 任务标题 |
| action_type | varchar(64) | read、organize、confirm、practice、review |
| status | varchar(64) | todo、doing、done、cancelled |
| priority | varchar(32) | low、normal、high |
| due_at | datetime null | 截止时间 |
| obsidian_path | text | 对应 Obsidian 任务页 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 11. 多对多关联表

### source_projects

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| source_id | bigint fk | Source |
| project_id | bigint fk | Project |
| confidence | decimal(5,4) | 置信度 |
| created_by | varchar(64) | user、agent |

### source_topics

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| source_id | bigint fk | Source |
| topic_id | bigint fk | Topic |
| confidence | decimal(5,4) | 置信度 |
| created_by | varchar(64) | user、agent |

### source_entities

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| source_id | bigint fk | Source |
| entity_id | bigint fk | Entity |
| mention_text | varchar(255) | 原文提及 |
| confidence | decimal(5,4) | 置信度 |

### source_tags

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| source_id | bigint fk | Source |
| tag_id | bigint fk | Tag |
| created_by | varchar(64) | user、agent |

### source_actions

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| source_id | bigint fk | Source |
| action_id | bigint fk | Action |

## 12. agent_runs

一次 Source 处理流程的总记录。MVP4 首轮由 Core Service 管理，后续可拆到 Agent Service。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| run_uid | varchar(64) unique | 运行 ID |
| source_id | bigint fk | Source |
| source_file_id | bigint fk null | Source File |
| run_type | varchar(64) | MVP4 固定为 ai_review |
| pipeline_version | varchar(64) | 流水线版本 |
| status | varchar(64) | pending、running、completed、failed |
| current_step | varchar(64) | 当前 Agent 步骤 |
| model_provider | varchar(128) | 模型供应商 |
| model_name | varchar(128) | 模型名 |
| started_at | datetime | 开始时间 |
| finished_at | datetime | 结束时间 |
| final_decision | varchar(64) | auto_archive、need_review、reject、failed |
| error_message | text | 错误信息 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 13. agent_steps

记录每个 Agent 节点的执行输入和输出。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| step_uid | varchar(64) unique | 步骤 ID |
| run_id | bigint fk | 关联 agent_runs |
| source_id | bigint fk | 关联 sources |
| source_file_id | bigint fk null | 关联 source_files |
| step_name | varchar(64) | MVP4 首轮为 draft_review，后续扩展 ingest、normalize、classify、integrate、review、archive |
| agent_name | varchar(128) | Agent 名称 |
| status | varchar(64) | pending、running、completed、failed |
| input_json | json | 输入 |
| output_json | json | 输出 |
| model_provider | varchar(128) | 模型供应商 |
| model_name | varchar(128) | 模型名 |
| prompt_version | varchar(64) | Prompt 版本 |
| error_message | text | 错误信息 |
| started_at | datetime | 开始时间 |
| finished_at | datetime | 结束时间 |
| created_at | datetime | 创建时间 |

## 14. review_items

人工审核队列。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| review_uid | varchar(64) unique | 审核项 ID |
| source_id | bigint fk | Source |
| source_file_id | bigint fk null | Source File |
| run_id | bigint fk | Agent Run |
| review_type | varchar(64) | MVP4 首轮为 ai整理建议，后续扩展 classification、integration、risk、duplicate、conflict |
| status | varchar(64) | pending、approved、rejected、modified |
| reason | text | 进入审核原因 |
| suggested_changes_json | json | Agent 建议 |
| markdown_draft | longtext | Markdown 草案 |
| user_decision | text | 用户决策说明 |
| reviewed_at | datetime | 审核时间 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 15. obsidian_notes

记录数据库对象和 Obsidian 文件之间的映射。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| note_uid | varchar(64) unique | Note ID |
| source_id | bigint fk | Source 主键 |
| source_file_id | bigint fk null | Source File 主键 |
| note_type | varchar(64) | MVP2 固定为 source_note，后续扩展 project、topic、entity、action、log、index |
| vault_name | varchar(128) | Obsidian Vault 名称 |
| vault_path | varchar(1024) | Vault 内相对路径，索引使用 `vault_path(255)` 前缀 |
| absolute_path | varchar(2048) | 服务运行环境下的绝对路径 |
| obsidian_uri | varchar(2048) | `obsidian://open` 打开链接 |
| title | varchar(512) | 标题 |
| frontmatter_json | json | frontmatter |
| content_hash | varchar(128) | 文件内容哈希 |
| status | varchar(64) | written |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 16. wiki_integrations

记录 Source 被整合到哪些 Wiki 页面。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| source_id | bigint fk | Source |
| note_id | bigint fk | Obsidian Note |
| integration_type | varchar(64) | summary、quote、reference、section_update |
| section_title | varchar(255) | 被更新章节 |
| change_summary | text | 变更摘要 |
| created_by_agent_step_id | bigint fk | 由哪个 Agent Step 生成 |
| created_at | datetime | 创建时间 |

## 17. artifacts

记录 HTML 预览、HTML 报告等非主知识源产物。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| artifact_uid | varchar(64) unique | Artifact ID |
| artifact_type | varchar(64) | html_preview、html_report |
| related_type | varchar(64) | source、project、topic、entity、run |
| related_id | bigint | 关联对象 ID |
| title | varchar(512) | 标题 |
| file_path | text | artifact 文件路径 |
| content_hash | varchar(128) | 内容哈希 |
| generated_by_agent_step_id | bigint null | 生成来源 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

规则：

- Markdown / Obsidian Wiki 是主知识源。
- HTML 只用于 Web UI 预览和报告导出。
- HTML artifact 可以重新生成，不作为唯一事实来源。

## 18. agent_office_status

办公室视图的 Agent 工位状态。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| agent_key | varchar(64) unique | ingest、normalize、classify、integrate、review、archive |
| agent_name | varchar(128) | 展示名称 |
| office_status | varchar(64) | idle、running、waiting_review、blocked、failed、completed |
| current_run_id | bigint null | 当前运行 |
| current_step_id | bigint null | 当前步骤 |
| current_source_id | bigint null | 当前资料 |
| last_error | text | 最近错误 |
| last_heartbeat_at | datetime | 最近心跳 |
| metadata_json | json | UI 展示元数据 |
| updated_at | datetime | 更新时间 |

办公室视图作为 V2 拓展能力，预留 6 个 Agent：

- ingest
- normalize
- classify
- integrate
- review
- archive

## 19. vector_export_jobs

记录 R6-1 向量导出任务。首版导出格式为 JSONL，不接真实向量库；当前用户主流程不展示向量导出入口，后续仅作为内部管道或高级能力评估。

对应 migration：

```text
V20260524_002__create_vector_export_tables.sql
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| export_uid | varchar(64) unique | 导出任务 ID |
| scope | varchar(64) | all、sources、personal_records |
| target_collection | varchar(128) | 目标向量集合名称 |
| export_format | varchar(32) | 当前固定为 jsonl |
| status | varchar(64) | completed、failed；后续可扩展 running |
| total_count | int | 导出的 chunk 数量，不是文档数量 |
| export_file_name | varchar(255) null | 文件名 |
| export_relative_path | varchar(1024) null | 相对 `vector_export_root` 的路径，不存本机绝对路径 |
| error_message | text null | 错误信息 |
| created_at | datetime | 创建时间 |
| finished_at | datetime null | 完成时间 |

## 20. content_chunks

保存后续向量化所需的文本分块。第一版可以先生成和管理分块，不要求接入具体向量数据库。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| chunk_uid | varchar(64) unique | 分块 ID |
| export_uid | varchar(64) | 所属导出任务 ID |
| content_type | varchar(64) | source_content、personal_record |
| source_uid | varchar(64) null | 来源 Source UID |
| file_uid | varchar(64) null | 来源 Source File UID |
| record_uid | varchar(64) null | 个人记录 UID |
| title | varchar(512) null | 标题 |
| chunk_index | int | 同一文档内分块序号 |
| chunk_text | longtext | 分块文本 |
| text_hash | varchar(128) | 分块文本 SHA-256 |
| char_count | int | 字符数 |
| token_estimate | int | 粗略估算 token 数 |
| metadata_json | json | 来源、标题、路径、标签等元数据 |
| embedding_status | varchar(64) | pending、embedded、stale、failed |
| target_collection | varchar(128) | 目标向量集合 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 20.1 knowledge_maintenance_runs

记录 R6-3 知识库体检运行。首版为手动触发，不自动修改用户资料，不做定时任务。

对应 migration：

```text
V20260524_003__create_knowledge_maintenance_tables.sql
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| run_uid | varchar(64) unique | 体检运行 ID |
| run_type | varchar(64) | 当前固定为 manual |
| status | varchar(64) | completed、failed；后续可扩展 running |
| stale_days | int | 判断未归档或待处理内容过期的天数阈值 |
| total_count | int | 本轮发现的问题总数 |
| issue_count | int | 本轮未关闭问题数，首版等于 total_count |
| error_message | text null | 错误信息 |
| started_at | datetime | 开始时间 |
| finished_at | datetime null | 完成时间 |
| created_at | datetime | 创建时间 |

## 20.2 knowledge_maintenance_items

记录每次知识库体检发现的问题。R6-3.1 增加人工处理闭环，支持已解决、忽略和重新打开；后续再扩展修复建议、自动修复和完整事件历史表。

对应 migration：

```text
V20260524_003__create_knowledge_maintenance_tables.sql
V20260524_004__extend_maintenance_items_workflow.sql
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| item_uid | varchar(64) unique | 问题 ID |
| run_uid | varchar(64) | 所属体检运行 ID |
| issue_type | varchar(64) | 当前 UI 暴露 missing_source_content、duplicate_source_content、unarchived_personal_record |
| severity | varchar(32) | high、medium、low |
| content_type | varchar(64) | source_content、personal_record 等 |
| source_uid | varchar(64) null | 关联 Source UID |
| file_uid | varchar(64) null | 关联 Source File UID |
| record_uid | varchar(64) null | 关联 Personal Record UID |
| chunk_uid | varchar(64) null | 历史预留 chunk UID，当前 UI 不展示 |
| export_uid | varchar(64) null | 历史预留 vector export UID，当前 UI 不展示 |
| title | varchar(512) null | 便于 UI 展示的标题 |
| summary | text | 问题摘要 |
| evidence_json | json null | 证据 JSON，例如 hash、数量、创建时间 |
| status | varchar(64) | open、resolved、ignored；重新打开时恢复 open |
| resolution_note | text null | 本次处理备注 |
| resolved_by | varchar(128) null | 本次处理人，Web UI 默认 web-ui |
| resolved_at | datetime null | 本次处理时间；重新打开时清空 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 21. embedding_jobs

记录后续真实批量向量化任务。R6-1 暂不建表，等待向量库和 embedding provider 选型后实现。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| job_uid | varchar(64) unique | 任务 ID |
| job_type | varchar(64) | export_only、embed、reembed |
| target_collection | varchar(128) | 目标集合 |
| status | varchar(64) | pending、running、completed、failed |
| total_chunks | int | 总分块数 |
| success_count | int | 成功数 |
| failed_count | int | 失败数 |
| model_provider | varchar(128) | embedding 模型供应商 |
| model_name | varchar(128) | embedding 模型 |
| error_message | text | 错误信息 |
| started_at | datetime | 开始时间 |
| finished_at | datetime | 完成时间 |
| created_at | datetime | 创建时间 |

## 22. model_providers

模型供应商配置。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| provider_name | varchar(128) | DeepSeek、MiniMax 等 |
| provider_type | varchar(64) | openai_compatible、custom |
| base_url | text | API Base URL |
| default_model | varchar(128) | 默认模型 |
| enabled | boolean | 是否启用 |
| config_json | json | 额外配置，不保存明文密钥 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 23. mcp_servers

记录 MCP Server / Client 相关配置。MVP5 HTTP Preview 使用固定内置 server，不依赖动态 server 配置表；该表后续在完整 MCP Server / Client 阶段引入。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| server_uid | varchar(64) unique | MCP Server ID |
| name | varchar(128) | 名称 |
| server_type | varchar(64) | internal、external |
| transport_type | varchar(64) | stdio、sse、http |
| endpoint | text | 外部 MCP Server 地址或启动配置 |
| enabled | boolean | 是否启用 |
| tools_json | json | 工具定义快照 |
| config_json | json | 配置信息，不保存明文密钥 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 24. mcp_tool_calls

记录 MCP 工具调用日志。MVP5 对应 migration 编号冻结为：

```text
V20260523_006__create_mcp_preview_tables.sql
```

敏感输入和输出必须脱敏：`rawContent`、`markdown`、`structured` 不写入调用日志原文。R4-4 已对 `get_obsidian_note` 的 Markdown 输出、`create_personal_record` 的原始内容和结构化字段做脱敏落库。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| call_uid | varchar(64) unique | 调用 ID |
| tool_name | varchar(128) | 工具名 |
| caller_type | varchar(64) | user、agent、external_agent、system |
| caller_id | varchar(128) | 调用方 ID |
| input_json | json | 脱敏后的输入参数 |
| output_json | json | 脱敏后的输出摘要 |
| status | varchar(64) | completed、failed |
| error_code | varchar(64) | 错误码 |
| error_message | text | 错误信息 |
| duration_ms | bigint | 执行耗时 |
| created_at | datetime | 创建时间 |

## 25. personal_records

保存非文档型个人记录，例如消费、账单、邮件、人际关系、个人事件和普通笔记。MVP5 R4-4 已通过 `create_personal_record` 做最小结构化写入。V1 扩展为 REST API + Web UI + Obsidian 归档闭环，仍不做 AI 总结和定时重组。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| record_uid | varchar(64) unique | 记录 ID |
| record_type | varchar(64) | expense、bill、email、relationship、event、note |
| title | varchar(512) | 标题 |
| occurred_at | datetime null | 发生时间 |
| source_channel | varchar(128) | web_ui、openclaw、hermes、mcp、email_connector 等 |
| source_ref | text | 来源引用，如邮件 ID、账单 ID、机器人 trace ID |
| raw_content | longtext | 原始记录内容 |
| structured_json | json | 结构化字段 |
| status | varchar(64) | pending、classified、summarized、archived、failed |
| sensitivity_level | varchar(32) | low、medium、high |
| created_by | varchar(128) | user、agent、openclaw、hermes |
| obsidian_vault_path | varchar(1024) null | V1 归档后的 Vault 相对路径，不存放给外部返回的本机绝对路径 |
| obsidian_uri | varchar(2048) null | `obsidian://open` 打开链接 |
| archived_at | datetime null | 写入 Obsidian 的时间 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 26. system_settings

系统配置。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| setting_key | varchar(128) unique | 配置键 |
| setting_value | json | 配置值 |
| description | text | 描述 |
| updated_at | datetime | 更新时间 |

关键配置：

- `obsidian_vault_path`
- `mysql_connection`
- `cc_switch_config`
- `default_model_provider`
- `default_model_name`
- `auto_archive_threshold`
- `path_scan_roots`
- `vault_name`
- `html_artifact_root`
- `raw_text_save_policy`
- `vector_export_root`
- `default_vector_collection`
- `embedding_provider`
- `mcp_server_enabled`
- `mcp_transport_type`
- `mcp_allowed_tools`
- `personal_record_default_sensitivity`

## 27. 状态枚举

### Source status

- pending
- organized
- organize_only
- processing
- normalized
- classified
- pending_review
- archived
- rejected
- failed

### Risk level

- low
- medium
- high

### Review status

- pending
- approved
- rejected
- modified

### Agent step status

- pending
- running
- completed
- failed

### Agent office status

- idle
- running
- waiting_review
- blocked
- failed
- completed

### Artifact type

- html_preview
- html_report

### Raw organize status

- pending
- copied
- duplicate
- need_confirm
- failed

### Connector status

- pending
- success
- permission_denied
- invalid_url
- connector_error
- empty_content

### Processing intent

- organize_only
- extract_later
- process_now

### Embedding status

- not_ready
- pending
- embedded
- stale
- failed

### MCP server type

- internal
- external

### MCP call status

- completed
- failed

### Personal record type

- expense
- bill
- email
- relationship
- event
- note

### Personal record status

- pending
- classified
- summarized
- archived
- failed

## 28. 数据模型原则

- Source 是所有资料的中心对象。
- 源文件整理是第一版核心能力，知识提炼可以在整理后再触发。
- Obsidian Note 是知识沉淀文件映射，不替代 Source。
- MySQL 是控制平面和索引库，不是最终知识正文主库。
- 数据模型优先兼容 Java + MyBatis-Plus / MyBatis 的主流实现方式。
- 解析正文采用分级保存策略。
- Agent 输出必须结构化保存，不能只写入 Markdown。
- HTML 产物是预览和报告，不是唯一事实来源。
- 向量库是后续规划，第一版需要保留分块、hash、embedding 状态和导出能力。
- MCP 是系统扩展层，需要记录工具配置和调用日志。
- 个人记录是核心对象之一，需要保留原始内容、结构化字段、来源和敏感级别。
- 所有关联关系都要保存置信度和创建来源。
- 人工审核结果优先于 Agent 建议。
- 敏感资产第一版只做标记和审核，不做加密。
- 向量检索不是第一版强依赖，但需要预留批量导入向量库的数据基础。
