# MVP2 Obsidian Source Note Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the MVP2 loop that turns an already organized Source File into a Source Note Markdown draft, writes it into the configured Obsidian Vault, previews it in the Web UI, and exposes a safe `obsidian://open` URI.

**Architecture:** Core Service owns `obsidian_notes`, Source Note template rendering, Vault path validation, Markdown writing, preview reads, and public Obsidian APIs. Worker Service stays out of MVP2 unless a separate parsing task is explicitly approved. UI calls Core only.

**Tech Stack:** Java 21/17, Spring Boot 3.x, MyBatis-Plus, Flyway, MySQL 8.x, JUnit 5, Vue 3, Vite, TypeScript, Element Plus, Axios, Docker Compose.

---

## Current Context

- Current version baseline: `0.03`, MVP1 local source ingestion is complete.
- Confirmed local Obsidian Vault host path: `E:\WikiForgeVault`.
- Docker container Vault path: `/data/wikiforge/obsidian-vault`.
- Docker host Vault mapping variable: `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH`.
- MVP2 must not introduce AI summarization, vector database, MCP server, online document connectors, or multi-agent office view.
- MVP2 writes metadata-based Source Notes only; generated content can contain "待补充" placeholders for summary and key content.

## Implementation Status

- 状态：已在 `codex/mvp2-obsidian-source-note` 完成后端、前端、测试、Docker 和浏览器验证。
- 契约修正：项目现有 `ApiResponse.ok(...)` 成功响应 `code` 为 `null`，不是示例中的 `"OK"`。
- MySQL 修正：`idx_obsidian_notes_vault_path` 使用 `vault_path(255)` 前缀索引，避免 `utf8mb4` 下完整索引超长。
- 验证：`git diff --check`、`docker compose config`、`npm run build`、`mvn test`、`docker compose up -d --build`、Core/Worker health、`POST /api/v1/obsidian/init`、本地浏览器 UI 检查均通过。

## API Contract

### Initialize Vault

`POST /api/v1/obsidian/init`

Response:

```json
{
  "success": true,
  "code": null,
  "message": "ok",
  "data": {
    "vaultPath": "E:\\WikiForgeVault",
    "vaultName": "WikiForgeVault",
    "createdDirectories": [
      "00_Inbox_收集箱/Sources_来源",
      "00_Inbox_收集箱/Review_审核",
      "90_System_系统/AgentLogs_Agent日志",
      "90_System_系统/Indexes_索引",
      "90_System_系统/Templates_模板",
      "90_System_系统/Schemas_规范"
    ]
  }
}
```

### Generate Draft

`POST /api/v1/source-files/{fileUid}/obsidian-note/draft`

Response:

```json
{
  "success": true,
  "code": null,
  "message": "ok",
  "data": {
    "fileUid": "file_xxx",
    "sourceUid": "src_xxx",
    "title": "example.pdf",
    "vaultPath": "00_Inbox_收集箱/Sources_来源/example-source_xxx.md",
    "markdown": "---\nid: source_xxx\ntype: source\n...\n---\n# example.pdf\n..."
  }
}
```

### Write Draft

`POST /api/v1/source-files/{fileUid}/obsidian-note/write`

Request:

```json
{
  "markdown": "---\nid: source_xxx\n...\n---\n# example.pdf\n..."
}
```

Response:

```json
{
  "success": true,
  "code": null,
  "message": "ok",
  "data": {
    "noteUid": "note_20260523_ab12cd34ef56",
    "fileUid": "file_xxx",
    "sourceUid": "src_xxx",
    "vaultName": "WikiForgeVault",
    "vaultPath": "00_Inbox_收集箱/Sources_来源/example-source_xxx.md",
    "absolutePath": "E:\\WikiForgeVault\\00_Inbox_收集箱\\Sources_来源\\example-source_xxx.md",
    "obsidianUri": "obsidian://open?vault=WikiForgeVault&file=00_Inbox_%E6%94%B6%E9%9B%86%E7%AE%B1%2FSources_%E6%9D%A5%E6%BA%90%2Fexample-source_xxx.md",
    "status": "written"
  }
}
```

