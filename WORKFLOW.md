# WikiForge 工作流 Workflow

当前工作流服务于 **MVP0 重新开始**。目标是减少过程负担，用单线、清晰、可验证的方式完成重构。

## 1. 当前工作方式

- 单主线推进，不做多 Agent 并行。
- 先需求与设计对齐，再进入代码改造。
- 每次只处理一个明确节点。
- 文档基座、代码改动和验证结果必须一致。
- Orchestration 辅助开发工程不再作为当前项目能力规划。
- 数据库按 MVP0 最小表集合推进，不预建未来能力表。
- 新增功能、服务、原子能力、API、数据库表必须同步维护项目架构强约定。
- API 请求、响应、错误和状态字段必须同步维护 MVP0 API 契约设计。
- 用户可见状态使用中文码值和中文说明，统一由字典表维护。

## 2. 启动顺序

新任务开始时按顺序读取：

1. `AGENTS.md`
2. `WORKFLOW.md`
3. `docs/README.md`
4. `docs/rebuild/2026-05-24-mvp0-baseline/README.md`
5. 当前任务需要的设计文档、计划文档或代码文件

只有需要追溯历史时，才读取 `docs/archive/`。

## 3. 节点状态

| 状态 | 含义 |
| --- | --- |
| 待办 | 已记录，暂不处理 |
| 就绪 | 需求和边界清楚 |
| 执行中 | 正在执行 |
| 复核 | 等待用户或自检确认 |
| 阻塞 | 存在真实阻塞 |
| 完成 | 已验证并更新相关文档 |

## 4. 任务卡

需要记录任务时，使用轻量任务卡：

```text
任务：
目标：
范围：
允许修改：
不修改：
验收：
风险：
下一步：
```

## 5. 完成定义

任务完成必须满足：

- 改动符合当前 MVP0 基座。
- 对应验证命令已执行，或明确说明未执行原因。
- 入口文档和当前节点文档已同步。
- 未引入历史阶段、并行 Agent 或 Orchestration 主流程设定。
- `git diff --check` 通过。

## 6. 当前结论

- 当前主线：统一入口、Raw Sources 收纳、SourceFile 账本、正文抽取、Obsidian LLM Wiki 写入。
- 当前数据库原则：保留最小收纳、Wiki 写入账本和中文状态字典，历史表结构后续单独清理。
- 当前 Obsidian 原则：只写 Vault 内 `WikiForge/` 托管目录，不覆盖托管区块外内容。
- 当前文档基座：`docs/rebuild/2026-05-24-mvp0-baseline/`。
- 当前公共规则：`AGENTS.md`、`WORKFLOW.md`、`docs/README.md`、`docs/current/分支管理策略-branch-strategy.md`。
- 当前全项目架构约定：`docs/current/项目架构强约定-WikiForge-project-architecture-conventions.md`。
- 当前历史参考：`docs/archive/2026-05-24/pre-rebuild-docs/`。
