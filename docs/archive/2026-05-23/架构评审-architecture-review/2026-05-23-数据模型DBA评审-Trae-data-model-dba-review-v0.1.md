# WikiForge 数据模型评审意见（DBA视角）

## 结论
- 是否建议进入 MVP 编码：是（有条件）

## P0 阻塞问题
- 无直接阻塞问题，建议按实施计划分阶段开发

## P1 高风险问题
1. **sources表冗余字段过多**：当前sources表包含了大量与在线文档、向量库、个人记录相关的字段，可能影响MVP开发效率
2. **索引设计不完整**：部分核心表缺少必要的索引，可能影响查询性能
3. **raw_text存储策略**：MVP阶段不建议将大文件文本存入MySQL，应优先使用文件路径引用

## P2 优化建议
1. **表结构拆分**：考虑将sources表中的在线文档相关字段拆分为独立表
2. **索引优化**：为关联查询频繁的字段添加复合索引
3. **状态枚举约束**：使用MySQL的ENUM类型或检查约束来限制状态字段的值范围

## MVP 范围建议
- 建议保留：
  - sources（核心字段）
  - source_files（核心字段）
  - import_jobs（核心字段）
  - obsidian_notes（全部字段）
  - review_items（核心字段）
- 建议移出：
  - content_chunks（向量库相关，MVP暂不实现）
  - embedding_jobs（向量库相关，MVP暂不实现）
  - mcp_servers（MCP相关，MVP暂不实现）
  - mcp_tool_calls（MCP相关，MVP暂不实现）
  - personal_records（个人记录相关，MVP暂不实现）
  - agent_office_status（办公室视图相关，MVP暂不实现）
- 建议新增：
  - 无

## 技术栈建议
- 后端：Java 21 + Spring Boot 3.x + MyBatis-Plus
- 前端：Vue 3 + Vite + TypeScript
- 数据库：MySQL 8.x
- 文件解析：优先使用轻量级解析器，如Apache Tika（但需注意依赖大小）

## 数据模型建议
### sources表
- **需要保留**：id, source_uid, title, source_type, source_platform, local_path, raw_original_path, raw_managed_path, raw_organize_status, processing_intent, content_hash, summary_short, status, review_required, review_reason, collected_at, source_created_at, created_at, updated_at
- **需要收敛**：connector_name, connector_status, connector_trace_id, raw_text, raw_text_saved, raw_text_policy, summary_structured, risk_level, priority, confidence
- **需要新增**：无
- **索引建议**：
  - 必须保留：idx_sources_status, idx_sources_hash, idx_sources_collected_at
  - 建议新增：idx_sources_type_platform, idx_sources_organize_status

### source_files表
- **需要保留**：id, source_id, import_job_id, file_name, file_ext, file_path, original_path, managed_path, file_size, mime_type, content_hash, parse_status, organize_status, duplicate_of_file_id, created_at
- **需要收敛**：parser_name, parse_error
- **需要新增**：无
- **索引建议**：
  - 必须新增：idx_source_files_source_id, idx_source_files_import_job_id, idx_source_files_content_hash

### import_jobs表
- **需要保留**：id, job_uid, import_type, input_path, organize_mode, status, total_count, success_count, skipped_count, failed_count, started_at, finished_at, created_at
- **需要收敛**：input_url, source_platform, connector_name, connector_status
- **需要新增**：无
- **索引建议**：
  - 必须保留：idx_import_jobs_status
  - 建议新增：idx_import_jobs_started_at

### obsidian_notes表
- **需要保留**：全部字段
- **需要收敛**：无
- **需要新增**：无
- **索引建议**：
  - 必须新增：idx_obsidian_notes_related_type_id, idx_obsidian_notes_vault_path

### review_items表
- **需要保留**：id, source_id, run_id, review_type, status, reason, suggested_changes, markdown_draft, user_decision, reviewed_at, created_at, updated_at
- **需要收敛**：无
- **需要新增**：无
- **索引建议**：
  - 必须新增：idx_review_items_source_id, idx_review_items_status, idx_review_items_review_type

## 最终建议
- 下一步可以开始 MVP 0 项目骨架，但需先完成数据模型的收敛和索引设计
- 建议按以下顺序开发：
  1. 数据库初始化（system_settings, model_providers）
  2. 源文件归集（import_jobs, sources, source_files）
  3. Obsidian Source Note（obsidian_notes）
  4. AI 辅助整理（agent_runs, agent_steps, review_items）
  5. 轻量 MCP（预留表结构，暂不实现）

## 数据模型收敛建议（MVP版）