### Preview Note

`GET /api/v1/obsidian/notes/{noteUid}/preview`

Response:

```json
{
  "success": true,
  "code": null,
  "message": "ok",
  "data": {
    "noteUid": "note_20260523_ab12cd34ef56",
    "vaultPath": "00_Inbox_收集箱/Sources_来源/example-source_xxx.md",
    "markdown": "---\nid: source_xxx\n...\n---\n# example.pdf\n..."
  }
}
```

## File Structure

### Backend Core

- Create: `backend/wikiforge-core-service/src/main/resources/db/migration/V20260523_003__create_obsidian_notes.sql`
- Modify: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/MigrationSqlCompatibilityTests.java`
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/CoreRuntimeProperties.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/domain/model/ObsidianNote.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/domain/repository/ObsidianNoteRepository.java`
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/domain/repository/SourceFileRepository.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/ObsidianNoteEntity.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/ObsidianNoteMapper.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/MyBatisObsidianNoteRepository.java`
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/MyBatisSourceFileRepository.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/ObsidianInitResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/SourceNoteDraftResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/WriteSourceNoteRequest.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/ObsidianNoteResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/ObsidianNotePreviewResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/ObsidianVaultService.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/interfaces/web/ObsidianController.java`
- Test: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/ObsidianApiIntegrationTests.java`

### Frontend

- Create: `frontend/src/types/obsidianNotes.ts`
- Create: `frontend/src/api/obsidian/index.ts`
- Modify: `frontend/src/views/DashboardView.vue`

### Deploy And Docs

- Modify: `.env.example`
- Modify: `deploy/docker-compose.yml`
- Modify: `docs/current/技术架构-technical-architecture.md`
- Modify: `docs/current/2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md`
- Modify: `docs/current/2026-05-22-开发者日志-WikiForge-developer-log.md`
- Archive snapshots under `docs/archive/2026-05-23/`

---

## Task 1: Freeze MVP2 Data Contract

**Files:**
- Create: `backend/wikiforge-core-service/src/main/resources/db/migration/V20260523_003__create_obsidian_notes.sql`
- Modify: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/MigrationSqlCompatibilityTests.java`

- [ ] **Step 1: Add Flyway migration**

Create:

```sql
CREATE TABLE obsidian_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    note_uid VARCHAR(64) NOT NULL,
    source_id BIGINT NOT NULL,
    source_file_id BIGINT NULL,
    note_type VARCHAR(64) NOT NULL DEFAULT 'source_note',
    vault_name VARCHAR(128) NOT NULL,
    vault_path VARCHAR(1024) NOT NULL,
    absolute_path VARCHAR(2048) NOT NULL,
    obsidian_uri VARCHAR(2048) NOT NULL,
    title VARCHAR(512) NOT NULL,
    frontmatter_json JSON NULL,
    content_hash VARCHAR(128) NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'written',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_obsidian_notes_note_uid (note_uid),
    KEY idx_obsidian_notes_source_id (source_id),
    KEY idx_obsidian_notes_source_file_id (source_file_id),
    KEY idx_obsidian_notes_vault_path (vault_path(255)),
    CONSTRAINT fk_obsidian_notes_source FOREIGN KEY (source_id) REFERENCES sources(id),
    CONSTRAINT fk_obsidian_notes_source_file FOREIGN KEY (source_file_id) REFERENCES source_files(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 2: Add migration compatibility assertions**

Update `MigrationSqlCompatibilityTests` with:

```java
@Test
void obsidianNotesMigrationUsesIndexablePathColumns() throws Exception {
    try (var inputStream = getClass().getResourceAsStream(
            "/db/migration/V20260523_003__create_obsidian_notes.sql"
    )) {
        assertThat(inputStream).isNotNull();
        String migrationSql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(migrationSql).contains("vault_path VARCHAR(1024) NOT NULL");
        assertThat(migrationSql).contains("absolute_path VARCHAR(2048) NOT NULL");
        assertThat(migrationSql).contains("obsidian_uri VARCHAR(2048) NOT NULL");
        assertThat(migrationSql).contains("KEY idx_obsidian_notes_vault_path (vault_path(255))");
    }
}
```

- [ ] **Step 3: Run focused backend test**

Run:

```powershell
mvn -B -Dmaven.repo.local=E:\repository -pl wikiforge-core-service -Dtest=MigrationSqlCompatibilityTests test
```

Expected: PASS.

---

## Task 2: Add Vault Runtime Configuration

**Files:**
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/CoreRuntimeProperties.java`
- Modify: `.env.example`
- Modify: `deploy/docker-compose.yml`

