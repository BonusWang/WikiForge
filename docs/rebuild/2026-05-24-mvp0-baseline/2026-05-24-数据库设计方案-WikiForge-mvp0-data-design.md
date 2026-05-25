# 2026-05-24 WikiForge MVP0 数据库设计方案 MVP0 Data Design

## 1. 设计目标

MVP0 数据库从减法开始，只保存当前闭环必须的数据：

- 收纳任务。
- Raw Sources 文件账本。
- 正文抽取结果。
- Obsidian LLM Wiki 写入结果。
- 中文状态字典。

不为 MCP、向量、LifeOS、知识体检、Orchestration 或未来连接器预建表。

## 2. 最小表集合

| 表 | 状态 | 归属 | 用途 |
| --- | --- | --- | --- |
| `import_jobs` | 主流程 | Core | 路径扫描、上传任务账本 |
| `source_files` | 主流程 | Core | Raw Sources 文件账本 |
| `source_contents` | 主流程 | Core | 正文抽取结果 |
| `wiki_ingest_runs` | 主流程 | Core | Obsidian LLM Wiki 写入运行结果 |
| `system_dictionaries` | 主流程 | Core | 状态码、中文名称、说明和颜色映射 |

`wiki_ingest_runs` 和 `system_dictionaries` 已进入 MVP0 fresh schema；不为设置、模型配置或未来连接器预建额外表。

## 3. 表设计草案

### 3.1 `import_jobs`

| 字段 | 用途 |
| --- | --- |
| `id` | 自增主键 |
| `job_uid` | 对外任务 UID |
| `import_type` | 本地路径 / 浏览器上传 |
| `source_path_masked` | 脱敏来源路径或上传批次描述 |
| `raw_source_root` | Raw Sources 根目录标识 |
| `status_code` | 中文状态码，来自字典 |
| `total_count` | 文件总数 |
| `success_count` | 成功数 |
| `duplicate_count` | 重复数 |
| `failed_count` | 失败数 |
| `failure_reason` | 失败原因 |
| `created_at` / `updated_at` / `completed_at` | 时间字段 |

### 3.2 `source_files`

| 字段 | 用途 |
| --- | --- |
| `id` | 自增主键 |
| `file_uid` | 对外文件 UID |
| `job_uid` | 所属收纳任务 |
| `original_name` | 原始文件名 |
| `original_path_masked` | 脱敏来源路径 |
| `raw_source_relative_path` | Raw Sources 相对路径 |
| `content_hash` | SHA-256 |
| `file_size_bytes` | 文件大小 |
| `file_type` | 文件类型 |
| `collect_status_code` | 收纳状态中文码值 |
| `extract_status_code` | 抽取状态中文码值 |
| `wiki_status_code` | Wiki 写入状态中文码值 |
| `failure_reason` | 失败原因 |
| `created_at` / `updated_at` | 时间字段 |

约束：

- `file_uid` 唯一。
- `content_hash` 建索引，用于去重。
- 不返回宿主机敏感绝对路径给前端。

### 3.3 `source_contents`

| 字段 | 用途 |
| --- | --- |
| `id` | 自增主键 |
| `content_uid` | 正文 UID |
| `file_uid` | 所属 SourceFile |
| `extract_status_code` | 抽取状态中文码值 |
| `plain_text` | 抽取正文 |
| `metadata_json` | 页数、标题、解析器等元数据 |
| `failure_reason` | 失败原因 |
| `created_at` / `updated_at` | 时间字段 |

约束：

- 一个 SourceFile 默认最多一条当前正文记录。
- 后续如需多版本正文，必须先新增设计，不在 MVP0 预建。

### 3.4 `wiki_ingest_runs`

| 字段 | 用途 |
| --- | --- |
| `id` | 自增主键 |
| `run_uid` | 对外运行 UID |
| `file_uid` | 所属 SourceFile |
| `status_code` | Wiki 写入状态中文码值 |
| `source_page_path` | Source page Vault 相对路径 |
| `wiki_page_paths_json` | Topic / Project 页面路径 |
| `index_updated` | 是否更新 index |
| `log_entry_appended` | 是否追加 log |
| `write_status_code` | 写入状态中文码值 |
| `failure_reason` | 失败原因 |
| `created_at` / `updated_at` / `completed_at` | 时间字段 |

约束：

- 不依赖 `agent_runs`。
- 记录结果，不保存大段生成内容；正文以 Obsidian Markdown 文件为准。
- 路径必须指向 Vault 内 `WikiForge/` 托管目录。

### 3.5 `system_dictionaries`

