# 2026-05-23 WikiForge MVP4 AI 辅助整理审核实施计划 AI Review Implementation Plan

## 版本信息

- 文档版本：v0.3
- 当前分支：`codex/mvp4-ai-review`
- 父分支：`codex/mvp3-document-parsing`
- 当前阶段：S6 / R3 MVP4 AI 辅助整理与审核
- 当前测试门禁：T3 构建与 Docker 配置验证

## 当前执行指针

- ( ) R3-1 冻结模型调用契约
- ( ) R3-2 Agent 运行账本和审核队列表
- ( ) R3-3 生成摘要、标签、分类建议和风险标记
- ( ) R3-4 Web UI 审核队列
- ( ) R3-5 审核通过后更新 Obsidian Source Note
- (x) R3-6 文档归档、验证、提交推送

## 节点清单

| 完成 | 节点 | 状态 | 事项 | 测试门禁 |
| --- | --- | --- | --- | --- |
| [x] | R3-1 | Done | 冻结模型调用和 AI 整理任务的最小契约 | T0 |
| [x] | R3-2 | Done | 新增 `agent_runs`、`agent_steps`、`review_items` 最小表 | T1 |
| [x] | R3-3 | Done | 基于已解析正文生成结构化整理草案，支持 MiniMax / 本地规则兜底 | T1 / T2 |
| [x] | R3-4 | Done | Web UI 增加审核队列列表和详情 | T2 |
| [ ] | R3-5 | Next | 审核通过后更新 Obsidian Source Note | T2 / T3 |
| [ ] | R3-6 | In Progress | 更新路线图、开发者日志、归档索引并提交推送 | T0 |

## 契约冻结

### AI 整理任务 API

第一轮提供 Core API，并支持真实 MiniMax OpenAI-compatible 调用；未配置密钥或模型时自动使用本地规则兜底：

- `POST /api/v1/source-files/{fileUid}/ai-review-runs`
- `GET /api/v1/ai-review-runs/{runUid}`
- `GET /api/v1/review-items?status=pending&page=1&pageSize=20`

### 模型调用契约

MVP4 第一轮冻结字段，不保存密钥：

- `providerName`
- `modelName`
- `baseUrl`
- `providerType`
- `configSource`

MiniMax 密钥只允许通过环境变量或本机 `.env` 注入，不写入代码、文档和 Git：

- `WIKIFORGE_MINIMAX_API_KEY`
- `WIKIFORGE_MINIMAX_BASE_URL`
- `WIKIFORGE_MINIMAX_MODEL`

后续多模型统一使用可扩展命名：

- `WIKIFORGE_MODEL_<PROVIDER>_TYPE`
- `WIKIFORGE_MODEL_<PROVIDER>_API_KEY`
- `WIKIFORGE_MODEL_<PROVIDER>_BASE_URL`
- `WIKIFORGE_MODEL_<PROVIDER>_MODEL`

例如 DeepSeek 可使用 `WIKIFORGE_MODEL_DEEPSEEK_API_KEY`、`WIKIFORGE_MODEL_DEEPSEEK_BASE_URL`、`WIKIFORGE_MODEL_DEEPSEEK_MODEL`。业务代码只根据 provider 配置走 OpenAI-compatible 调用，不为每个大模型复制一套审核服务。

### 输出策略

第一轮输出策略：

- 从 `source_contents.raw_text` 生成短摘要。
- 生成默认标签建议。
- 生成分类建议。
- 写入 `agent_runs`、`agent_steps`、`review_items`。
- `review_items.status` 默认为 `pending`。
- 若 `providerName=minimax` 且环境变量完整，则调用 MiniMax `/chat/completions` 生成结构化草案。
- 若 MiniMax 未配置或调用失败，则保留本地规则兜底，并在建议 JSON 中记录 provider notice。

## 本轮完成结果

- 新增 Flyway：`V20260523_005__create_agent_review_tables.sql`。
- 新增 Core API：创建 AI 审核运行、查询运行、查询审核队列。
- 新增 MiniMax OpenAI-compatible Adapter，默认 Base URL 为 `https://api.minimax.io/v1`。
- 新增通用 AI Provider 配置解析，支持 MiniMax、DeepSeek、CC Switch 等后续 OpenAI-compatible Provider 按环境变量切换。
- 新增 Dashboard Provider / Model / Base URL 配置行、审核队列和 Source Files 行内 `AI 整理`入口。
- 密钥仅通过环境变量读取，未写入仓库。

## 验证命令

```text
mvn -B -s <temp-settings> -gs <temp-settings> test
npm run build
docker compose -f deploy/docker-compose.yml config --quiet
```
