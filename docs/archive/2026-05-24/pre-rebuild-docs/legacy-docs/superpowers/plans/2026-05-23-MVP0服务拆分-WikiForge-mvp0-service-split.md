# 2026-05-23 WikiForge MVP0 服务拆分 Service Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前单 `backend` Spring Boot 骨架拆分为 `wikiforge-common`、`wikiforge-core-service`、`wikiforge-worker-service` 三个 Maven 模块，并同步更新 Docker、Compose 与 CI。

**Architecture:** 采用少服务微服务骨架。Core 作为 MVP 对外 API 入口和数据库迁移归属服务，Worker 作为文件处理任务执行服务，UI 只调用 Core。

**Tech Stack:** Java 21, Spring Boot 3.3.x, Maven, MyBatis-Plus 3.5.x, Flyway, MySQL 8, Vue 3, Vite, Docker Compose, GitHub Actions.

---

## Work Order

```text
任务名称：MVP0 后端拆分为 common/core/worker
目标服务：wikiforge-common, wikiforge-core-service, wikiforge-worker-service, wikiforge-ui
目标文件：backend/pom.xml, backend/wikiforge-*/, deploy/docker/*.Dockerfile, deploy/docker-compose*.yml, .github/workflows/ci.yml, frontend/nginx.conf
依赖契约：Core 暴露 /api/health 和 /actuator/health；Worker 暴露 /api/v1/worker/health 和 /actuator/health；UI /api 只代理到 Core
数据表归属：system_settings, model_providers 的 Flyway migration 归属 Core
验证命令：mvn -B test, mvn -B -DskipTests package, npm run build, docker compose config
是否需要归档：是
```

## File Structure

```text
backend/
  pom.xml
  wikiforge-common/
  wikiforge-core-service/
  wikiforge-worker-service/
deploy/
  docker/
    core-service.Dockerfile
    worker-service.Dockerfile
    frontend.Dockerfile
  docker-compose.yml
  docker-compose.dev.yml
```

## Task 1: Backend Module Tests

**Files:**
- Create: `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/WikiForgeCoreApplicationTests.java`
- Create: `backend/wikiforge-worker-service/src/test/java/com/wikiforge/worker/WikiForgeWorkerApplicationTests.java`

- [ ] Add Core Spring context and `/api/health` contract test.
- [ ] Add Worker Spring context and `/api/v1/worker/health` contract test.
- [ ] Run module tests and confirm they fail before production classes exist.

## Task 2: Maven Multi Module Backend

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/wikiforge-common/pom.xml`
- Create: `backend/wikiforge-core-service/pom.xml`
- Create: `backend/wikiforge-worker-service/pom.xml`

- [ ] Convert `backend/pom.xml` to parent POM with modules.
- [ ] Move common response classes into `wikiforge-common`.
- [ ] Configure Core dependencies for web, validation, actuator, MyBatis-Plus, MySQL, Flyway, springdoc and tests.
- [ ] Configure Worker dependencies for web, validation, actuator and tests.

## Task 3: Core And Worker Service Skeleton

**Files:**
- Create: `backend/wikiforge-common/src/main/java/com/wikiforge/common/web/ApiResponse.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/WikiForgeCoreApplication.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/config/MybatisPlusConfig.java`
- Create: `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/interfaces/web/HealthController.java`
- Create: `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/WikiForgeWorkerApplication.java`
- Create: `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/interfaces/web/WorkerHealthController.java`

- [ ] Implement the minimal Core application and health endpoint.
- [ ] Implement the minimal Worker application and health endpoint.
- [ ] Move Flyway migration and Core application config under Core service.
- [ ] Add Worker application config with its own port and actuator health.

## Task 4: Docker, Compose, CI

**Files:**
- Create: `deploy/docker/core-service.Dockerfile`
- Create: `deploy/docker/worker-service.Dockerfile`
- Modify: `deploy/docker-compose.yml`
- Modify: `deploy/docker-compose.dev.yml`
- Modify: `frontend/nginx.conf`
- Modify: `.github/workflows/ci.yml`
- Modify: `.env.example`

- [ ] Build Core and Worker images from their own modules.
- [ ] Compose services become `wikiforge-core-service`, `wikiforge-worker-service`, `wikiforge-ui`, `mysql`.
- [ ] UI proxies `/api` and `/actuator` to Core only.
- [ ] CI tests and packages all backend modules, then builds Core, Worker and UI images.

## Task 5: Verification And Archive

**Files:**
- Modify: `docs/current/2026-05-23-开发者日志-WikiForge-developer-log.md`
- Modify/Add: `docs/archive/2026-05-23/*`

- [ ] Run backend test and package with local Maven repository.
- [ ] Run frontend build.
- [ ] Run Compose config validation.
- [ ] Validate WikiForge development Skill.
- [ ] Update developer log and archive index with this service split snapshot.