### sources（MVP版）
```sql
CREATE TABLE sources (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_uid VARCHAR(64) UNIQUE NOT NULL,
  title VARCHAR(512) NOT NULL,
  source_type VARCHAR(64) NOT NULL COMMENT 'webpage、video、pdf、word、markdown、image、chat、file',
  source_platform VARCHAR(128) NOT NULL COMMENT '知乎、B站、飞书、腾讯文档、微信、本地等',
  local_path TEXT,
  raw_original_path TEXT NOT NULL,
  raw_managed_path TEXT,
  raw_organize_status VARCHAR(64) NOT NULL COMMENT 'pending、copied、duplicate、need_confirm、failed',
  processing_intent VARCHAR(64) NOT NULL COMMENT 'organize_only、extract_later、process_now',
  content_hash VARCHAR(128) NOT NULL,
  summary_short TEXT,
  status VARCHAR(64) NOT NULL COMMENT 'pending、processing、pending_review、archived、rejected、failed',
  review_required BOOLEAN DEFAULT FALSE,
  review_reason TEXT,
  collected_at DATETIME NOT NULL,
  source_created_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_sources_status ON sources(status);
CREATE INDEX idx_sources_hash ON sources(content_hash);
CREATE INDEX idx_sources_collected_at ON sources(collected_at);
CREATE INDEX idx_sources_type_platform ON sources(source_type, source_platform);
CREATE INDEX idx_sources_organize_status ON sources(raw_organize_status);
```

### source_files（MVP版）
```sql
CREATE TABLE source_files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_id BIGINT NOT NULL,
  import_job_id BIGINT NOT NULL,
  file_name VARCHAR(512) NOT NULL,
  file_ext VARCHAR(32) NOT NULL,
  file_path TEXT NOT NULL,
  original_path TEXT NOT NULL,
  managed_path TEXT,
  file_size BIGINT NOT NULL,
  mime_type VARCHAR(128) NOT NULL,
  content_hash VARCHAR(128) NOT NULL,
  parse_status VARCHAR(64) NOT NULL COMMENT 'pending、success、failed、partial',
  organize_status VARCHAR(64) NOT NULL COMMENT 'pending、copied、duplicate、need_confirm、failed',
  duplicate_of_file_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (source_id) REFERENCES sources(id),
  FOREIGN KEY (import_job_id) REFERENCES import_jobs(id)
);

CREATE INDEX idx_source_files_source_id ON source_files(source_id);
CREATE INDEX idx_source_files_import_job_id ON source_files(import_job_id);
CREATE INDEX idx_source_files_content_hash ON source_files(content_hash);
```

### import_jobs（MVP版）
```sql
CREATE TABLE import_jobs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_uid VARCHAR(64) UNIQUE NOT NULL,
  import_type VARCHAR(64) NOT NULL COMMENT 'manual、upload、path_scan、url',
  input_path TEXT NOT NULL,
  organize_mode VARCHAR(64) NOT NULL COMMENT 'copy、move、index_only',
  status VARCHAR(64) NOT NULL COMMENT 'pending、running、completed、failed',
  total_count INT DEFAULT 0,
  success_count INT DEFAULT 0,
  skipped_count INT DEFAULT 0,
  failed_count INT DEFAULT 0,
  started_at DATETIME,
  finished_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_import_jobs_status ON import_jobs(status);
CREATE INDEX idx_import_jobs_started_at ON import_jobs(started_at);
```

### obsidian_notes（MVP版）
```sql
CREATE TABLE obsidian_notes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  note_uid VARCHAR(64) UNIQUE NOT NULL,
  note_type VARCHAR(64) NOT NULL COMMENT 'source、project、topic、entity、action、log、index',
  related_type VARCHAR(64) NOT NULL COMMENT 'source、project、topic、entity、action',
  related_id BIGINT NOT NULL,
  title VARCHAR(512) NOT NULL,
  vault_path TEXT NOT NULL,
  absolute_path TEXT NOT NULL,
  obsidian_uri TEXT NOT NULL,
  preview_enabled BOOLEAN DEFAULT TRUE,
  frontmatter_json JSON,
  content_hash VARCHAR(128) NOT NULL,
  last_written_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_obsidian_notes_related_type_id ON obsidian_notes(related_type, related_id);
CREATE INDEX idx_obsidian_notes_vault_path ON obsidian_notes(vault_path);
```

### review_items（MVP版）
```sql
CREATE TABLE review_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_id BIGINT NOT NULL,
  run_id BIGINT,
  review_type VARCHAR(64) NOT NULL COMMENT 'classification、integration、risk、duplicate、conflict',
  status VARCHAR(64) NOT NULL COMMENT 'pending、approved、rejected、modified',
  reason TEXT,
  suggested_changes JSON,
  markdown_draft LONGTEXT,
  user_decision TEXT,
  reviewed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (source_id) REFERENCES sources(id)
);

CREATE INDEX idx_review_items_source_id ON review_items(source_id);
CREATE INDEX idx_review_items_status ON review_items(status);
CREATE INDEX idx_review_items_review_type ON review_items(review_type);
```