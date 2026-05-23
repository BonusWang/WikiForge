# WikiForge 开发工作流 Development Workflow

## 1. Symphony-inspired Task Control

WikiForge 采用轻量 Symphony-inspired 工作模式：

- GitHub Issue 或 Issue 风格任务卡是任务控制平面。
- `WORKFLOW.md` 是工作流入口，定义任务状态、分支隔离、Handoff 和完成定义。
- `docs/current/` 和 `docs/archive/` 是产品、架构和历史决策事实来源。
- `docs/superpowers/plans/` 是可执行 Work Order。
- 当前不引入 Symphony 服务端、独立调度数据库或自动 Codex runner。

开始任何开发切片前，先确认是否已有任务卡：

```text
任务ID：
父任务：
当前状态：
Owner：
目标：
范围：
允许修改文件：
禁止修改文件：
输入契约：
输出契约：
验收命令：
文档更新：
风险：
下一步：
```

没有真实 GitHub Issue 时，也必须在 Work Order 中保留同等字段。

## 2. Work Order

每个开发切片开始前，先写清楚：

```text
任务名称：
目标服务：
目标文件：
依赖契约：
数据表归属：
验证命令：
是否需要归档：
```

并行开发时必须升级为 Parallel Work Order v2：

```text
任务ID：
父任务：
专家角色：
目标服务或文档：
允许修改文件：
禁止修改文件：
依赖任务：
输入契约：
输出契约：
验证命令：
是否可并行：
合并顺序：
Handoff 要求：
```

示例：

```text
任务名称：MVP0 后端拆分为 common/core/worker
目标服务：wikiforge-common, wikiforge-core-service, wikiforge-worker-service
目标文件：backend/pom.xml, backend/wikiforge-*/
依赖契约：Core / Worker health API, Docker Compose service name
数据表归属：system_settings, model_providers 属于 Core
验证命令：mvn -B test, docker compose config
是否需要归档：是
```

## 3. Contract First

以下变更必须先改契约文档，再改实现：

- REST API path。
- Request / response DTO。
- 数据表归属。
- 状态枚举。
- 错误码。
- 跨服务调用。
- Docker Compose 服务名和端口。

并行开发时，以下内容未冻结前不得分派实现任务：

- REST API 前缀，例如统一使用 `/api/v1/{domain}`。
- Core -> Worker 请求和响应 DTO。
- Flyway migration 编号。
- `sources`、`source_files`、`import_jobs` 表结构和归属。
- MVP1 状态枚举。
- 路径安全规则、允许扫描根目录、符号链接策略、大文件阈值。

## 4. Branch And Scope

每个开发切片只处理一个主要目标。

推荐分支命名：

```text
codex/mvp0-service-split
codex/mvp1-source-import
codex/mvp1-worker-hash
codex/docs-architecture-update
```

不要把架构重构、业务功能、UI 改版、CI 优化混在同一个切片里。

## 4.1 Git Commit Rules

提交前必须确认没有依赖目录、编译产物、运行数据或本地配置进入 Git。

禁止提交：

- `frontend/node_modules/`
- `frontend/dist/`
- `frontend/.vite/`
- `backend/**/target/`
- `backend/**/build/`
- `.env`
- `logs/`
- `data/`
- `WikiForge_RawSources/`
- `WikiForge_Vault/`

允许提交：

- 源码。
- 文档。
- `package-lock.json`。
- `.env.example`。
- Dockerfile。
- GitHub Actions workflow。
- Flyway migration。

提交前检查：

```text
git status --short
git diff --check
```

如果发现 `node_modules` 或其他编译产物被暂存，必须先移出暂存区再提交。

## 4.2 Developer Log And Archive Rules

同一天内多次更新开发日志或归档快照时，使用滚动版本文件，避免为每次小改动创建大量文件。

规则：

- 同一日期、同一文档类型，只维护当天最新版本文件。
- 在文件内部追加“版本记录 / Version History”或新的日期小节，记录每次变更。
- 文件名版本号每次递增 `0.1`：`v0.1 -> v0.2 -> ... -> v0.9 -> v1.0 -> v1.1`。
- 当小版本累计到 10 次时提升大版本，不能出现 `v0.10`。
- 归档索引只指向最新版本文件，并在说明中概括该文件内包含的多次更新。
- 已经存在的旧版本文件作为历史保留，不再主动批量删除或改写。
- 如果文件内版本记录过多导致上下文变大，必须在文件顶部增加“版本索引 / Version Index”。
- AI 读取长归档文件时，默认先读版本索引和最新版本小节；只有需要追溯决策原因、历史争议或用户明确要求时，才读取旧版本内容。

版本索引建议格式：

```text
## 版本索引 Version Index

- 最新版本：v1.0
- 推荐阅读：先读“2026-05-23 归档规则上下文优化”小节
- 历史版本：v0.1-v0.9 仅在追溯需求演进时阅读
```

示例：

```text
2026-05-23-开发者日志-WikiForge-developer-log-v0.8.md
更新后重命名为：
2026-05-23-开发者日志-WikiForge-developer-log-v0.9.md
```

## 5. Implementation Order

后端服务拆分优先顺序：

1. Parent Maven POM。
2. `wikiforge-common`。
3. `wikiforge-core-service`。
4. `wikiforge-worker-service`。
5. Dockerfile。
6. Docker Compose。
7. CI。
8. 文档和归档。

业务功能优先顺序：

1. 数据模型和 Flyway。
2. Domain 模型。
3. Repository 接口。
4. Infrastructure persistence。
5. Application service。
6. Interfaces controller。
7. UI API 调用。
8. 页面状态和交互。
9. 验证和归档。

并行执行顺序：

1. 主编排 Agent 建立 Parallel Work Order。
2. 将节点拆成 Issue 风格任务卡，并标记 Ready / Doing / Review / Blocked / Done。
3. Contract API Designer 串行冻结 API / DTO / DDL / 状态枚举。
4. Core / Worker / UI 在不重叠文件边界下并行实现。
5. DevOps 根据已冻结环境变量和服务名更新部署。
6. Test QA 和 Review Agent 做只读验证或给出修复建议。
7. 主编排 Agent 汇总结果、解决冲突、运行全量验证。
8. Docs Agent 或主编排 Agent 更新开发日志和归档。

## 6. Verification

每个切片至少运行相关验证。

后端：

```text
mvn -B test
mvn -B -DskipTests package
```

前端：

```text
npm ci
npm run build
```

部署：

```text
docker compose -f deploy/docker-compose.yml config
docker compose -f deploy/docker-compose.dev.yml config
```

Skill：

```text
python <skill-creator>/scripts/quick_validate.py docs/ai-skills/wikiforge-development
```

## 7. Completion Report

完成时必须写：

```text
完成内容：
影响服务：
修改文件：
验证结果：
归档文件：
已知风险：
下一步：
```

如果验证未能完成，必须说明是代码问题还是环境问题。
