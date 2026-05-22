# WikiForge 后端 DDD 标准 Backend DDD Standard

## 模块结构

后端采用 Maven monorepo：

```text
backend/
  pom.xml
  wikiforge-common/
  wikiforge-core-service/
  wikiforge-worker-service/
  wikiforge-gateway/        # 后续预留
```

服务内部包结构：

```text
com.wikiforge.{service}
  interfaces/
    web/
    facade/
    assembler/
  application/
    service/
    dto/
    command/
    query/
  domain/
    model/
    service/
    repository/
    event/
  infrastructure/
    persistence/
    filesystem/
    integration/
    config/
```

## 分层依赖

允许：

- `interfaces` 调用 `application`。
- `application` 编排 `domain`。
- `infrastructure` 实现 `domain.repository`。
- `application` 使用仓储接口，不直接依赖 MyBatis Mapper。

禁止：

- `domain` 依赖 Spring 注解、MyBatis、HTTP、文件系统、LLM SDK。
- Controller 直接写业务逻辑。
- Mapper 直接暴露给 Controller。
- DTO 混入领域对象长期使用。

## 公共模块

`wikiforge-common` 放置：

- `ApiResponse`
- `PageRequest`
- `PageResult`
- `BusinessException`
- `ErrorCode`
- 通用时间、路径、hash、JSON 工具
- 基础安全校验工具

不要把业务模型放进 common。

## API 约定

- 对外 REST 使用 `/api/v1/{domain}`。
- 健康检查使用 `/actuator/health`。
- 响应体统一使用 `ApiResponse<T>`。
- 参数校验使用 Spring Validation。
- 错误码格式：`{DOMAIN}_{NNN}`，如 `SOURCE_001`、`IMPORT_001`。

## 数据访问

- 使用 MyBatis-Plus 3.5.x。
- 简单 CRUD 用 MyBatis-Plus。
- 复杂查询使用 MyBatis XML 或自定义 Mapper。
- Flyway migration 按服务或归属域拆分命名。
- MVP 阶段共享 MySQL 实例，但表归属必须在数据模型中写明。

## 耗时任务

文件扫描、复制、hash、解析、AI 调用、向量化都属于耗时任务。

MVP 做法：

- Core 创建任务。
- Worker 执行任务。
- 任务状态可查询。
- 每个步骤记录失败原因。

后续可升级为消息队列或调度中心，但不得改变任务状态模型。
