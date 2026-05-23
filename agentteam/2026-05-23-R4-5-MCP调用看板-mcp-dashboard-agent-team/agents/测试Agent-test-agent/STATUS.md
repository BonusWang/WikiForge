# 测试Agent Status

| 字段 | 内容 |
| --- | --- |
| 任务ID | R4-5-4 / R4-5-5 |
| 状态 | Integrated |
| 建议分支 | `codex/r4-5-test-review` |
| 当前目标 | 输出并执行 R4-5 验证矩阵 |
| 允许修改 | 见 `WORKSPACE.md` |
| 已完成 | 只读侦察已完成；R4-5-1/2/3 已按矩阵执行后端定向测试、前端构建和浏览器验证 |
| 验证命令 | 见 `WORKSPACE.md` |
| 风险 | 需补 `GET /api/v1/mcp/calls` 查询端点测试；Docker 实启依赖本机环境 |

## R4-5 验证矩阵

### 文档-only

```powershell
git diff --check
```

预期：无空白错误，Agent Team 状态文件字段完整。

### 后端小改

```powershell
cd E:\github\WikiForge\backend
mvn -B -pl wikiforge-core-service -am -Dtest=McpPreviewApiIntegrationTests -Dsurefire.failIfNoSpecifiedTests=false test
```

预期：MCP 集成测试通过；如改 calls 列表，需覆盖分页、筛选、字段 shape 和 `pageSize` 上限。

### 前端联动

```powershell
cd E:\github\WikiForge\frontend
npm run build
```

预期：TypeScript 与 Vite build 通过。

### Docker 烟测

```powershell
docker compose -f deploy/docker-compose.yml config --quiet
docker compose -f deploy/docker-compose.dev.yml config --quiet
```

发布候选再执行实启健康检查。

## 验收重点

- `GET /api/v1/mcp/tools` 返回 5 个冻结工具。
- `GET /api/v1/mcp/calls?page=1&pageSize=20` 返回分页结构。
- 成功和失败调用都写入 `mcp_tool_calls`。
- 列表和详情不得暴露 `rawContent`、`markdown`、`structured` 原文。
- UI 展示工具清单、最近调用、状态、错误码、耗时和调用方。
- OpenClaw / Hermes 文档不写真实 token 或密钥。

## 本轮验证结果

- 后端：`McpPreviewApiIntegrationTests` 8 个测试通过。
- 前端：`npm --prefix frontend run build` 通过。
- 浏览器：`http://127.0.0.1:3002/` 可见 MCP Preview、工具清单、调用日志列；无调用工具按钮；控制台 error 数为 0。
- Docker：本轮未改 Compose，R4-5-5 发布收口时再执行 config 和必要健康检查。
