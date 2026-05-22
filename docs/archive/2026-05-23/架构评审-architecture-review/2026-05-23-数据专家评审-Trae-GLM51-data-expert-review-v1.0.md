# WikiForge 架构评审意见（数据模型 / DBA 视角）

评审人：Trae_GLM5.1_数据专家
评审版本：v1.0
评审日期：2026-05-23
评审范围：sources、source_files、import_jobs、obsidian_notes、review_items 五张核心表 + 相关索引

---

## 结论

- 是否建议进入 MVP 编码：**有条件通过**

当前数据模型整体方向正确，五张核心表能覆盖 MVP 闭环。但存在以下条件：
1. sources 表需收敛在线文档连接器相关字段，MVP 阶段这些字段为死字段，增加首版开发心智负担。
2. 四张表（source_files、import_jobs、obsidian_notes、review_items）完全没有索引建议，必须补齐。
3. raw_text（longtext）与"MySQL 不作为知识正文主库"原则存在张力，MVP 需明确收敛策略。

---

## P0 阻塞问题

### P0-1：source_files、import_jobs、obsidian_notes、review_items 缺少索引设计

当前数据模型文档仅 sources 表给出了 5 条索引建议，其余四张 MVP 必需表索引为零。

这四张表在 MVP 中的核心查询路径：

| 表 | 核心查询 | 缺失索引 |
|---|---|---|
| source_files | 按 source_id 查文件列表；按 import_job_id 查任务文件；按 content_hash 去重 | `idx_source_files_source_id`、`idx_source_files_import_job_id`、`idx_source_files_content_hash` |
| import_jobs | 按状态查任务列表；按时间排序 | `idx_import_jobs_status`、`idx_import_jobs_created_at` |
| obsidian_notes | 按 related_type + related_id 查映射；按 vault_path 定位文件 | `idx_obsidian_notes_related`（related_type, related_id）、`idx_obsidian_notes_vault_path` |
| review_items | 按状态查待审核队列；按 source_id 查审核记录；按 review_type 筛选 | `idx_review_items_status`、`idx_review_items_source_id`、`idx_review_items_review_type` |

**建议**：MVP 0 Flyway 初始化时必须包含以上索引。缺少这些索引，MVP 1 文件列表页和 MVP 3 审核队列页在大数据量下会出现全表扫描。

### P0-2：sources 表状态枚举不一致

数据模型第 3 节 sources 表 status 字段说明为：

> pending、processing、pending_review、archived、rejected、failed

但第 26 节状态枚举中 Source status 列出：

> pending、organized、organize_only、processing、normalized、classified、pending_review、archived、rejected、failed

两者不一致。第 26 节多了 organized、organize_only、normalized、classified 四个值。

**影响**：如果 Flyway 使用 CHECK 约束或 Java 侧使用枚举类，不一致会导致运行时错误或数据无法写入。

**建议**：统一为一份权威枚举。MVP 阶段建议收敛为：pending → organized → processing → pending_review → archived / rejected / failed。其中 organized 对应"文件归集完成但未进入 AI 处理"，processing 对应"AI 处理中"。normalized、classified 作为 processing 的子状态，MVP 不需要在 sources.status 中区分，可由 agent_steps.step_name 体现。

---

## P1 高风险问题

### P1-1：sources 表在线文档连接器字段为 MVP 死字段

以下字段在 MVP 阶段无任何代码路径写入或读取：

| 字段 | 用途 | MVP 状态 |
|---|---|---|
| source_url | 在线文档原始链接 | MVP 不做在线采集 |
| connector_name | openclaw、hermes、native | MVP 不做连接器 |
| connector_status | 连接器读取状态 | MVP 不做连接器 |
| connector_trace_id | 连接器追踪 ID | MVP 不做连接器 |

**风险**：Flyway 建表时这些字段全部为 NULL，Java 侧 Entity/DTO 需要携带这些字段但无业务逻辑，增加首版代码复杂度。

