# 后端开发Agent Status

| 字段 | 内容 |
| --- | --- |
| 任务ID | R4-5-3 |
| 状态 | Integrated |
| 建议分支 | `codex/r4-5-backend-mcp-hardening` |
| 当前目标 | 补强 `GET /api/v1/mcp/calls` 查询端点测试，必要时补脱敏日志展示字段 |
| 允许修改 | 见 `WORKSPACE.md` |
| 已完成 | 已补 `GET /api/v1/mcp/calls` 查询端点集成测试，并将 calls pageSize 上限从 Source 搜索上限解耦为 100 |
| 验证命令 | `McpPreviewApiIntegrationTests` 8 个测试通过 |
| 风险 | 本轮未新增 DDL、common 错误码或完整 MCP transport |

## 侦察结论

现有接口：

- `GET /api/v1/mcp/tools`
- `POST /api/v1/mcp/tools/{toolName}/call`
- `GET /api/v1/mcp/calls`

已启用工具：

- `search_sources`
- `get_source`
- `create_source`
- `get_obsidian_note`
- `create_personal_record`

最小缺口：

- `GET /api/v1/mcp/calls` 缺少专门集成测试。
- calls 列表目前只适合审计元信息展示，不适合详情回放。
- 如新增 input / output 展示，必须返回脱敏摘要，不返回原文。

## 建议修改文件

- `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/McpPreviewApiIntegrationTests.java`
- 如必要，再修改 MCP 相关 DTO / Service / Controller。

## 建议验证

```powershell
cd E:\github\WikiForge\backend
mvn -s %TEMP%\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\repository" -pl wikiforge-core-service -am -Dtest=McpPreviewApiIntegrationTests test
```

## 集成结果

- 新增 calls 查询测试，断言分页、筛选、倒序、字段 shape，并确认不返回 input/output 原文。
- 修复 MCP calls 查询 pageSize 上限：R4-5 审计看板允许 `pageSize=100`。
- 定向 MCP 集成测试通过：8 tests, 0 failures.
