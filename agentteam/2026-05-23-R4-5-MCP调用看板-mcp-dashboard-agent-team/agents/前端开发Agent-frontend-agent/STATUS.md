# 前端开发Agent Status

| 字段 | 内容 |
| --- | --- |
| 任务ID | R4-5-1 / R4-5-2 |
| 状态 | Ready for Implementation |
| 建议分支 | `codex/r4-5-ui-mcp-dashboard` |
| 当前目标 | MCP tools / calls 前端接入与 Dashboard 只读展示 |
| 允许修改 | 见 `WORKSPACE.md` |
| 已完成 | 只读侦察已完成；建议首版只展示 MCP tools 与 calls 日志，默认不开放工具调用 |
| 验证命令 | `npm run build` |
| 风险 | 工具调用会写入业务数据；calls 接口当前不含 input/output 详情，首版只展示审计列表 |

## 侦察结论

- 首选落点：`frontend/`，不要放到 `orchestration-ui/`。
- `orchestration-ui` 当前代理到 `localhost:8090`，主要服务编排辅助工程；MCP 看板直接放这里会引入 Core API 代理问题。
- R4-5 最小可落地范围：
  - 展示 MCP tools 列表。
  - 展示 MCP calls 日志表。
  - 默认不发起工具调用。
- 可展示字段：
  - `callUid`
  - `toolName`
  - `caller`
  - `status`
  - `error`
  - `duration`
  - `createdAt`

## 建议修改文件

- `frontend/src/api/mcp/index.ts`
- `frontend/src/types/mcp.ts`
- `frontend/src/views/DashboardView.vue`
- `frontend/src/styles/main.css`

## 建议验证

```powershell
npm --prefix frontend run build
```