**建议**：MVP Flyway 不建这四个字段。V1 在线文档阶段通过 Flyway ADD COLUMN 补充。这样做的好处：
- Java 侧 MVP Entity 干净，不携带死字段
- Flyway 迁移历史清晰，每个阶段加什么字段可追溯
- 不影响后续扩展，ADD COLUMN 是最简单的 DDL 操作

### P1-2：raw_text（longtext）与 MySQL 定位原则存在张力

数据模型原则明确："MySQL 是控制平面和索引库，不是最终知识正文主库。"但 sources 表包含 raw_text（longtext）字段，最大可存 4GB 文本。

**风险**：
1. 如果 MVP 允许 raw_text 写入，MySQL 单行可能膨胀到数十 MB，影响 InnoDB buffer pool 效率。
2. 大字段导致查询性能劣化——即使 SELECT 不包含 raw_text，InnoDB 的行格式也可能导致溢出页读取。
3. 与"正文保存策略"的分级策略矛盾——如果大部分场景 raw_text 为空，那这个字段在 MVP 的实际价值有限。

**建议**：MVP 阶段收敛策略：
- 保留 raw_text 字段定义（Flyway 建表时包含），但 MVP 代码路径中**不写入** raw_text。
- 保留 raw_text_saved（boolean）和 raw_text_policy（varchar(64)），用于标记策略决策。
- MVP 的"轻量内容缓存"仅使用 summary_short（text）。
- 如果 MVP 3 AI 处理需要原文输入，直接从 managed_path 读取文件，不经过 MySQL。
- V1 再根据实际需要决定是否启用 raw_text 写入。

这样做的好处：
- Flyway 不需要后续 ADD COLUMN（字段已存在但 MVP 不用）
- Java 侧 Entity 字段保留但 MVP 代码不赋值
- 不违背 MySQL 定位原则

### P1-3：sources 与 source_files 字段重复需明确主从关系

以下字段在两张表中同时存在：

| 字段 | sources | source_files |
|---|---|---|
| content_hash | ✅ | ✅ |
| original_path / raw_original_path | ✅（raw_original_path） | ✅（original_path） |
| managed_path / raw_managed_path | ✅（raw_managed_path） | ✅（managed_path） |
| organize_status / raw_organize_status | ✅（raw_organize_status） | ✅（organize_status） |

**风险**：一个 Source 对应多个 Source File 时，sources 级别的路径和状态与 source_files 级别可能不一致。例如：
- Source A 有 3 个文件，2 个 copied，1 个 duplicate → sources.raw_organize_status 应该是什么？
- Source A 的 raw_managed_path 指向哪个文件？

**建议**：
- MVP 明确规则：**一个 Source 对应一个 Source File**（1:1）。当前 MVP 闭环是"一个文件 → 一个 Source → 一个 Source Note"，不存在一对多场景。
- 在 1:1 前提下，sources 级别字段是 source_files 的冗余快照，用于列表页快速查询，避免 JOIN。
- 如果后续出现一对多（如一个网页 Source 包含多个附件文件），再重新设计 sources 级别的聚合逻辑。
- MVP 阶段建议在 source_files 上加 `UNIQUE INDEX idx_source_files_source_id (source_id)`，强制 1:1。

### P1-4：import_jobs 表在线文档字段为 MVP 死字段

与 P1-1 同理，以下字段 MVP 不使用：

| 字段 | 用途 | MVP 状态 |
|---|---|---|
| input_url | 在线文档链接 | MVP 不做在线采集 |
| source_platform | 在线来源平台 | MVP 仅本地文件 |
| connector_name | 使用的连接器 | MVP 不做连接器 |
| connector_status | 连接器读取状态 | MVP 不做连接器 |

**建议**：同 P1-1，MVP Flyway 不建这四个字段，V1 通过 ADD COLUMN 补充。

---

## P2 优化建议

### P2-1：obsidian_notes.vault_path 使用 TEXT 类型不利于索引

