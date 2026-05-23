# 2026-05-23 WikiForge OpenClaw / Hermes 接入说明 OpenClaw Hermes MCP Integration

## 版本信息

- 文档版本：v0.2
- 当前阶段：MVP5 / R4-6 审核加固后的 OpenClaw 与 Hermes 本机接入说明
- 适用接口：WikiForge MCP HTTP Preview API
- 默认本机地址：`http://localhost:8080/api/v1/mcp`

## 1. 当前接入定位

WikiForge 当前提供的是 **MCP HTTP Preview**，不是完整 stdio / SSE MCP transport。

因此 OpenClaw、Hermes 或其他机器人第一版建议按 HTTP 工具方式接入：

```text
OpenClaw / Hermes / Local Agent
  -> HTTP Tool / Custom Action / Bridge Script
  -> WikiForge Core Service /api/v1/mcp
  -> MySQL 调用日志 + Source / Personal Record / Obsidian Note
```

后续如果引入正式 MCP Server，可以复用当前 tool schema、权限边界和调用日志。

## 2. 地址选择

| 场景 | Base URL |
| --- | --- |
| 本机浏览器、PowerShell、Codex 调试 | `http://localhost:8080/api/v1/mcp` |
| Docker 外部容器访问宿主机 Core Service | `http://host.docker.internal:8080/api/v1/mcp` |
| 同一 Docker Compose 网络内访问 | `http://wikiforge-core-service:8080/api/v1/mcp` |

MVP5 当前没有登录鉴权。Compose 默认通过 `WIKIFORGE_PORT_BIND=127.0.0.1` 只绑定宿主机本机端口，请优先在本机或同一 Compose 网络内使用，不要直接暴露到公网。

如果 OpenClaw / Hermes 运行在另一个 Docker 容器中，`host.docker.internal` 只有在宿主机端口对该容器可达时才适用。更稳妥的方式是把调用方放进同一 Compose 网络并使用 `http://wikiforge-core-service:8080/api/v1/mcp`。确实需要让外部容器或局域网访问宿主机端口时，再显式设置 `WIKIFORGE_PORT_BIND=0.0.0.0`，并同时配置防火墙、反向代理鉴权或 IP allowlist。

## 3. 调用方审计 Header

WikiForge 使用 Header 记录调用来源，便于 Web UI 的 MCP Preview 看板展示。

```text
X-WikiForge-Caller-Type: agent
X-WikiForge-Caller-Id: openclaw-local
```

推荐值：

| 调用方 | `X-WikiForge-Caller-Type` | `X-WikiForge-Caller-Id` |
| --- | --- | --- |
| OpenClaw | `agent` | `openclaw-local` |
| Hermes | `agent` | `hermes-local` |
| 本机脚本 | `external_agent` | `local-script` |
| Codex 调试 | `agent` | `codex-dev` |

Header 只用于审计，不是权限凭证。

## 4. 查询工具清单

```powershell
curl.exe -s "http://localhost:8080/api/v1/mcp/tools" |
  ConvertFrom-Json |
  Select-Object -ExpandProperty data |
  Select-Object -ExpandProperty tools
```

当前工具：

| Tool | 用途 |
| --- | --- |
| `search_sources` | 搜索已进入 WikiForge 的 Source / SourceFile 安全摘要 |
| `get_source` | 读取单个 Source / SourceFile 的安全摘要和正文摘录 |
| `create_source` | 写入文本型 Source 草案 |
| `get_obsidian_note` | 读取已登记 Obsidian Note |
| `create_personal_record` | 写入消费、账单、邮件、人际关系、事件或普通记录草案 |

## 5. 写入个人记录

适合 OpenClaw / Hermes 把用户日常记录、消费、账单、邮件、人际关系和事件写入 WikiForge。

```powershell
$body = @{
  arguments = @{
    recordType = "note"
    title = "OpenClaw 本机记录测试"
    rawContent = "这是一条由 OpenClaw 写入 WikiForge 的测试记录。"
    sourceChannel = "openclaw"
    sourceRef = "local-chat"
    sensitivityLevel = "medium"
  }
} | ConvertTo-Json -Depth 8

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/mcp/tools/create_personal_record/call" `
  -ContentType "application/json" `
  -Headers @{
    "X-WikiForge-Caller-Type" = "agent"
    "X-WikiForge-Caller-Id" = "openclaw-local"
  } `
  -Body $body
```

支持 `recordType`：

- `expense`
- `bill`
- `email`
- `relationship`
- `event`
- `note`

当前行为：

- 写入 `personal_records`。
- 初始状态为 `pending`。
- 不自动调用 AI。
- 不自动写入 Obsidian。
- MCP 调用日志不保存 `rawContent` 原文，只保存脱敏摘要。

## 6. 写入文本 Source

适合机器人把外部材料、网页摘录、聊天整理内容先作为 Source 草案放入 WikiForge。

```powershell
$body = @{
  arguments = @{
    title = "Hermes 收集的资料片段"
    rawContent = "这里是 Hermes 读取或整理后的资料正文。"
    sourceType = "text"
    sourcePlatform = "hermes"
    processingIntent = "organize_only"
  }
} | ConvertTo-Json -Depth 8

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/mcp/tools/create_source/call" `
  -ContentType "application/json" `
  -Headers @{
    "X-WikiForge-Caller-Type" = "agent"
    "X-WikiForge-Caller-Id" = "hermes-local"
  } `
  -Body $body