- [ ] **Step 1: Add runtime getters**

Add methods:

```java
public String obsidianVaultPath() {
    return firstConfigured("wikiforge.obsidian-vault-path", "WIKIFORGE_OBSIDIAN_VAULT_PATH");
}

public String obsidianVaultName() {
    String configured = firstConfigured("wikiforge.obsidian-vault-name", "WIKIFORGE_OBSIDIAN_VAULT_NAME");
    if (configured != null) {
        return configured;
    }
    String vaultPath = obsidianVaultPath();
    if (vaultPath == null || vaultPath.isBlank()) {
        return "WikiForgeVault";
    }
    Path fileName = Path.of(vaultPath).normalize().getFileName();
    return fileName == null ? "WikiForgeVault" : fileName.toString();
}
```

Also add:

```java
import java.nio.file.Path;
```

- [ ] **Step 2: Keep Docker host path configurable**

Ensure `.env.example` contains:

```text
WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH=../data/obsidian-vault
WIKIFORGE_OBSIDIAN_VAULT_PATH=/data/wikiforge/obsidian-vault
WIKIFORGE_OBSIDIAN_VAULT_NAME=WikiForgeVault

# 当前开发者本机 Obsidian Vault 可设置为：
# WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH=E:/WikiForgeVault
```

Ensure `deploy/docker-compose.yml` mounts:

```yaml
- ${WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH:-../data/obsidian-vault}:/data/wikiforge/obsidian-vault
```

- [ ] **Step 3: Run Compose config check**

Run:

```powershell
docker compose -f deploy/docker-compose.yml config
```

Expected: config renders successfully and includes the `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH` default mapping.

---

## Task 3: Implement Obsidian Note Persistence

**Files:**
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/domain/model/ObsidianNote.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/domain/repository/ObsidianNoteRepository.java`
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/domain/repository/SourceFileRepository.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/ObsidianNoteEntity.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/ObsidianNoteMapper.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/MyBatisObsidianNoteRepository.java`
- Modify: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/MyBatisSourceFileRepository.java`

- [ ] **Step 1: Create domain model**

```java
package com.wikiforge.core.domain.model;

import java.time.LocalDateTime;