| 字段 | 用途 |
| --- | --- |
| `id` | 自增主键 |
| `dict_type` | 字典类型，例如：收纳任务状态、资料状态、Wiki 写入状态 |
| `dict_code` | 中文业务码值，例如：执行中、已收纳、已写入 |
| `label_zh` | 中文展示名 |
| `description_zh` | 中文说明 |
| `sort_order` | 排序 |
| `color_token` | 前端状态颜色标识 |
| `is_terminal` | 是否终态 |
| `is_success` | 是否成功态 |
| `is_active` | 是否启用 |
| `created_at` / `updated_at` | 时间字段 |

约束：

- `dict_type + dict_code` 唯一。
- 前端不得硬编码状态文案。
- 新状态必须先写字典，再进入接口和页面。

## 4. 状态字典初始值

### 4.1 收纳任务状态

| 字典类型 | 中文码值 | 颜色 | 终态 |
| --- | --- | --- | --- |
| 收纳任务状态 | 已创建 | neutral | 否 |
| 收纳任务状态 | 执行中 | info | 否 |
| 收纳任务状态 | 已完成 | success | 是 |
| 收纳任务状态 | 部分失败 | warning | 是 |
| 收纳任务状态 | 失败 | danger | 是 |

### 4.2 资料状态

| 字典类型 | 中文码值 | 颜色 | 终态 |
| --- | --- | --- | --- |
| 资料状态 | 已登记 | neutral | 否 |
| 资料状态 | 已收纳 | success | 否 |
| 资料状态 | 重复文件 | warning | 是 |
| 资料状态 | 待抽取 | neutral | 否 |
| 资料状态 | 抽取中 | info | 否 |
| 资料状态 | 已抽取 | success | 否 |
| 资料状态 | 待整理到 Wiki | neutral | 否 |
| 资料状态 | 已写入 Wiki | success | 是 |
| 资料状态 | 失败 | danger | 是 |

### 4.3 Wiki 写入状态

| 字典类型 | 中文码值 | 颜色 | 终态 |
| --- | --- | --- | --- |
| Wiki 写入状态 | 已创建 | neutral | 否 |
| Wiki 写入状态 | 写入中 | info | 否 |
| Wiki 写入状态 | 已写入 | success | 是 |
| Wiki 写入状态 | 失败 | danger | 是 |

## 5. 历史表处理

| 历史资源 | 处理方式 | 原因 |
| --- | --- | --- |
| `sources` | 退役 | 与 MVP0 `source_files` 主账本语义重叠，迁移文件已移除 |
| `system_settings` / `model_providers` | 退役 | 设置持久化和模型配置不进入 MVP0，预建迁移已移除 |
| `obsidian_notes` | 退役 | 旧 Source Note 语义退出，改用 `wiki_ingest_runs` |
| `agent_runs` / `agent_steps` / `review_items` | 退役 | 不再承载 Wiki ingest |
| `wiki_pages` / `wiki_integrations` | 退役 | MVP0 只保留 Obsidian 文件和 `wiki_ingest_runs`，不预建页面注册表 |
| `mcp_tool_calls` | 退役 | MCP 不进 MVP0，迁移文件已移除 |
| `personal_records` | 退役 | LifeOS 不进 MVP0，迁移文件已移除 |
| `vector_export_jobs` / `content_chunks` | 退役 | 向量不进 MVP0，迁移文件已移除 |
| `knowledge_maintenance_*` | 退役 | 知识体检不进 MVP0，迁移文件已移除 |
| Orchestration 相关配置或表 | 退役 | 辅助开发工程退出项目主线 |

## 6. Migration 策略

MVP0 新库只执行最小收纳、正文抽取、字典和 Wiki ingest 迁移。

2026-05-25 起，历史高级能力迁移不再随 MVP0 classpath 发布。已经执行过旧迁移的本地库，需要单独重建或迁移，不能把历史表继续作为 MVP0 依赖。

后续如需清理已有库中的历史表，必须分三步：

1. 输出当前库表快照和受影响代码清单。
2. 新增最小 migration，不直接破坏用户数据。
3. 单独出历史表退役 migration，包含备份和回滚说明。

禁止事项：

- 不在同一个 migration 里同时新增 MVP0 表和删除历史表。
- 不为了未来能力预建空表。
- 不让 Worker 拥有业务表。
- 不让前端直连数据库或猜测状态文案。

## 7. 验收规则

后续数据库实现必须满足：

- `git diff --check` 通过。
- Flyway migration 可在空库执行。
- 状态字典初始数据可重复初始化或幂等导入。
- 后端测试覆盖路径导入、上传导入、重复文件、Wiki 写入、字典映射。
- 数据库清理节点必须附带备份和回滚方案。
