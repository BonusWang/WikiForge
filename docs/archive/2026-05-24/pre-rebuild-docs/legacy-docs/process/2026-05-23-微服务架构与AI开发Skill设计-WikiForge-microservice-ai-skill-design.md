# 2026-05-23 WikiForge 微服务架构与 AI 开发 Skill 设计 v0.1

## 1. 背景

用户补充确认：WikiForge 应采用微服务模式，并根据领域进行服务拆分，以支撑长期需求拓展和多人维护。

用户同时提供民生 CDP 项目的 AI 开发范式目录，要求只学习其中的架构样式和设计理念，不复制业务内容，并结合 WikiForge 生成对应 Skill。

## 2. 架构选择

最终选择 B 方案：少服务微服务。

MVP 0/1 目标服务：

```text
wikiforge-ui
wikiforge-core-service
wikiforge-worker-service
mysql
```

可预留但不强制运行：

```text
wikiforge-gateway
```

后续扩展服务：

```text
wikiforge-agent-service
wikiforge-connector-service
wikiforge-mcp-service
wikiforge-vector-service
wikiforge-record-service
```

## 3. 服务职责

### 3.1 wikiforge-core-service

职责：

- 系统配置。
- Source / SourceFile / ImportJob 元数据。
- 审核队列。
- Obsidian Note 映射。
- UI 对外 REST API。
- 创建任务并调用 Worker 执行。

### 3.2 wikiforge-worker-service

职责：

- 本地路径扫描。
- Raw Sources 复制归集。
- SHA-256 hash。
- 文件类型识别。
- 文档解析前置处理。
- 后续 OCR / Source Note 生成前置任务。

Worker 不承载用户查询入口，UI 不直接调用 Worker。

### 3.3 wikiforge-ui

职责：

- Console 看板。
- 导入任务、资料库、审核队列、Obsidian 预览。
- 后续办公室视图。

## 4. 从 CDP 范式吸收的设计理念

采用：

- 全局强约束、脚手架规范、服务条线规范的分层方式。
- 每个服务单独定义职责、包结构、API、数据表归属、异常码和跨服务调用。
- DDD 四层架构：`interfaces -> application -> domain <- infrastructure`。
- 多人或多 AI 开发前先声明目标服务、文件边界、依赖契约和验证命令。
- 耗时任务不能阻塞前台 API，必须进入 Worker / Job / Task 模式。

不采用：

- CDP 业务模型。
- CDP 的认证、标签、分群、OneId 等业务概念。
- Spring Boot 4.1.x 版本约束。
- MVP 0/1 中引入 Nacos、Kafka、Redis、XXL-JOB、TDSQL、StarRocks。

## 5. WikiForge AI 开发 Skill

已创建项目内 Skill：

```text
docs/ai-skills/wikiforge-development/SKILL.md
```

引用文件：

```text
docs/ai-skills/wikiforge-development/references/agent-role-prompts.md
docs/ai-skills/wikiforge-development/references/architecture-style.md
docs/ai-skills/wikiforge-development/references/service-boundaries.md
docs/ai-skills/wikiforge-development/references/backend-ddd-standard.md
docs/ai-skills/wikiforge-development/references/frontend-standard.md
docs/ai-skills/wikiforge-development/references/ci-docker-standard.md
docs/ai-skills/wikiforge-development/references/multi-agent-collaboration.md
docs/ai-skills/wikiforge-development/references/development-workflow.md
```

AGENTS.md 已补充规则：后续 AI 在进行架构、代码、评审、重构、CI/CD、MCP、Agent、Obsidian 或多人协作任务前，必须读取该 Skill。

## 6. 对 MVP 0 的影响

当前已存在单后端 MVP0 骨架，但目标架构已经调整。

下一步应重构为：

```text
backend/
  pom.xml
  wikiforge-common/
  wikiforge-core-service/
  wikiforge-worker-service/
  wikiforge-gateway/        # 后续预留
frontend/
deploy/
```

重构策略：

- 先保留当前已验证的健康检查和配置能力。
- 将通用响应体、异常、配置基础类迁入 `wikiforge-common`。
- 将 API、配置、Flyway、Source 状态管理放入 `wikiforge-core-service`。
- 将文件扫描、hash、Raw Sources 归集任务放入 `wikiforge-worker-service`。
- 更新 Docker Compose、CI 和 Dockerfile，使 Core / Worker 独立构建和健康检查。

## 7. 协作约定

多人开发时按服务边界分工：

- UI Agent：`frontend/`。
- Core Agent：`backend/wikiforge-core-service/`。
- Worker Agent：`backend/wikiforge-worker-service/`。
- Common Agent：`backend/wikiforge-common/`。
- DevOps Agent：`deploy/`、`.github/`。
- Docs Agent：`docs/`。

跨边界修改必须说明原因，并同步相关文档和归档快照。

## 8. 角色 Prompt 与 Work Order

为方便后续把任务交给其他 AI 工具或开发者，已补充角色 Prompt：

- Architect Agent：架构评审。
- Core Service Agent：核心业务 API。
- Worker Service Agent：文件任务执行。
- UI Agent：前端页面。
- DevOps Agent：CI/CD 与 Docker。
- Docs Agent：文档归档。
- Review Agent：代码评审。

每个开发切片必须先写 Work Order：

```text
任务名称：
目标服务：
目标文件：
依赖契约：
数据表归属：
验证命令：
是否需要归档：
```

该约定用于降低多人并行开发时的服务边界冲突和文件冲突。
