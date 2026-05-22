# 知识熔炉 WikiForge 数据模型 v0.2

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

一次 Source 处理流程的总记录。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| run_uid | varchar(64) unique | 运行 ID |
| source_id | bigint fk | Source |
| pipeline_version | varchar(64) | 流水线版本 |
| status | varchar(64) | pending、running、completed、failed |
| current_step | varchar(64) | 当前 Agent 步骤 |
| office_status | varchar(64) | idle、running、waiting_review、blocked、failed、completed |
| started_at | datetime | 开始时间 |
| finished_at | datetime | 结束时间 |
| final_decision | varchar(64) | auto_archive、need_review、reject、failed |
| error_message | text | 错误信息 |
| created_at | datetime | 创建时间 |

## 13. agent_steps

记录每个 Agent 节点的执行输入和输出。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| run_id | bigint fk | 关联 agent_runs |
| source_id | bigint fk | 关联 sources |
| step_name | varchar(64) | ingest、normalize、classify、integrate、review、archive |
| agent_name | varchar(128) | Agent 名称 |
| status | varchar(64) | pending、running、completed、failed |
| input_json | json | 输入 |
| output_json | json | 输出 |
| model_provider | varchar(128) | 模型供应商 |
| model_name | varchar(128) | 模型名 |
| prompt_version | varchar(64) | Prompt 版本 |
| token_usage | json | Token 用量 |
| office_visible | boolean | 是否展示在办公室视图 |
| error_message | text | 错误信息 |
| started_at | datetime | 开始时间 |
| finished_at | datetime | 结束时间 |
| created_at | datetime | 创建时间 |

## 14. review_items

人工审核队列。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| source_id | bigint fk | Source |
| run_id | bigint fk | Agent Run |
| review_type | varchar(64) | classification、integration、risk、duplicate、conflict |
| status | varchar(64) | pending、approved、rejected、modified |
| reason | text | 进入审核原因 |
| suggested_changes | json | Agent 建议 |
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
| note_type | varchar(64) | source、project、topic、entity、action、log、index |
| related_type | varchar(64) | source、project、topic、entity、action |
| related_id | bigint | 关联对象 ID |
| title | varchar(512) | 标题 |
| vault_path | text | Vault 内相对路径 |
| absolute_path | text | 绝对路径 |
| obsidian_uri | text | `obsidian://open` 打开链接 |
| preview_enabled | boolean | 是否允许 Web UI 预览 |
| frontmatter_json | json | frontmatter |
| content_hash | varchar(128) | 文件内容哈希 |
| last_written_at | datetime | 最近写入时间 |
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

## 19. content_chunks

保存后续向量化所需的文本分块。第一版可以先生成和管理分块，不要求接入具体向量数据库。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| chunk_uid | varchar(64) unique | 分块 ID |
| related_type | varchar(64) | source、note、project、topic、entity |
| related_id | bigint | 关联对象 ID |
| source_id | bigint null | 来源 Source |
| note_id | bigint null | Obsidian Note |
| chunk_index | int | 分块序号 |
| chunk_text | longtext | 分块文本 |
| chunk_hash | varchar(128) | 分块哈希 |
| token_count | int | 估算 token 数 |
| metadata_json | json | 来源、标题、路径、标签等元数据 |
| embedding_status | varchar(64) | not_ready、pending、embedded、stale、failed |
| vector_collection | varchar(128) null | 目标向量集合 |
| vector_id | varchar(128) null | 向量库 ID |
| embedded_at | datetime null | 向量化时间 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 20. embedding_jobs

记录批量向量化任务。

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

## 21. model_providers

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

## 22. mcp_servers

记录 MCP Server / Client 相关配置。MVP 先预留配置和调用日志模型，MVP 4 再实现轻量 WikiForge MCP Server。

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

## 23. mcp_tool_calls

记录 MCP 工具调用日志。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| call_uid | varchar(64) unique | 调用 ID |
| server_id | bigint fk | MCP Server |
| tool_name | varchar(128) | 工具名 |
| caller_type | varchar(64) | user、agent、external_agent、system |
| caller_id | varchar(128) | 调用方 ID |
| related_source_id | bigint null | 关联 Source |
| related_run_id | bigint null | 关联 Agent Run |
| input_json | json | 输入参数 |
| output_json | json | 输出结果 |
| status | varchar(64) | pending、running、completed、failed |
| error_message | text | 错误信息 |
| started_at | datetime | 开始时间 |
| finished_at | datetime | 结束时间 |
| created_at | datetime | 创建时间 |

## 24. personal_records

保存非文档型个人记录，例如消费、账单、邮件、人际关系和个人事件。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint pk | 主键 |
| record_uid | varchar(64) unique | 记录 ID |
| record_type | varchar(64) | expense、bill、email、relationship、event |
| title | varchar(512) | 标题 |
| occurred_at | datetime | 发生时间 |
| source_channel | varchar(128) | web_ui、openclaw、hermes、mcp、email_connector 等 |
| source_ref | text | 来源引用，如邮件 ID、账单 ID、机器人 trace ID |
| raw_content | longtext | 原始记录内容 |
| structured_json | json | 结构化字段 |
| summary | text | Agent 生成摘要 |
| status | varchar(64) | pending、classified、summarized、archived、failed |
| sensitivity_level | varchar(32) | low、medium、high |
| related_project_id | bigint null | 关联项目 |
| related_entity_id | bigint null | 关联人/公司/实体 |
| obsidian_note_id | bigint null | 关联 Obsidian Note |
| created_by | varchar(128) | user、agent、openclaw、hermes |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 25. system_settings

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

## 26. 状态枚举

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

- pending
- running
- completed
- failed

### Personal record type

- expense
- bill
- email
- relationship
- event

### Personal record status

- pending
- classified
- summarized
- archived
- failed

## 27. 数据模型原则

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
