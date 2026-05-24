# 2026-05-23 WikiForge MVP 审核报告 MVP Audit Report

## 版本信息

- 文档版本：v0.1
- 审核范围：MVP5 / R4-6，覆盖部署配置、MCP HTTP Preview 暴露边界、文档状态、发布状态、Git 卫生和敏感信息风险
- 当前分支：`codex/mvp-audit-fixes`
- 目标版本：`0.08-preview.4`

## 审核结论

MVP 主链路已经具备继续演进的基础：本地文件归集、Obsidian Source Note、正文解析、AI 审核、MCP HTTP Preview、MCP 调用看板和 Orchestration 辅助工程均已形成闭环。

本轮审核发现的主要问题不在业务逻辑，而在“私有化部署默认值”和“发布后文档状态”：

- 当前 MVP 无登录鉴权，如果 Compose 默认暴露到所有网卡，会放大本地私有工具的误用风险。
- 部分文档和环境变量仍指向历史开发分支，容易让后续 Agent 误判当前状态。
- `0.08-preview.3` 已发布后，`main` 又追加了发布记录提交，既有标签不应移动，需要用新标签记录加固结果。

## 问题清单 Findings

| 严重级别 | 问题 | 影响 | 处理状态 |
| --- | --- | --- | --- |
| High | `deploy/docker-compose.yml` 与 `deploy/docker-compose.dev.yml` 默认将 MySQL、Core、Worker、Orchestration、UI 端口发布到所有网卡 | MVP 当前无登录鉴权，局域网或错误网络环境下可能访问本地知识库 API、MCP 写入入口或 MySQL | 已修复：统一改为 `${WIKIFORGE_PORT_BIND:-127.0.0.1}:host:container` |
| Medium | `.env.example` 与 Orchestration Compose 默认 `WIKIFORGE_ACTIVE_BRANCH` 仍指向 `codex/mvp5-mcp-preview` | Orchestration UI / 后续 Agent 会把旧阶段分支识别为当前分支 | 已修复：默认值改为 `main` |
| Medium | Roadmap、MCP 契约和接入说明仍以 R4-5 发布候选为最新状态 | 后续 AI 按最新快照启动时可能遗漏 R4-6 审核加固 | 已修复：文档版本、阶段说明和 R4-6 记录已更新 |
| Medium | `0.08-preview.3` 标签位于发布候选提交，`main` 后续追加了发布完成记录 | 标签和主干不完全一致，继续移动既有发布标签会破坏历史可追溯 | 已处理：不移动旧标签，使用 `0.08-preview.4` 收口审核加固 |
| Low | OpenClaw / Hermes 文档未说明默认本机绑定后，Docker 外部容器访问宿主机端口可能受限 | 外部容器接入可能误以为 `host.docker.internal` 一定可用 | 已修复：补充同一 Compose 网络优先、显式开放端口和安全前提 |

## 修复内容 Fixes

- `.env.example`
  - 新增 `WIKIFORGE_PORT_BIND=127.0.0.1`。
  - `WIKIFORGE_ACTIVE_BRANCH` 默认值改为 `main`。
- `deploy/docker-compose.yml`
  - MySQL、Core、Worker、Orchestration Service、UI、Orchestration UI 端口默认仅绑定 `127.0.0.1`。
  - Orchestration Service 默认分支改为 `main`。
- `deploy/docker-compose.dev.yml`
  - MySQL、Orchestration Service、Orchestration UI 端口默认仅绑定 `127.0.0.1`。
  - Orchestration Service 默认分支改为 `main`。
- `docs/current/2026-05-23-OpenClaw-Hermes接入说明-WikiForge-openclaw-hermes-mcp-integration.md`
  - 补充默认本机绑定、外部容器访问限制、同一 Compose 网络优先和对外开放安全条件。
- `docs/current/MCP接口契约-mcp-api-contract.md`
  - 补充 R4-6 审核加固记录。
- `docs/current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md`
  - 增加 R4-6 审核加固节点和最新归档索引指针。

## 保留边界 Residual Risk

- MVP 仍没有登录鉴权；默认本机绑定只能降低误暴露风险，不能替代正式权限系统。
- `WIKIFORGE_INTERNAL_API_TOKEN=change-me` 仍只适合本地开发；正式部署必须覆盖为真实 token。
- MCP HTTP Preview 仍不是完整 MCP stdio / SSE transport；当前优先服务 OpenClaw / Hermes 的 HTTP Tool / Connector 接入。
- `create_personal_record` 会保存业务原文到 `personal_records`；只适合本地私有化部署，不适合公网开放。

## 验证结果 Verification Result

- `git diff --check`：通过。
- `docker compose -f deploy/docker-compose.yml config --quiet`：通过。
- `docker compose -f deploy/docker-compose.dev.yml config --quiet`：通过。
- Compose 渲染检查：生产配置端口均包含 `host_ip: 127.0.0.1`，`WIKIFORGE_ACTIVE_BRANCH: main` 生效。
- 后端 Maven 全量测试：通过，5 个模块合计 52 个测试，0 失败。第一次使用默认 Java 8 运行失败，已切换到 Java 21 重跑通过。
- `npm --prefix frontend run build`：通过，保留既有 Rollup 大 chunk / PURE 注释 warning。
- `npm --prefix orchestration-ui run build`：通过。
- 敏感信息扫描：未发现 MiniMax 密钥前缀、Bearer token 或常见 API Key 模式写入仓库。
- 禁止路径扫描：未发现 `node_modules`、`dist`、`target`、`.env`、`data`、Vault、Raw Sources 被 Git 跟踪。

## 当前状态

- [x] 问题清单已整理。
- [x] 高优先级部署暴露问题已修复。
- [x] 文档和环境变量状态漂移已修复。
- [x] 验证命令执行完成。
- [ ] 提交、合入 main、推送和 `0.08-preview.4` 发布完成。
