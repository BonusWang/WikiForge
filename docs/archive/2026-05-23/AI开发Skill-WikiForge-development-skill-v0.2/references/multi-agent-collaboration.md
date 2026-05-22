# WikiForge 多人和多 AI 协作 Multi Agent Collaboration

## 工作前声明

每个开发者或 AI Agent 开始前必须说明：

- 目标服务或文档。
- 预计修改文件。
- 依赖的 API、表、配置或文档。
- 验证命令。
- 是否需要与其他 Agent 串行。

## 文件边界

默认按服务边界分配文件：

- UI Agent：`frontend/`
- Core Agent：`backend/wikiforge-core-service/`
- Worker Agent：`backend/wikiforge-worker-service/`
- Common Agent：`backend/wikiforge-common/`
- Docs Agent：`docs/`
- DevOps Agent：`.github/`、`deploy/`

跨边界修改必须在回复里说明原因。

## 契约优先

多人并行时，先改契约再改实现：

- REST API path。
- request / response DTO。
- 数据表归属。
- 状态枚举。
- 错误码。
- 事件或任务状态模型。

契约变更必须同步文档。

## 文档快照

关键变更必须同步：

- 当前主文档。
- `docs/archive/YYYY-MM-DD/` 日期快照。
- 开发者日志。
- 归档索引。

如果只改代码但不更新对应规约，后续 AI 容易跑偏。

## 冲突处理

发现以下情况必须暂停并说明：

- 最新快照与当前主文档冲突。
- 用户新指令与架构决策冲突。
- 两个 Agent 同时修改同一服务边界核心文件。
- 需要跨服务直接读写数据。
- 需要提前引入 MVP 暂缓的中间件。

## 完成报告

完成时必须列出：

- 修改范围。
- 影响服务。
- 验证命令和结果。
- 未完成项。
- 下一步建议。
