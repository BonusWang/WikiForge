# WikiForge 多人和多 AI 协作 Multi Agent Collaboration

## 主编排规则

并行开发必须有一个主编排 Agent。主编排 Agent 负责：

- 拆解任务清单。
- 判断哪些任务可并行、哪些必须串行。
- 选择合适专家角色。
- 分配文件边界和禁止修改范围。
- 收集子 Agent Handoff Packet。
- 运行最终组合验证。
- 统一更新文档、开发者日志和归档索引。

子 Agent 不默认继承完整上下文。主编排 Agent 只提供最小上下文包：

```text
AGENTS.md
最新归档索引的版本索引和当前阶段结论
docs/ai-skills/wikiforge-development/SKILL.md
对应 role prompt
对应 reference 文档
Parallel Work Order 中分配给该 Agent 的任务
目标文件列表
```

除非任务需要追溯历史决策，子 Agent 不读取旧归档全文。

## 工作前声明

每个开发者或 AI Agent 开始前必须说明：

- 目标服务或文档。
- 预计修改文件。
- 依赖的 API、表、配置或文档。
- 验证命令。
- 是否需要与其他 Agent 串行。

## Parallel Work Order v2

并行开发前必须先写 Parallel Work Order：

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

并行判定：

- 契约未冻结前，不允许实现任务并行。
- 同一服务核心文件不得多人同时修改。
- 不同服务、不同文件、只依赖已冻结契约的任务可以并行。
- Review、Docs、DevOps 可以与实现并行准备，但最终落文件必须由主编排 Agent 排序。

推荐顺序：

```text
契约/DDL/状态枚举
  -> Core / Worker / UI 并行实现
  -> DevOps / CI 适配
  -> 集成验证
  -> Review
  -> Docs / Archive
```

## 文件边界

默认按服务边界分配文件：

- UI Agent：`frontend/`
- Core Agent：`backend/wikiforge-core-service/`
- Worker Agent：`backend/wikiforge-worker-service/`
- Common Agent：`backend/wikiforge-common/`
- Docs Agent：`docs/`
- DevOps Agent：`.github/`、`deploy/`

跨边界修改必须在回复里说明原因。

高冲突串行区：

- `backend/pom.xml`
- `backend/wikiforge-common/`
- Flyway migration
- 共享 DTO
- 错误码
- 状态枚举
- `deploy/docker-compose*.yml`
- `.github/workflows/ci.yml`
- `.env.example`

高冲突串行区同一时间只允许一个 Agent 修改。

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

## 专家选择矩阵

借鉴 `awesome-codex-subagents` 的思想，WikiForge 不追求一次启用大量专家，而是按任务选择少量合适角色。

| 任务类型 | 推荐专家 |
| --- | --- |
| 任务拆解和收口 | Orchestrator Agent |
| 上下文压缩和契约提取 | Context Manager Agent |
| API、DTO、状态、错误码 | Contract API Designer Agent |
| 后端 Core 实现 | Core Service Agent |
| Worker 文件任务实现 | Worker Service Agent |
| 前端页面和交互 | UI Agent |
| Docker、CI、环境变量 | DevOps Agent |
| 测试设计和回归验证 | Test QA Agent |
| 安全、路径和本地文件风险 | Security Review Agent |
| 最终集成评审 | Integration Review Agent |

## Handoff Packet

子 Agent 完成任务后必须输出：

```text
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

## 完成报告

完成时必须列出：

- 修改范围。
- 影响服务。
- 验证命令和结果。
- 未完成项。
- 下一步建议。
