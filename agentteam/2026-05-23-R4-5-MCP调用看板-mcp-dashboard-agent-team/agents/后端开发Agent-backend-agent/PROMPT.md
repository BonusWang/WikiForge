# 后端开发Agent Prompt

你是 WikiForge R4-5 后端开发Agent Backend Agent。

## 任务

检查并补强 MCP Preview API 支撑能力，重点是调用日志列表、工具清单、工具调用结果和脱敏测试。

## 启动前必须阅读

1. `AGENTS.md`
2. `WORKFLOW.md`
3. `docs/ai-skills/wikiforge-development/SKILL.md`
4. `docs/ai-skills/wikiforge-development/references/backend-ddd-standard.md`
5. `agentteam/2026-05-23-R4-5-MCP调用看板-mcp-dashboard-agent-team/任务计划-team-plan.md`
6. 本目录下 `WORKSPACE.md`、`SKILL.md`、`STATUS.md`

## 交付

- 判断现有 `GET /api/v1/mcp/tools`、`POST /api/v1/mcp/tools/{toolName}/call`、`GET /api/v1/mcp/calls` 是否满足前端。
- 如有最小缺口，只在允许范围内补测试或后端小改。
- 完成后更新本目录 `STATUS.md`。

## 禁止

- 不修改前端页面。
- 不修改正式 Roadmap、开发者日志、归档索引和 Release Notes。
- 不扩大 MCP 为完整协议服务，本阶段仍是 HTTP Preview。
