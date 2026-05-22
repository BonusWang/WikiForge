# WikiForge 开发工作流 Development Workflow

## 1. Work Order

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

## 2. Contract First

以下变更必须先改契约文档，再改实现：

- REST API path。
- Request / response DTO。
- 数据表归属。
- 状态枚举。
- 错误码。
- 跨服务调用。
- Docker Compose 服务名和端口。

## 3. Branch And Scope

每个开发切片只处理一个主要目标。

推荐分支命名：

```text
codex/mvp0-service-split
codex/mvp1-source-import
codex/mvp1-worker-hash
codex/docs-architecture-update
```

不要把架构重构、业务功能、UI 改版、CI 优化混在同一个切片里。

## 4. Implementation Order

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

## 5. Verification

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

## 6. Completion Report

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
