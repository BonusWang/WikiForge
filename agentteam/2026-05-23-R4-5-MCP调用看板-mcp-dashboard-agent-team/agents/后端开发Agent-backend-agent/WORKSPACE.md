# 后端开发Agent Workspace

## 允许修改

- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/interfaces/**` 中 MCP 相关文件
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/**` 中 MCP 相关文件
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/**` 中 MCP 相关文件
- `backend/wikiforge-core-service/src/test/**` 中 MCP 相关测试
- 本目录 `STATUS.md`

## 禁止修改

- `frontend/**`
- `backend/wikiforge-worker-service/**`
- Flyway migration，除非主 Agent 明确授权
- 共享 DTO、错误码，除非主 Agent 明确授权
- `docs/current/**`
- `docs/archive/**`

## 建议分支

```text
codex/r4-5-backend-mcp-hardening
```

## 验证命令

```powershell
cd E:\github\WikiForge\backend
mvn -pl wikiforge-core-service test -Dtest=McpPreviewApiIntegrationTests
```
