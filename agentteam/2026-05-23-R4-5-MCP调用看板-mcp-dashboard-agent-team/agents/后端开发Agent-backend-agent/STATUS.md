# 后端开发Agent Status

| 字段 | 内容 |
| --- | --- |
| 任务ID | R4-5-3 |
| 状态 | Ready for Implementation |
| 建议分支 | `codex/r4-5-backend-mcp-hardening` |
| 当前目标 | 补强 `GET /api/v1/mcp/calls` 查询端点测试，必要时补脱敏日志展示字段 |
| 允许修改 | 见 `WORKSPACE.md` |
| 已完成 | 只读侦察已完成；现有 API 足够支撑首版前端只读展示 |
| 验证命令 | `mvn -pl wikiforge-core-service test -Dtest=McpPreviewApiIntegrationTests` |
| 风险 | DDL、common 错误码和完整 MCP transport 不应在 R4-5 后端小改中扩展 |

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