vault_path 是 Obsidian Vault 内相对路径，用于通过路径定位 Note。当前定义为 TEXT。

**问题**：MySQL 的 TEXT 类型不能作为完整索引的前缀，最多只能索引前 N 个字符。如果 vault_path 较长，索引可能不精确。

**建议**：
- 如果 vault_path 最大长度可控（Obsidian 文件路径一般不超过 500 字符），改为 `VARCHAR(512)` 并建普通索引。
- 如果坚持 TEXT，则索引使用前缀 `vault_path(128)` 或额外增加 `vault_path_hash VARCHAR(64)` 字段存路径哈希用于精确匹配。

### P2-2：review_items.run_id 应允许 NULL

当前 review_items.run_id 标记为 bigint fk，未标注 nullable。

**场景**：MVP 2 阶段用户可以手动为 Source 生成 Source Note 草案并进入审核，此时没有 agent_run。如果 run_id 为 NOT NULL，则必须先创建一条 agent_run 记录，增加不必要的复杂度。

**建议**：run_id 改为 `bigint null`，允许手动触发的审核项不关联 agent_run。

### P2-3：多对多关联表缺少唯一约束

source_projects、source_topics、source_entities、source_tags、source_actions 五张关联表均未定义复合唯一约束。

**风险**：如果代码层没有防重逻辑，可能出现同一对 (source_id, project_id) 被插入多次。

**建议**：每张关联表增加复合唯一索引：
- `UNIQUE idx_source_projects (source_id, project_id)`
- `UNIQUE idx_source_topics (source_id, topic_id)`
- `UNIQUE idx_source_entities (source_id, entity_id)`
- `UNIQUE idx_source_tags (source_id, tag_id)`
- `UNIQUE idx_source_actions (source_id, action_id)`

### P2-4：sources 表缺少 error_message 字段

MVP 1 文件归集和 MVP 3 AI 处理都可能失败，但 sources 表没有 error_message 字段记录失败原因。

当前失败信息只能通过 source_files.parse_error 或 agent_steps.error_message 查找，需要 JOIN 才能在 Source 列表页展示失败原因。

**建议**：sources 表增加 `error_message TEXT NULL`，用于在 Source 级别快速展示最近一次失败原因。这是列表页的刚需字段。

### P2-5：source_files 缺少 updated_at 字段

source_files 仅有 created_at，没有 updated_at。当 parse_status 或 organize_status 发生变更时，无法追踪最后更新时间。

**建议**：增加 `updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`。

### P2-6：import_jobs 缺少 updated_at 和 error_message 字段

import_jobs 没有 updated_at，状态变更无法追踪。也没有 error_message，任务失败时无法在列表页直接展示失败原因。

