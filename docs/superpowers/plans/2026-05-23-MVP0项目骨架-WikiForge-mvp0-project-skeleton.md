# 2026-05-23 WikiForge MVP0 项目骨架 Project Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the WikiForge MVP 0 engineering skeleton so backend, frontend, MySQL migration, CI, and Docker Compose have a verifiable baseline.

**Architecture:** Use a frontend/backend separated repository layout. Backend is a single Spring Boot module with MyBatis-Plus, Flyway, Actuator, and MVP 0 tables. Frontend is a Vue 3 + Vite + TypeScript app with a minimal dashboard that can call backend health APIs.

**Tech Stack:** Java 21/17, Spring Boot 3.x, Maven, MyBatis-Plus 3.5.x, Flyway, MySQL 8, Vue 3, Vite, TypeScript, Element Plus, Pinia, Vue Router, Axios, Docker Compose, GitHub Actions.

---

## File Structure

- `backend/`: Spring Boot backend application.
- `backend/src/main/resources/db/migration/`: Flyway SQL migrations.
- `frontend/`: Vue 3 + Vite frontend application.
- `frontend/nginx.conf`: Container nginx reverse proxy for `/api`.
- `deploy/docker/backend.Dockerfile`: Backend image.
- `deploy/docker/frontend.Dockerfile`: Frontend image.
- `deploy/docker-compose.yml`: Production-like local compose stack.
- `deploy/docker-compose.dev.yml`: MySQL-only development compose stack.
- `.github/workflows/ci.yml`: CI for backend, frontend, Docker build.
- `.env.example`: Runtime configuration template.

## Task 1: Backend Skeleton

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/wikiforge/WikiForgeApplication.java`
- Create: `backend/src/main/java/com/wikiforge/config/MybatisPlusConfig.java`
- Create: `backend/src/main/java/com/wikiforge/web/ApiResponse.java`
- Create: `backend/src/main/java/com/wikiforge/web/HealthController.java`
- Create: `backend/src/test/java/com/wikiforge/WikiForgeApplicationTests.java`

- [x] Create a Spring Boot project with web, validation, actuator, MyBatis-Plus, MySQL driver, Flyway, springdoc-openapi, and test dependencies.
- [x] Add `WikiForgeApplication`.
- [x] Add MyBatis-Plus pagination configuration.
- [x] Add `/api/health` returning a simple JSON response.
- [x] Add a Spring context load test.
- [x] Verify with `mvn test`.

## Task 2: Backend Configuration And Flyway

**Files:**
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/db/migration/V20260523_001__create_mvp0_tables.sql`
- Create: `backend/src/test/resources/application-test.yml`

- [x] Configure application name, datasource, Flyway, MyBatis-Plus, Actuator, and WikiForge path settings from environment variables.
- [x] Create MVP 0 tables: `system_settings` and `model_providers`.
- [x] Configure tests to run without requiring a local MySQL connection.
- [x] Verify SQL is syntactically readable and packaged.

## Task 3: Frontend Skeleton

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/stores/app.ts`
- Create: `frontend/src/services/http.ts`
- Create: `frontend/src/views/DashboardView.vue`
- Create: `frontend/src/styles/main.css`

- [x] Create a Vue 3 + Vite + TypeScript app.
- [x] Add Element Plus, Pinia, Vue Router, and Axios.
- [x] Add a minimal dashboard with MVP 0 status cards.
- [x] Configure `/api` proxy for local development.
- [x] Verify with `npm run build`.

## Task 4: Docker And Compose

**Files:**
- Create: `deploy/docker/backend.Dockerfile`
- Create: `deploy/docker/frontend.Dockerfile`
- Create: `frontend/nginx.conf`
- Create: `deploy/docker-compose.yml`
- Create: `deploy/docker-compose.dev.yml`
- Create: `.env.example`

- [x] Add backend multi-stage Dockerfile.
- [x] Add frontend multi-stage Dockerfile with nginx.
- [x] Add nginx config that proxies `/api` to backend.
- [x] Add compose stack for mysql, backend, frontend.
- [x] Add dev compose stack for mysql only.
- [x] Add environment template.

## Task 5: CI Baseline

**Files:**
- Create: `.github/workflows/ci.yml`

- [x] Add backend test/package job.
- [x] Add frontend build job.
- [x] Add Docker build job for backend and frontend images.

## Task 6: Documentation Log

**Files:**
- Modify: `docs/current/2026-05-23-开发者日志-WikiForge-developer-log.md`
- Add archive snapshots under `docs/archive/2026-05-23/`

- [x] Record that MVP 0 development started.
- [x] Record created skeleton areas and verification results.
- [x] Update latest archive index if new snapshots are created.

## Verification

Run what is locally available:

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm install
npm run build
```

```powershell
docker compose -f deploy/docker-compose.yml config
```

If a tool is not installed or network dependency resolution fails, record it in the final report.

## Verification Result

Completed on 2026-05-23:

- Backend test passed with local Maven repository: `mvn -s %TEMP%\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\repository" test`.
- Backend package passed with local Maven repository: `mvn -s %TEMP%\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\repository" -B -DskipTests package`.
- Frontend dependency install passed: `npm ci`.
- Frontend build passed: `npm run build`.
- Compose syntax validation passed: `docker compose -f deploy/docker-compose.yml config`.
- Development compose syntax validation passed: `docker compose -f deploy/docker-compose.dev.yml config`.
- Docker image build was attempted, but local Docker Desktop Linux engine was not running: `dockerDesktopLinuxEngine` pipe not found.

Notes:

- The local global Maven settings were bypassed with a temporary Maven settings file because the configured mirror was unreachable in this environment.
- In PowerShell, quote the Maven local repository argument: `"-Dmaven.repo.local=E:\repository"`.
- Vite reported a non-blocking large chunk warning from the initial Element Plus dashboard bundle; this is acceptable for MVP 0 and can be optimized when the dashboard grows.
