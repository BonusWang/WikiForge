# 前端开发Agent Status

| 字段 | 内容 |
| --- | --- |
| 任务ID | R4-5-1 / R4-5-2 |
| 状态 | Integrated |
| 建议分支 | `codex/r4-5-ui-mcp-dashboard` |
| 当前目标 | MCP tools / calls 前端接入与 Dashboard 只读展示 |
| 允许修改 | 见 `WORKSPACE.md` |
| 已完成 | 已新增 MCP API 封装、类型定义、Dashboard MCP Preview 只读工具清单和调用日志表 |
| 验证命令 | `npm --prefix frontend run build`、浏览器验证 `http://127.0.0.1:3002/` |
| 风险 | 工具调用会写入业务数据，本轮已明确不提供调用入口 |

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

## 集成结果

- 新增 `frontend/src/types/mcp.ts`。
- 新增 `frontend/src/api/mcp/index.ts`。
- 更新 `frontend/src/views/DashboardView.vue`，加入 MCP Preview 卡片。
- 更新 `frontend/src/styles/main.css`，加入 MCP 筛选和表格布局。
- 构建通过。
- 浏览器检查通过：MCP Preview、工具清单和调用日志可见，页面无 error。