**建议**：
- 增加 `updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
- 增加 `error_message TEXT NULL`

### P2-7：sources 表 author 和 language 字段 MVP 价值有限

MVP 核心闭环是本地文件归集，author 和 language 对于本地文件（Word、PDF、Markdown）通常无法自动提取，需要手动填写或 AI 推断。

**建议**：保留字段定义（不删），但 MVP 代码不强制要求填写，允许 NULL。MVP 3 AI 处理时可选择性填充。

### P2-8：sources 表 summary_structured（JSON）可推迟到 MVP 3

summary_structured 是 AI 生成的结构化摘要，MVP 1 和 MVP 2 不涉及 AI 处理。

**建议**：保留字段定义，MVP 1/2 代码不写入。MVP 3 AI 处理时再启用。

---

## MVP 范围建议

### 建议保留

以下表和字段属于 MVP 必需：

| 表 | MVP 阶段 | 核心字段 |
|---|---|---|
| sources | MVP 1 起 | id, source_uid, title, source_type, source_platform, local_path, raw_original_path, raw_managed_path, raw_organize_status, processing_intent, content_hash, summary_short, status, review_required, review_reason, collected_at, source_created_at, created_at, updated_at |
| source_files | MVP 1 起 | id, source_id, import_job_id, file_name, file_ext, file_path, original_path, managed_path, file_size, mime_type, content_hash, parse_status, organize_status, duplicate_of_file_id, created_at |
| import_jobs | MVP 1 起 | id, job_uid, import_type, input_path, organize_mode, status, total_count, success_count, skipped_count, failed_count, started_at, finished_at, created_at |
| obsidian_notes | MVP 2 起 | 全部字段 |
| review_items | MVP 3 起 | 全部字段 |
| agent_runs | MVP 3 起 | 全部字段 |
| agent_steps | MVP 3 起 | 全部字段 |
| model_providers | MVP 0 起 | 全部字段 |
| system_settings | MVP 0 起 | 全部字段 |

### 建议移出（MVP Flyway 不建表，V1/V2 再建）

| 表 | 移出原因 | 归入阶段 |
|---|---|---|
| content_chunks | 向量库相关，MVP 不实现 | V2 |
| embedding_jobs | 向量库相关，MVP 不实现 | V2 |
| mcp_servers | MCP 相关，MVP 4 再建 | MVP 4 |
| mcp_tool_calls | MCP 相关，MVP 4 再建 | MVP 4 |
| personal_records | 个人记录相关，V1 再建 | V1 |
| agent_office_status | 办公室视图相关，V2 再建 | V2 |
| wiki_integrations | Wiki 整合追踪，MVP 不涉及 | V1 |
| artifacts | HTML 产物，MVP 2 可简化处理 | MVP 2 视需要 |

### 建议移出（MVP Flyway 不建字段，V1 再 ADD COLUMN）

| 表 | 字段 | 移出原因 |
|---|---|---|
| sources | source_url, connector_name, connector_status, connector_trace_id | 在线文档连接器，V1 |
| import_jobs | input_url, source_platform, connector_name, connector_status | 在线文档连接器，V1 |

### 建议新增

| 表 | 字段 | 原因 |
|---|---|---|
| sources | error_message TEXT NULL | Source 级别失败原因，列表页刚需 |
| source_files | updated_at DATETIME | 状态变更追踪 |
| import_jobs | updated_at DATETIME | 状态变更追踪 |
| import_jobs | error_message TEXT NULL | 任务失败原因，列表页刚需 |

---

## 技术栈建议

- 后端：Java 21 + Spring Boot 3.x + MyBatis-Plus，无异议
- 前端：Vue 3 + Vite + TypeScript + Element Plus，无异议
- 数据库：MySQL 8.x + Flyway，无异议
- 文件解析：Apache POI + Apache PDFBox + commonmark-java，MVP 不引入 Tika，无异议

---

## 数据模型建议

### 需要保留

#### sources 表（19 个 MVP 核心字段）

| 字段 | 类型 | MVP 必要性 | 说明 |
|---|---|---|---|
| id | bigint pk | 必须 | 主键 |
| source_uid | varchar(64) unique | 必须 | 全局 ID |
| title | varchar(512) | 必须 | 标题 |
| source_type | varchar(64) | 必须 | 文件类型 |
| source_platform | varchar(128) | 必须 | 来源平台，MVP 固定为 local |
| local_path | text | 必须 | 本地文件路径 |
| raw_original_path | text | 必须 | 原始路径，归集溯源 |
| raw_managed_path | text | 必须 | 归集路径，文件定位 |
| raw_organize_status | varchar(64) | 必须 | 归集状态，列表页筛选 |
| processing_intent | varchar(64) | 必须 | 处理意图，决定是否进 AI |
| content_hash | varchar(128) | 必须 | 去重核心字段 |
| summary_short | text | 必须 | 一句话摘要，列表页展示 |
| status | varchar(64) | 必须 | 主状态，列表页筛选 |
| review_required | boolean | 必须 | 是否需审核，审核队列入口 |
| review_reason | text | 必须 | 审核原因 |
| collected_at | datetime | 必须 | 收集时间，排序 |
| source_created_at | datetime null | 必须 | 原资料时间 |
| created_at | datetime | 必须 | 创建时间 |
| updated_at | datetime | 必须 | 更新时间 |

#### source_files 表（15 个 MVP 核心字段）

全部保留，无收敛。这是 MVP 1 文件归集的核心表，每个字段都有明确用途。

#### import_jobs 表（12 个 MVP 核心字段）

| 字段 | 类型 | MVP 必要性 | 说明 |
|---|---|---|---|
| id | bigint pk | 必须 | 主键 |
| job_uid | varchar(64) unique | 必须 | 全局 ID |
| import_type | varchar(64) | 必须 | MVP 仅 path_scan |
| input_path | text | 必须 | 扫描路径 |
| organize_mode | varchar(64) | 必须 | MVP 固定为 copy |
| status | varchar(64) | 必须 | 任务状态 |
| total_count | int | 必须 | 统计 |
| success_count | int | 必须 | 统计 |
| skipped_count | int | 必须 | 统计 |
| failed_count | int | 必须 | 统计 |
| started_at | datetime | 必须 | 开始时间 |
| finished_at | datetime | 必须 | 完成时间 |
| created_at | datetime | 必须 | 创建时间 |

#### obsidian_notes 表

全部保留，无收敛。这是 MVP 2 Obsidian 映射的核心表，字段设计合理。

#### review_items 表

全部保留，无收敛。仅 run_id 改为 nullable（P2-2）。

### 需要收敛

| 表 | 收敛字段 | 收敛方式 | 原因 |
|---|---|---|---|
| sources | source_url | MVP Flyway 不建 | V1 在线文档 |
| sources | connector_name | MVP Flyway 不建 | V1 在线文档 |
| sources | connector_status | MVP Flyway 不建 | V1 在线文档 |
| sources | connector_trace_id | MVP Flyway 不建 | V1 在线文档 |
| sources | raw_text | MVP 代码不写入 | 与 MySQL 定位原则矛盾 |
| sources | raw_text_saved | 保留，MVP 默认 false | 策略标记 |
| sources | raw_text_policy | 保留，MVP 默认 metadata_only | 策略标记 |
| sources | summary_structured | 保留，MVP 不写入 | MVP 3 AI 再启用 |
| sources | author | 保留，允许 NULL | MVP 不强制 |
| sources | language | 保留，允许 NULL | MVP 不强制 |
| sources | risk_level | 保留，MVP 默认 low | MVP 3 AI 再启用 |
| sources | priority | 保留，MVP 默认 normal | MVP 3 AI 再启用 |
| sources | confidence | 保留，允许 NULL | MVP 3 AI 再启用 |
| import_jobs | input_url | MVP Flyway 不建 | V1 在线文档 |
| import_jobs | source_platform | MVP Flyway 不建 | V1 在线文档 |
| import_jobs | connector_name | MVP Flyway 不建 | V1 在线文档 |
| import_jobs | connector_status | MVP Flyway 不建 | V1 在线文档 |
| source_files | parser_name | 保留，允许 NULL | MVP 3 再启用 |
| source_files | parse_error | 保留，允许 NULL | MVP 3 再启用 |

### 需要新增

| 表 | 新增字段 | 类型 | 原因 |
|---|---|---|---|
| sources | error_message | text null | Source 级别失败原因 |
| source_files | updated_at | datetime | 状态变更追踪 |
| import_jobs | updated_at | datetime | 状态变更追踪 |
| import_jobs | error_message | text null | 任务失败原因 |

---

## 索引设计建议（MVP 必须包含）

### sources

```sql
CREATE INDEX idx_sources_status ON sources(status);
CREATE INDEX idx_sources_type ON sources(source_type);
CREATE INDEX idx_sources_hash ON sources(content_hash);
CREATE INDEX idx_sources_collected_at ON sources(collected_at);
CREATE INDEX idx_sources_organize_status ON sources(raw_organize_status);
```

说明：
- `idx_sources_status`：列表页按状态筛选，高频使用
- `idx_sources_type`：列表页按文件类型筛选
- `idx_sources_hash`：去重查询核心索引，必须保留
- `idx_sources_collected_at`：列表页按时间排序
- `idx_sources_organize_status`：列表页按归集状态筛选
- 原 `idx_sources_platform`：MVP 阶段 source_platform 几乎全部为 local，区分度极低，建议 MVP 不建，V1 在线文档阶段再补

### source_files

```sql
CREATE INDEX idx_source_files_source_id ON source_files(source_id);
CREATE INDEX idx_source_files_import_job_id ON source_files(import_job_id);
CREATE INDEX idx_source_files_content_hash ON source_files(content_hash);
CREATE UNIQUE INDEX uk_source_files_source_id ON source_files(source_id);
```

说明：
- `idx_source_files_source_id`：Source 详情页查文件列表
- `idx_source_files_import_job_id`：导入任务详情页查文件列表
- `idx_source_files_content_hash`：文件去重核心索引
- `uk_source_files_source_id`：强制 1:1 关系（P1-3），MVP 阶段一个 Source 只有一个 File

### import_jobs

```sql
CREATE INDEX idx_import_jobs_status ON import_jobs(status);
CREATE INDEX idx_import_jobs_created_at ON import_jobs(created_at);
```

说明：
- `idx_import_jobs_status`：任务列表按状态筛选
- `idx_import_jobs_created_at`：任务列表按时间排序

### obsidian_notes

```sql
CREATE INDEX idx_obsidian_notes_related ON obsidian_notes(related_type, related_id);
CREATE INDEX idx_obsidian_notes_vault_path ON obsidian_notes(vault_path(128));
```

说明：
- `idx_obsidian_notes_related`：通过关联对象查 Note 映射，核心查询路径
- `idx_obsidian_notes_vault_path`：通过路径定位 Note，前缀索引 128 字符足够

### review_items

```sql
CREATE INDEX idx_review_items_status ON review_items(status);
CREATE INDEX idx_review_items_source_id ON review_items(source_id);
CREATE INDEX idx_review_items_review_type ON review_items(review_type);
```

说明：
- `idx_review_items_status`：审核队列按状态筛选，最高频查询
- `idx_review_items_source_id`：Source 详情页查审核记录
- `idx_review_items_review_type`：按审核类型筛选

### 多对多关联表

```sql
CREATE UNIQUE INDEX uk_source_projects ON source_projects(source_id, project_id);
CREATE UNIQUE INDEX uk_source_topics ON source_topics(source_id, topic_id);
CREATE UNIQUE INDEX uk_source_entities ON source_entities(source_id, entity_id);
CREATE UNIQUE INDEX uk_source_tags ON source_tags(source_id, tag_id);
CREATE UNIQUE INDEX uk_source_actions ON source_actions(source_id, action_id);
```

---

## 最终建议

### 下一步是否可以开始 MVP 0 项目骨架：**是**

条件：
1. **Flyway V1 初始化脚本**必须包含上述全部索引定义，不能留空。
2. **sources 表状态枚举**必须在 Java 侧和 Flyway CHECK 约束中统一，建议 MVP 收敛为：pending、organized、processing、pending_review、archived、rejected、failed。
3. **在线文档连接器字段**（sources 的 4 个 + import_jobs 的 4 个）MVP Flyway 不建，V1 通过 ADD COLUMN 补充。
4. **raw_text 字段**保留定义但 MVP 代码不写入，通过 raw_text_policy 标记策略。
5. **source_files 与 sources 1:1 关系**通过唯一索引强制，MVP 阶段不做一对多。
6. **新增字段**：sources.error_message、source_files.updated_at、import_jobs.updated_at、import_jobs.error_message。

以上条件不阻塞 MVP 0 骨架搭建，但需在 MVP 1（源文件归集）开发前完成 Flyway 脚本定稿。