public record ObsidianNote(
        Long id,
        String noteUid,
        Long sourceId,
        Long sourceFileId,
        String noteType,
        String vaultName,
        String vaultPath,
        String absolutePath,
        String obsidianUri,
        String title,
        String frontmatterJson,
        String contentHash,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
```

- [ ] **Step 2: Add repository contract**

```java
package com.wikiforge.core.domain.repository;

import com.wikiforge.core.domain.model.ObsidianNote;
import java.util.Optional;

public interface ObsidianNoteRepository {
    ObsidianNote save(ObsidianNote note);
    Optional<ObsidianNote> findByNoteUid(String noteUid);
    Optional<ObsidianNote> findBySourceFileUid(String fileUid);
}
```

- [ ] **Step 3: Add SourceFile lookup contract**

Extend `SourceFileRepository`:

```java
Optional<SourceFileRecord> findByFileUid(String fileUid);
```

Add `Long sourceId` and `Long sourceFileId` to `SourceFileRecord` so Obsidian writing can persist foreign keys without leaking internal IDs to UI responses.

- [ ] **Step 4: Implement MyBatis persistence**

Follow the existing `ImportJobEntity`, `SourceFileEntity`, and mapper style. The repository must:

- Generate `noteUid` in service, not mapper.
- Return saved entity with generated `id`.
- Query by `note_uid`.
- Query by `source_files.file_uid` using `source_file_id`.

- [ ] **Step 5: Run backend compile**

Run:

```powershell
mvn -B -Dmaven.repo.local=E:\repository -pl wikiforge-core-service -DskipTests compile
```

Expected: compile succeeds.

---

## Task 4: Implement Source Note Draft And Vault Writer

**Files:**
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/ObsidianInitResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/SourceNoteDraftResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/WriteSourceNoteRequest.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/ObsidianNoteResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/dto/ObsidianNotePreviewResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/ObsidianVaultService.java`

- [ ] **Step 1: Add DTO records**

Use records consistent with existing DTO style:

```java
public record SourceNoteDraftResponse(
        String fileUid,
        String sourceUid,
        String title,
        String vaultPath,
        String markdown
) {
}
```

```java
public record WriteSourceNoteRequest(String markdown) {
}
```

```java
public record ObsidianNoteResponse(
        String noteUid,
        String fileUid,
        String sourceUid,
        String vaultName,
        String vaultPath,
        String absolutePath,
        String obsidianUri,
        String status
) {
}
```

- [ ] **Step 2: Implement Vault initialization**

`ObsidianVaultService.initializeVault()` must create:

```text
00_Inbox_收集箱/Sources_来源
00_Inbox_收集箱/Review_审核
01_Projects_项目
02_Areas_领域
03_Resources_资源/Topics_主题
03_Resources_资源/Entities_实体
03_Resources_资源/References_参考
04_Archives_归档
05_Actions_行动
06_Secrets_敏感资产
07_Records_个人记录
90_System_系统/AgentLogs_Agent日志
90_System_系统/Templates_模板
90_System_系统/Indexes_索引
90_System_系统/Schemas_规范
```

It must resolve the configured Vault path with `PathSafety.normalizeAbsolute(Path.of(runtimeProperties.obsidianVaultPath()))`.

- [ ] **Step 3: Implement Markdown draft template**

Generated Markdown must match this shape:

```markdown
---
id: source_xxx
type: source
title: "example.pdf"
source_type: file
source_platform: local
source_url: ""
imported_from: path_scan
original_path: "E:\\source\\example.pdf"
managed_path: "E:\\raw\\01_Documents_文档\\example.pdf"
content_hash: "abc123"
status: pending_review
risk_level: low
priority: normal
projects: []
topics: []
tags: []
summary: ""
review_required: true
created_at: 2026-05-23
updated_at: 2026-05-23
---

# example.pdf

## 摘要

待生成或人工补充。

## 来源

- 原始路径：E:\\source\\example.pdf
- 归集路径：E:\\raw\\01_Documents_文档\\example.pdf
- 文件类型：pdf
- 内容 Hash：abc123

## 分类

- 项目：
- 主题：
- 标签：

## 关键内容

待生成或人工补充。

## 处理记录

- 导入状态：organized
- 归集状态：copied
- 审核状态：pending_review
```

- [ ] **Step 4: Write Markdown safely**

`writeSourceNote(fileUid, markdown)` must:

- Resolve `vaultPath` under `00_Inbox_收集箱/Sources_来源`.
- Slug filename as `<safe-title>-<sourceUid>.md`.
- Reject `..`, absolute relative paths, and paths outside the Vault.
- Create parent directories before writing.
- Write to `*.wf.tmp`, then atomically move to final path with `StandardCopyOption.REPLACE_EXISTING`.
- Store `obsidian_notes` row.
- Return `ObsidianNoteResponse`.

- [ ] **Step 5: Build `obsidian://open` safely**

Generate:

```text
obsidian://open?vault=<encodedVaultName>&file=<encodedVaultRelativePath>
```

Use `URLEncoder.encode(value, StandardCharsets.UTF_8)` and replace `+` with `%20`.

- [ ] **Step 6: Run service tests after controller task exists**

Run:

```powershell
mvn -B -Dmaven.repo.local=E:\repository -pl wikiforge-core-service -Dtest=ObsidianApiIntegrationTests test
```

Expected: PASS.

---

## Task 5: Add Obsidian REST APIs

**Files:**
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/interfaces/web/ObsidianController.java`
- Test: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/ObsidianApiIntegrationTests.java`

- [ ] **Step 1: Write integration tests first**

Create tests covering:

- `POST /api/v1/obsidian/init` creates directories under a temporary Vault.
- `POST /api/v1/source-files/{fileUid}/obsidian-note/draft` returns Markdown with frontmatter and file metadata.
- `POST /api/v1/source-files/{fileUid}/obsidian-note/write` writes a `.md` file, inserts `obsidian_notes`, and returns an encoded `obsidian://open` URI.
- `GET /api/v1/obsidian/notes/{noteUid}/preview` reads Markdown only from inside the Vault.

- [ ] **Step 2: Create controller mappings**

```java
@RestController
@RequestMapping("/api/v1")
public class ObsidianController {

    private final ObsidianVaultService obsidianVaultService;

    public ObsidianController(ObsidianVaultService obsidianVaultService) {
        this.obsidianVaultService = obsidianVaultService;
    }

    @PostMapping("/obsidian/init")
    public ApiResponse<ObsidianInitResponse> initializeVault() {
        return ApiResponse.ok(obsidianVaultService.initializeVault());
    }

    @PostMapping("/source-files/{fileUid}/obsidian-note/draft")
    public ApiResponse<SourceNoteDraftResponse> generateDraft(@PathVariable String fileUid) {
        return ApiResponse.ok(obsidianVaultService.generateDraft(fileUid));
    }

    @PostMapping("/source-files/{fileUid}/obsidian-note/write")
    public ApiResponse<ObsidianNoteResponse> writeNote(
            @PathVariable String fileUid,
            @RequestBody WriteSourceNoteRequest request
    ) {
        return ApiResponse.ok(obsidianVaultService.writeSourceNote(fileUid, request.markdown()));
    }

    @GetMapping("/obsidian/notes/{noteUid}/preview")
    public ApiResponse<ObsidianNotePreviewResponse> preview(@PathVariable String noteUid) {
        return ApiResponse.ok(obsidianVaultService.preview(noteUid));
    }
}
```

- [ ] **Step 3: Run integration tests**

Run:

```powershell
mvn -B -Dmaven.repo.local=E:\repository -pl wikiforge-core-service -Dtest=ObsidianApiIntegrationTests test
```

Expected: PASS.

---

## Task 6: Add UI Preview And Open Flow

**Files:**
- Create: `frontend/src/types/obsidianNotes.ts`
- Create: `frontend/src/api/obsidian/index.ts`
- Modify: `frontend/src/views/DashboardView.vue`

- [ ] **Step 1: Add frontend types**

```ts
export interface SourceNoteDraftResponse {
  fileUid: string
  sourceUid: string
  title: string
  vaultPath: string
  markdown: string
}

export interface ObsidianNoteResponse {
  noteUid: string
  fileUid: string
  sourceUid: string
  vaultName: string
  vaultPath: string
  absolutePath: string
  obsidianUri: string
  status: string
}
```

- [ ] **Step 2: Add API client**

```ts
import http from '@/services/http'
import type { ApiResponse } from '@/services/http'
import type { ObsidianNoteResponse, SourceNoteDraftResponse } from '@/types/obsidianNotes'

export async function initializeObsidianVault() {
  const response = await http.post<ApiResponse<unknown>>('/obsidian/init')
  return response.data.data
}

export async function generateSourceNoteDraft(fileUid: string) {
  const response = await http.post<ApiResponse<SourceNoteDraftResponse>>(
    `/source-files/${fileUid}/obsidian-note/draft`
  )
  return response.data.data
}

export async function writeSourceNote(fileUid: string, markdown: string) {
  const response = await http.post<ApiResponse<ObsidianNoteResponse>>(
    `/source-files/${fileUid}/obsidian-note/write`,
    { markdown }
  )
  return response.data.data
}
```

- [ ] **Step 3: Update Dashboard file table actions**

Add action buttons for each non-failed source file:

```vue
<el-button size="small" @click="handleGenerateDraft(row)">预览 Note</el-button>
<el-button size="small" type="primary" @click="handleWriteNote(row)">写入 Vault</el-button>
```

Use an `el-drawer` or `el-dialog` to show Markdown preview. Keep the UI compact; do not create a marketing-style page.

- [ ] **Step 4: Open Obsidian URI after successful write**

After write returns `obsidianUri`, show a button:

```vue
<el-button tag="a" :href="writtenNote.obsidianUri">打开 Obsidian</el-button>
```

Vue binding must keep `href` from string interpolation in raw HTML.

- [ ] **Step 5: Build frontend**

Run:

```powershell
npm run build
```

from `frontend/`.

Expected: PASS.

---

## Task 7: Full Verification And Docker Smoke

**Files:**
- Existing backend, frontend, deploy, and docs files touched only when verification reveals a concrete issue.

- [ ] **Step 1: Run backend tests**

Run:

```powershell
mvn -B -Dmaven.repo.local=E:\repository test
```

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run:

```powershell
npm run build
```

from `frontend/`.

Expected: PASS.

- [ ] **Step 3: Validate Docker Compose**

Run:

```powershell
docker compose -f deploy/docker-compose.yml config
```

Expected: PASS.

- [ ] **Step 4: Docker smoke with user Vault**

Create or update local `.env` only on the developer machine:

```text
WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH=E:/WikiForgeVault
```

Run:

```powershell
docker compose -f deploy/docker-compose.yml up -d --build
```

Expected:

- `wikiforge-core-service` healthy.
- `wikiforge-worker-service` healthy.
- `wikiforge-ui` healthy.
- `POST /api/v1/obsidian/init` creates directories under `E:\WikiForgeVault`.

---

## Task 8: Docs And Archive

**Files:**
- Modify: `docs/current/2026-05-22-开发者日志-WikiForge-developer-log.md`
- Modify: `docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-vX.Y.md`
- Create or update latest snapshots for changed current docs.

- [ ] **Step 1: Update developer log**

Record:

- MVP2 plan creation.
- Confirmed Vault path `E:\WikiForgeVault`.
- `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH` mapping.
- Verification results.

- [ ] **Step 2: Update archive snapshots**

Use same-day rolling version rule:

```text
2026-05-23-开发者日志-WikiForge-developer-log-v1.8.md
2026-05-23-归档索引-archive-index-v1.8.md
2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.5.md
2026-05-23-技术架构-technical-architecture-v0.6.md
```

- [ ] **Step 3: Run final doc check**

Run:

```powershell
git diff --check
rg -n "docs/需求文档|docs/技术架构|docs/数据模型|docs/2026-05-22-MVP" README.md AGENTS.md docs
```

Expected: no stale references in current project entry files; historical archive references may remain.

---

## Self-Review

### Spec Coverage

- Local Vault path is captured and used through environment mapping.
- MVP2 is scoped to Obsidian Source Note only.
- Core owns APIs and database writes.
- Worker remains unchanged for this phase.
- UI can preview, write, and open Obsidian notes.
- Docker and CI/CD constraints remain compatible with local and container deployment.

### Placeholder Scan

This plan intentionally allows "待生成或人工补充" inside generated Markdown because MVP2 has no AI summarization. That phrase is part of the user-facing Source Note template, not an unfinished implementation instruction.

### Type Consistency

- Public ID names: `fileUid`, `sourceUid`, `noteUid`.
- Database foreign keys: `source_id`, `source_file_id`.
- Vault path fields: `vaultName`, `vaultPath`, `absolutePath`, `obsidianUri`.
- Status value for written notes: `written`.
