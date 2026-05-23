# R4-5 MCP 调用看板 Agent Team

本 Agent Team 用于执行 WikiForge R4-5：展示 MCP 调用日志，并补充 OpenClaw / Hermes 本机接入说明。

## Team 组成

| Agent | 角色 | 主要职责 |
| --- | --- | --- |
| 主Agent Master Agent | 统筹协调 | 拆解任务、跟踪状态、检查交付、集成验证、更新正式文档 |
| 前端开发Agent Frontend Agent | UI 实现 | MCP 工具清单、工具调用、调用日志看板 |
| 后端开发Agent Backend Agent | Core API 加固 | MCP Preview API 缺口检查、后端测试补强、日志字段确认 |
| 测试Agent Test Agent | 验证与风险 | 设计 R4-5 验证矩阵、执行回归、自检风险 |

## 状态流转

```text
Assigned -> In Progress -> Ready for Integration -> Integrated
                      \-> Blocked
```

主 Agent 读取每个 Agent 的 `STATUS.md` 后，统一决定是否合并、补测、更新正式文档和发布。

## 本轮基线

- 当前分支：`codex/r4-5-mcp-dashboard-connectors`
- 上一节点：R4-4 已完成 `get_obsidian_note` / `create_personal_record`
- 当前节点：R4-5 MCP 调用展示与 OpenClaw / Hermes 接入说明
- 当前测试门禁：前端联动至少 T2，阶段发布前再走 T4

## 参考

- `E:\github\claude-agent-examples\build-agent-example\doc\step07_plan_todolist.md`
- `E:\github\claude-agent-examples\build-agent-example\doc\step08_subagent.md`
- `E:\github\claude-agent-examples\build-agent-example\doc\step09_agent_team.md`
