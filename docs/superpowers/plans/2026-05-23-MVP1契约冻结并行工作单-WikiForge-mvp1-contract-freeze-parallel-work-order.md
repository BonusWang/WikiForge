# MVP1 Contract Freeze Parallel Work Order

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` when dispatching tasks, or `superpowers:executing-plans` for inline execution. This document freezes the MVP1 contract before Core, Worker, UI, DevOps, Test/Review, and Docs work in parallel.

**Goal:** Freeze the MVP1 source ingestion contract so multiple agents can implement the local file collection loop without changing API, DTO, DDL, status enums, or service boundaries independently.

**Architecture:** Core owns public APIs, metadata tables, ImportJob lifecycle, Source and SourceFile records. Worker owns local filesystem scanning, hash calculation, type detection, and Raw Sources copying, then reports results back through Core internal APIs. UI only calls Core. DevOps only wires environment variables, volumes, health checks, and CI build verification.

**Tech Stack:** Java 21, Spring Boot 3.3, MyBatis-Plus, Flyway, MySQL 8, Vue 3, Vite, TypeScript, Docker Compose, GitHub Actions.

---

## 1. Current Stage

Current branch: `codex/mvp1-source-ingestion`

Base version: `0.02`

MVP1 scope:

- Create local path import jobs.
- Scan local directories.
- Copy source files into Raw Sources by type.
- Record `sources`, `source_files`, and `import_jobs`.
- Show import jobs and collected files in UI.

Out of scope for MVP1:

- Obsidian Source Note generation.
- AI summarization, classification, and multi-agent knowledge refining.
- MCP Server.
- Vector database.
- Online document connectors.
- Personal record ingestion.

## 2. Frozen Service Boundaries

### Core Service

Owns:

- Public REST API for UI.
- Internal callback API for Worker.
- `sources`, `source_files`, `import_jobs`.
- Import job status transitions.
- Source and SourceFile metadata persistence.

Must not:

- Scan local directories.
- Copy source files.
- Parse file content.
- Compute file hash directly in controller.

### Worker Service

Owns:

- Directory walking.
- File filter rules.
- SHA-256 calculation.
- MIME / file type detection.
- Raw Sources copy operation.
- Duplicate detection by content hash.

Must not:

- Expose user-facing query APIs.
- Write UI state.
- Modify Core tables directly unless a later decision explicitly allows it.

### UI

Owns:

- Import job creation form.
- Import job list and detail status.
- Source file list for a job.

Must not:

- Call Worker directly.
- Access local filesystem directly.
- Access database directly.

## 3. Frozen API Contract

### 3.1 UI -> Core: create local import job

```text
POST /api/v1/import-jobs/local
```

Request:

```json
{
  "inputPath": "E:/example/messy-sources",
  "rawSourcesRoot": "E:/WikiForge_RawSources",
  "recursive": true,
  "organizeMode": "copy",
  "maxCopyFileSizeMb": 100
}
```

Rules:

- `inputPath` is required.
- `rawSourcesRoot` is required.
- Both paths must be absolute after normalization.
- `inputPath` must exist.
- MVP1 accepts directory input first. Single-file input is reserved.
- `recursive` defaults to `true`.
- `organizeMode` defaults to `copy`; MVP1 UI must not expose `move`.
- `maxCopyFileSizeMb` defaults to `100`.
- `rawSourcesRoot` must match the configured `wikiforge.storage.raw-sources-root`; request override is accepted only to make UI state explicit.
- After persisting the pending job, Core must dispatch Worker through section 3.5; UI must not call Worker directly.

Success response:

```json
{
  "success": true,
  "data": {
    "jobUid": "job_20260523_ab12cd34ef56",
    "importType": "path_scan",
    "inputPath": "E:/example/messy-sources",
    "rawSourcesRoot": "E:/WikiForge_RawSources",
    "recursive": true,
    "organizeMode": "copy",
    "maxCopyFileSizeMb": 100,
    "status": "pending",
    "totalCount": 0,
    "successCount": 0,
    "skippedCount": 0,
    "failedCount": 0,
    "createdAt": "2026-05-23T03:40:00+08:00"
  },
  "message": "ok",
  "code": null
}
```

Failure response:

```json
{
  "success": false,
  "data": null,
  "message": "inputPath must not overlap rawSourcesRoot",
  "code": "SOURCE_001"
}
```

### 3.2 UI -> Core: list import jobs

```text
GET /api/v1/import-jobs?status=pending&page=1&pageSize=20
```

Response data fields:

- `items`
- `page`
- `pageSize`
- `total`

Each item uses the same fields as create response.

### 3.3 UI -> Core: get import job detail

```text
GET /api/v1/import-jobs/{jobUid}
```

Response data uses the same fields as create response plus:

- `startedAt`
- `finishedAt`
- `errorMessage`

### 3.4 UI -> Core: list source files for job

```text
GET /api/v1/source-files?jobUid=job_20260523_ab12cd34ef56&page=1&pageSize=50
```

Source file item fields:

- `fileUid`
- `sourceUid`
- `jobUid`
- `fileName`
- `fileExt`
- `originalPath`
- `managedPath`
- `fileSize`
- `mimeType`
- `contentHash`
- `parseStatus`
- `organizeStatus`
- `duplicateOfFileUid`
- `createdAt`

### 3.5 Core -> Worker: start local import

```text
POST /api/v1/worker/import-jobs/local/run
```

Request:

```json
{
  "jobUid": "job_20260523_ab12cd34ef56",
  "inputPath": "E:/example/messy-sources",
  "rawSourcesRoot": "E:/WikiForge_RawSources",
  "recursive": true,
  "organizeMode": "copy",
  "maxCopyFileSizeMb": 100,
  "skipHidden": true,
  "skipTemporary": true,
  "followSymlinks": false
}
```

Response:

```json
{
  "success": true,
  "data": {
    "jobUid": "job_20260523_ab12cd34ef56",
    "accepted": true,
    "workerStatus": "accepted"
  },
  "message": "ok",
  "code": null
}
```

### 3.6 Worker -> Core: update import job status

```text
PATCH /api/v1/internal/import-jobs/{jobUid}/status
Header: X-WikiForge-Internal-Token: ${WIKIFORGE_INTERNAL_API_TOKEN}
```

Request:

```json
{
  "status": "running",
  "totalCount": 10,
  "successCount": 3,
  "skippedCount": 1,
  "failedCount": 0,
  "errorMessage": null
}
```

### 3.7 Worker -> Core: submit source files batch

```text
POST /api/v1/internal/import-jobs/{jobUid}/source-files/batch
Header: X-WikiForge-Internal-Token: ${WIKIFORGE_INTERNAL_API_TOKEN}
```

Request:

```json
{
  "files": [
    {
      "fileName": "example.pdf",
      "fileExt": "pdf",
      "originalPath": "E:/example/messy-sources/example.pdf",
      "managedPath": "E:/WikiForge_RawSources/03_PDFs_PDF/example.pdf",
      "fileSize": 1024,
      "mimeType": "application/pdf",
      "contentHash": "sha256",
      "parseStatus": "pending",
      "organizeStatus": "copied",
      "duplicateOfFileUid": null
    }
  ]
}
```

### 3.8 Duplicate Ownership And Idempotency

Core is the source of truth for persisted duplicate relationships.

Worker responsibilities:

- Calculate SHA-256 for every file it accepts.
- Avoid copying the same hash twice inside the same run when it can identify a duplicate locally.
- Use a hash-aware managed path so an existing managed file can be reused without overwriting.
- Send `contentHash`, `managedPath`, and `organizeStatus` to Core.

Core responsibilities:

- Resolve `duplicateOfFileUid` when a submitted `contentHash` already exists in `source_files`.
- Persist a `source_files` row for copied and duplicate files.
- Treat repeated batch submissions for the same `jobUid + originalPath + contentHash` as idempotent and not create duplicate rows.

No separate hash lookup API is required in MVP1. If Worker cannot know the global duplicate file uid, it sends `duplicateOfFileUid = null`; Core fills it during persistence.

## 4. Frozen DTO Names

Core public DTOs:

- `CreateLocalImportJobRequest`
- `ImportJobResponse`
- `ImportJobPageResponse`
- `SourceFileResponse`

Core internal DTOs:

- `UpdateImportJobStatusRequest`
- `SubmitSourceFilesBatchRequest`
- `SubmitSourceFileItem`

Worker DTOs:

- `RunLocalImportJobRequest`
- `RunLocalImportJobResponse`

Shared response:

- `ApiResponse<T>` with `success`, `data`, `message`, `code`.

## 5. Frozen Status Enums

### ImportJobStatus

- `pending`
- `running`
- `completed`
- `failed`
- `cancelled`

Transitions:

```text
pending -> running
pending -> failed
running -> completed
running -> failed
running -> cancelled
```

### ImportType

- `path_scan`
- `url`
- `upload`

MVP1 implements only `path_scan`.

### OrganizeMode

- `copy`
- `index_only`

MVP1 UI exposes only `copy`.

### SourceStatus

- `pending`
- `organized`
- `failed`

Later stages may add `processing`, `pending_review`, `archived`, `rejected`.

### ParseStatus

- `pending`
- `success`
- `failed`
- `partial`

MVP1 defaults to `pending`.

### RawOrganizeStatus

- `pending`
- `copied`
- `duplicate`
- `need_confirm`
- `failed`

Skipped files are counted on `import_jobs.skipped_count`; they do not require `source_files` rows in MVP1.

## 6. Frozen Error Codes

- `SOURCE_001`: invalid or unsafe local path.
- `SOURCE_002`: input path not found.
- `SOURCE_003`: unsupported input type.
- `IMPORT_001`: import job not found.
- `IMPORT_002`: invalid import job status transition.
- `WORKER_001`: worker rejected import task.
- `COMMON_001`: validation failed.

## 7. Frozen Path Safety Rules

- Normalize paths before comparison.
- Require absolute paths.
- `inputPath` must exist.
- `inputPath` must be a directory in MVP1.
- `inputPath` and `rawSourcesRoot` must not overlap in either direction:
  - input inside Raw Sources is forbidden.
  - Raw Sources inside input is forbidden.
- If `wikiforge.security.allowed-scan-roots` is configured, `inputPath` must be under one of those roots after real-path resolution.
- `rawSourcesRoot` must equal configured `wikiforge.storage.raw-sources-root` after normalization.
- Existing input directories must be checked with no-follow semantics; symlink or junction-like entries are skipped by Worker in MVP1.
- Default `followSymlinks = false`.
- Never move or delete original files in MVP1.
- Hidden files, system files, temporary files, and lock files are skipped by Worker.
- Temporary file patterns:
  - `~$*`
  - `*.tmp`
  - `*.temp`
  - `*.swp`
  - `.DS_Store`
  - `Thumbs.db`
- Files larger than `maxCopyFileSizeMb` are not copied in MVP1. Worker reports them as `organizeStatus = need_confirm` and increments `skippedCount`.

## 8. Frozen DDL

Migration:

```text
backend/wikiforge-core-service/src/main/resources/db/migration/V20260523_002__create_source_import_tables.sql
```

Tables:

- `sources`
- `source_files`
- `import_jobs`

Required indexes:

- `uk_sources_source_uid`
- `idx_sources_status`
- `idx_sources_hash`
- `uk_source_files_file_uid`
- `idx_source_files_job`
- `idx_source_files_hash`
- `uk_import_jobs_job_uid`
- `idx_import_jobs_status_created`

No `source_contents`, `agent_runs`, `review_items`, `obsidian_notes`, or vector tables in MVP1 implementation tasks unless explicitly approved.

Authoritative MVP1 DDL:

```sql
CREATE TABLE import_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_uid VARCHAR(64) NOT NULL,
    import_type VARCHAR(64) NOT NULL,
    input_path TEXT NULL,
    input_url TEXT NULL,
    raw_sources_root TEXT NULL,
    recursive_scan TINYINT(1) NOT NULL DEFAULT 1,
    organize_mode VARCHAR(64) NOT NULL DEFAULT 'copy',
    max_copy_file_size_mb INT NOT NULL DEFAULT 100,
    source_platform VARCHAR(128) NULL,
    connector_name VARCHAR(128) NULL,
    connector_status VARCHAR(64) NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'pending',
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_import_jobs_job_uid (job_uid),
    KEY idx_import_jobs_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_uid VARCHAR(64) NOT NULL,
    title VARCHAR(512) NULL,
    source_type VARCHAR(64) NOT NULL DEFAULT 'file',
    source_platform VARCHAR(128) NOT NULL DEFAULT 'local',
    source_url TEXT NULL,
    connector_name VARCHAR(128) NULL,
    connector_status VARCHAR(64) NULL,
    connector_trace_id VARCHAR(128) NULL,
    local_path TEXT NULL,
    raw_original_path TEXT NULL,
    raw_managed_path TEXT NULL,
    raw_organize_status VARCHAR(64) NOT NULL DEFAULT 'pending',
    processing_intent VARCHAR(64) NOT NULL DEFAULT 'organize_only',
    content_hash VARCHAR(128) NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'pending',
    collected_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sources_source_uid (source_uid),
    KEY idx_sources_status (status),
    KEY idx_sources_hash (content_hash),
    KEY idx_sources_collected_at (collected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE source_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    file_uid VARCHAR(64) NOT NULL,
    source_id BIGINT NULL,
    import_job_id BIGINT NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    file_ext VARCHAR(32) NULL,
    original_path TEXT NOT NULL,
    managed_path TEXT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(128) NULL,
    content_hash VARCHAR(128) NULL,
    parser_name VARCHAR(128) NULL,
    parse_status VARCHAR(64) NOT NULL DEFAULT 'pending',
    organize_status VARCHAR(64) NOT NULL DEFAULT 'pending',
    duplicate_of_file_id BIGINT NULL,
    parse_error TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_files_file_uid (file_uid),
    KEY idx_source_files_job (import_job_id),
    KEY idx_source_files_hash (content_hash),
    KEY idx_source_files_source (source_id),
    CONSTRAINT fk_source_files_source FOREIGN KEY (source_id) REFERENCES sources(id),
    CONSTRAINT fk_source_files_import_job FOREIGN KEY (import_job_id) REFERENCES import_jobs(id),
    CONSTRAINT fk_source_files_duplicate FOREIGN KEY (duplicate_of_file_id) REFERENCES source_files(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 8.1 Frozen Runtime Configuration

Official environment variables:

- `WIKIFORGE_RAW_SOURCES_ROOT`: container path for Raw Sources, default `/data/wikiforge/raw-sources`.
- `WIKIFORGE_WORKER_BASE_URL`: Core -> Worker base URL, default `http://wikiforge-worker-service:8081`.
- `WIKIFORGE_CORE_SERVICE_BASE_URL`: Worker -> Core base URL, default `http://wikiforge-core-service:8080`.
- `WIKIFORGE_INTERNAL_API_TOKEN`: shared token for Worker -> Core internal callbacks. Compose local default is `change-me`; real deployment must override it.
- `WIKIFORGE_ALLOWED_SCAN_ROOTS`: optional comma-separated allowed input roots. Empty means unrestricted local desktop mode.

Compose-only host variable:

- `WIKIFORGE_HOST_RAW_SOURCES_ROOT`: host bind path for Raw Sources, default `../data/raw-sources`.

Legacy compatibility:

- `WIKIFORGE_RAW_SOURCES_PATH` may remain as an alias during MVP1, but new code must prefer `WIKIFORGE_RAW_SOURCES_ROOT`.

## 9. Parallel Work Order v2

### PWO-MVP1-CF-001 Contract API Designer

任务ID：PWO-MVP1-CF-001
父任务：MVP1 Source Ingestion
专家角色：Contract API Designer Agent
目标服务或文档：Contract documents
允许修改文件：

- `docs/superpowers/plans/2026-05-23-MVP1契约冻结并行工作单-WikiForge-mvp1-contract-freeze-parallel-work-order.md`
- `docs/数据模型-data-model.md`
- `docs/技术架构-technical-architecture.md`
- `docs/2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md`

禁止修改文件：

- `backend/`
- `frontend/`
- `deploy/`
- `.github/`

依赖任务：无
输入契约：当前最新归档索引 v1.3、WikiForge Skill、MVP1 需求
输出契约：本文件第 3-8 节全部冻结
验证命令：`git diff --check`
是否可并行：否，必须最先完成
合并顺序：1
Handoff 要求：列出冻结 API、DTO、DDL、状态枚举和仍有争议的点。

### PWO-MVP1-COMMON-001 Common Contract Utilities

任务ID：PWO-MVP1-COMMON-001
父任务：MVP1 Source Ingestion
专家角色：Core Service Agent
目标服务或文档：`wikiforge-common`
允许修改文件：

- `backend/wikiforge-common/src/main/java/com/wikiforge/common/web/ApiResponse.java`
- `backend/wikiforge-common/src/main/java/com/wikiforge/common/error/`
- `backend/wikiforge-common/src/main/java/com/wikiforge/common/filesystem/`

禁止修改文件：

- `backend/wikiforge-core-service/`
- `backend/wikiforge-worker-service/`
- `frontend/`
- `deploy/`
- `.github/`

依赖任务：PWO-MVP1-CF-001
输入契约：ApiResponse and error codes from sections 4 and 6; path safety rules from section 7
输出契约：`ApiResponse.fail`, `BusinessException`, `ErrorCode`, `PathSafety`
验证命令：`mvn -s %TEMP%\\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\\repository" -pl wikiforge-common test`
是否可并行：否，高冲突串行区
合并顺序：2
Handoff 要求：说明新增 common 类型和是否影响已有 health API。

### PWO-MVP1-CORE-001 Core Import Job API

任务ID：PWO-MVP1-CORE-001
父任务：MVP1 Source Ingestion
专家角色：Core Service Agent
目标服务或文档：`wikiforge-core-service`
允许修改文件：

- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/`
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/domain/`
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/`
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/interfaces/web/`
- `backend/wikiforge-core-service/src/test/`
- `backend/wikiforge-core-service/src/main/resources/db/migration/V20260523_002__create_source_import_tables.sql`

禁止修改文件：

- `backend/wikiforge-worker-service/`
- `frontend/`
- `deploy/`
- `.github/`

依赖任务：PWO-MVP1-CF-001, PWO-MVP1-COMMON-001
输入契约：sections 3-8
输出契约：Core public APIs, Core internal Worker callback APIs, persistence for `import_jobs`, `sources`, `source_files`
验证命令：`mvn -s %TEMP%\\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\\repository" -pl wikiforge-core-service -am test`
是否可并行：可与 Worker/UI/DevOps 在文件边界不冲突时并行
合并顺序：3
Handoff 要求：列出接口路径、测试覆盖、DDL 文件名、未接 Worker 的模拟点。

### PWO-MVP1-WORKER-001 Worker Local Scanner

任务ID：PWO-MVP1-WORKER-001
父任务：MVP1 Source Ingestion
专家角色：Worker Service Agent
目标服务或文档：`wikiforge-worker-service`
允许修改文件：

- `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/application/`
- `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/domain/`
- `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/infrastructure/filesystem/`
- `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/infrastructure/integration/`
- `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/interfaces/web/`
- `backend/wikiforge-worker-service/src/test/`

禁止修改文件：

- `backend/wikiforge-core-service/`
- `frontend/`
- `deploy/`
- `.github/`

依赖任务：PWO-MVP1-CF-001, PWO-MVP1-COMMON-001
输入契约：Core -> Worker API section 3.5; Worker -> Core APIs sections 3.6 and 3.7
输出契约：local scan accepts task, scans directory, copies files, calculates SHA-256, reports back to Core
验证命令：`mvn -s %TEMP%\\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\\repository" -pl wikiforge-worker-service -am test`
是否可并行：可与 Core/UI/DevOps 在文件边界不冲突时并行
合并顺序：4
Handoff 要求：列出文件过滤规则、Raw Sources 分类路径、hash duplicate behavior、Core callback assumptions。

### PWO-MVP1-UI-001 Import Dashboard

任务ID：PWO-MVP1-UI-001
父任务：MVP1 Source Ingestion
专家角色：UI Agent
目标服务或文档：`wikiforge-ui`
允许修改文件：

- `frontend/src/`
- `frontend/package.json`
- `frontend/package-lock.json`

禁止修改文件：

- `backend/`
- `deploy/`
- `.github/`

依赖任务：PWO-MVP1-CF-001
输入契约：Core public APIs sections 3.1-3.4
输出契约：UI can create local import job, show job list, show source files list with pending/running/completed/failed status
验证命令：`npm run build`
是否可并行：可与 Core/Worker after contract freeze; UI can use mocked API shape until Core merges
合并顺序：5
Handoff 要求：列出 API client methods, pages/components touched, any mocked data remaining。

### PWO-MVP1-DEVOPS-001 Runtime Config And Compose

任务ID：PWO-MVP1-DEVOPS-001
父任务：MVP1 Source Ingestion
专家角色：DevOps Agent
目标服务或文档：Docker, Compose, CI
允许修改文件：

- `deploy/`
- `.github/workflows/ci.yml`
- `.dockerignore`
- `.env.example`

禁止修改文件：

- `backend/` business code
- `frontend/src/`

依赖任务：PWO-MVP1-CF-001
输入契约：service names, Raw Sources path, environment variables
输出契约：

- `WIKIFORGE_RAW_SOURCES_ROOT`
- `WIKIFORGE_WORKER_BASE_URL`
- `WIKIFORGE_CORE_SERVICE_BASE_URL`
- `WIKIFORGE_INTERNAL_API_TOKEN`
- Raw Sources volume mapping
- Compose config still valid

验证命令：

- `docker compose -f deploy/docker-compose.yml config`
- `docker compose -f deploy/docker-compose.dev.yml config`

是否可并行：可与 Core/Worker/UI after contract freeze
合并顺序：6
Handoff 要求：列出 environment variables, volume paths, CI impact, Docker Desktop dependency。

### PWO-MVP1-QA-001 Test And Review

任务ID：PWO-MVP1-QA-001
父任务：MVP1 Source Ingestion
专家角色：Test QA Agent + Security Review Agent + Integration Review Agent
目标服务或文档：read-only review first; tests only if explicitly assigned
允许修改文件：

- 默认只读
- 若主编排 Agent 明确授权，可补测试文件

禁止修改文件：

- shared DTO and migrations unless main orchestrator approves

依赖任务：PWO-MVP1-CF-001
输入契约：all sections in this PWO
输出契约：risk list, missing tests, integration blockers, security concerns
验证命令：

- `mvn -s %TEMP%\\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\\repository" test`
- `npm run build`
- `docker compose -f deploy/docker-compose.yml config`

是否可并行：只读评审可并行；落测试代码需等相关实现完成
合并顺序：7
Handoff 要求：Findings first, ordered by severity, with file/line references where applicable。

### PWO-MVP1-DOCS-001 Docs And Archive

任务ID：PWO-MVP1-DOCS-001
父任务：MVP1 Source Ingestion
专家角色：Docs Agent
目标服务或文档：docs and archive
允许修改文件：

- `docs/`
- `README.md`
- `AGENTS.md` only if rules change

禁止修改文件：

- `backend/`
- `frontend/`
- `deploy/`
- `.github/`

依赖任务：implementation and verification handoff packets
输入契约：Handoff Packets from all tasks
输出契约：developer log, archive index, relevant docs updated
验证命令：`git diff --check`
是否可并行：可 prepare drafts, final write must happen after integration
合并顺序：8
Handoff 要求：list changed docs, latest archive version, known unarchived gaps。

## 10. Dispatch Prompt Template

Use this template when sending one task to another AI tool:

```text
你正在参与 WikiForge 项目开发。

开始前必须阅读：
1. AGENTS.md
2. docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-v1.5.md 的 Version Index 和当前阶段结论
3. docs/ai-skills/wikiforge-development/SKILL.md
4. docs/ai-skills/wikiforge-development/references/[对应 reference]
5. docs/superpowers/plans/2026-05-23-MVP1契约冻结并行工作单-WikiForge-mvp1-contract-freeze-parallel-work-order.md 中分配给你的任务

你的任务ID：[填入 PWO 任务ID]
你的角色：[填入专家角色]
允许修改文件：[从 PWO 粘贴]
禁止修改文件：[从 PWO 粘贴]
依赖契约：[从 PWO 粘贴]
验证命令：[从 PWO 粘贴]

完成后输出 Handoff Packet：
任务ID：
状态：DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED
完成内容：
实际修改文件：
契约变更：
验证命令和结果：
未验证原因：
风险：
需要主编排 Agent 集成的事项：
```

## 11. Go / No-Go

Go 条件：

- PWO-MVP1-CF-001 已确认。
- PWO-MVP1-COMMON-001 已完成或明确由 Core Agent 串行先做。
- Migration 编号固定为 `V20260523_002__create_source_import_tables.sql`。
- API paths and DTO names from sections 3 and 4 are not changed by implementation agents.

No-Go 条件：

- 任一 Agent 需要修改另一个 Agent 的禁止文件。
- 任一 Agent 要直接跳过 Core 内部 API 写 Core 表。
- 任一 Agent 要引入 Redis、Kafka、Nacos、XXL-JOB 或新网关。
- Path safety rules are disputed or incomplete.