```

当前边界：

- 不接受本地路径。
- 不抓取 `sourceUrl`。
- 不自动写入 Obsidian。
- 不自动调用 AI 审核。

## 7. 查询 Source

```powershell
$body = @{
  arguments = @{
    keyword = "Hermes"
    page = 1
    pageSize = 10
  }
} | ConvertTo-Json -Depth 8

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/mcp/tools/search_sources/call" `
  -ContentType "application/json" `
  -Headers @{
    "X-WikiForge-Caller-Type" = "agent"
    "X-WikiForge-Caller-Id" = "hermes-local"
  } `
  -Body $body
```

读取单个 Source：

```powershell
$body = @{
  arguments = @{
    sourceUid = "src_xxx"
    includeContentExcerpt = $true
  }
} | ConvertTo-Json -Depth 8

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/mcp/tools/get_source/call" `
  -ContentType "application/json" `
  -Headers @{
    "X-WikiForge-Caller-Type" = "agent"
    "X-WikiForge-Caller-Id" = "hermes-local"
  } `
  -Body $body
```

`get_source` 不返回本地绝对路径，只返回安全摘要、正文摘录和 Obsidian Note 摘要。

## 8. 读取 Obsidian Note

只能读取已登记的 `noteUid`，不能传入本地文件路径。

```powershell
$body = @{
  arguments = @{
    noteUid = "note_xxx"
    includeMarkdown = $true
  }
} | ConvertTo-Json -Depth 8

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/mcp/tools/get_obsidian_note/call" `
  -ContentType "application/json" `
  -Headers @{
    "X-WikiForge-Caller-Type" = "agent"
    "X-WikiForge-Caller-Id" = "openclaw-local"
  } `
  -Body $body
```

安全边界：

- 不返回 `absolutePath`。
- `vaultPath` 必须是 Vault 相对路径。
- Markdown 读取前会校验最终真实路径仍在配置的 Obsidian Vault 根目录内。

## 9. 查询 MCP 调用日志

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/mcp/calls?callerType=agent&page=1&pageSize=20"
```

可选过滤：

- `toolName`
- `status=completed|failed`
- `callerType`
- `page`
- `pageSize`，最大 100

调用日志会展示在 Web UI 的 `MCP Preview` 看板中。

## 10. OpenClaw / Hermes 配置建议

### OpenClaw

建议先配置为 HTTP Tool 或自定义 Action：

```text
Name: WikiForge MCP Preview
Base URL: http://localhost:8080/api/v1/mcp
Headers:
  X-WikiForge-Caller-Type: agent
  X-WikiForge-Caller-Id: openclaw-local
```

建议优先开放给 OpenClaw 的动作：

- `create_personal_record`
- `create_source`
- `search_sources`
- `get_source`

### Hermes

建议作为本机 HTTP Connector：

```text
Connector: WikiForge HTTP MCP Preview
Base URL: http://localhost:8080/api/v1/mcp
Headers:
  X-WikiForge-Caller-Type: agent
  X-WikiForge-Caller-Id: hermes-local
```

建议 Hermes 第一阶段用于：

- 从聊天、网页、在线文档读取后调用 `create_source`。
- 从个人输入中调用 `create_personal_record`。
- 查询 `search_sources` 或 `get_source` 辅助回答。

## 11. 安全注意事项

- 不要把真实 token、密码、账单明细或密钥写进项目文档。
- Docker Compose 默认仅绑定 `127.0.0.1`；对外开放前必须明确修改 `WIKIFORGE_PORT_BIND`。
- 生产或公网环境必须先补鉴权、IP allowlist 或反向代理访问控制。
- 当前 `create_personal_record` 会保存 `rawContent` 到 `personal_records`，适合本地私有部署，不适合公网开放。
- MCP 调用日志已脱敏，但业务表会保存用户提交的原始内容。
- OpenClaw / Hermes 自动写入前，建议先默认使用 `sensitivityLevel=medium`。

## 12. R4-5 验收清单

- [ ] OpenClaw / Hermes 能按本文档查询 `GET /api/v1/mcp/tools`。
- [ ] OpenClaw / Hermes 能写入一条 `create_personal_record` 测试记录。
- [ ] OpenClaw / Hermes 能写入一条 `create_source` 测试 Source。
- [ ] Web UI 的 `MCP Preview` 看板能看到对应调用日志。
- [ ] 调用日志不出现 `rawContent`、`markdown`、`structured` 原文。
- [ ] 不通过 MCP 返回本地绝对路径。
