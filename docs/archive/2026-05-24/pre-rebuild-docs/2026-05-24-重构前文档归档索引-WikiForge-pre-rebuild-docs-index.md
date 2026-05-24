# 2026-05-24 WikiForge 重构前文档归档索引 Pre-Rebuild Docs Index

## 归档结论

本目录保存 MVP0 重新开始前仍放在 `docs/current/` 的历史主文档，以及旧过程材料、旧 Work Order、旧项目内 AI Skill、旧并行 Agent 状态收件箱。

这些文档不再作为当前开发主线依据，仅用于追溯旧阶段的需求、架构、数据模型、计划和验证记录。当前主线以 `docs/rebuild/2026-05-24-mvp0-baseline/` 为准。

## 推荐阅读方式

新任务默认只读当前 MVP0 基座：

- `docs/rebuild/2026-05-24-mvp0-baseline/README.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构需求文档-WikiForge-rebuild-prd.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-四层架构设计-WikiForge-four-layer-architecture.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构路线-WikiForge-refactor-roadmap.md`
- `docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-现有资源盘点-WikiForge-reusable-assets-inventory.md`

只有在以下场景读取本目录：

- 需要追溯旧需求或历史决策。
- 需要从旧模块中筛选可复用代码。
- 需要解释旧数据库表、旧 API 或旧 UI 入口来源。

## 文件清单

| 文件 | 历史用途 | 当前处理 |
| --- | --- | --- |
| [需求文档 PRD](需求文档-knowledge-base-prd.md) | 旧知识库产品需求 | archive |
| [技术架构 Technical Architecture](技术架构-technical-architecture.md) | 旧技术架构与服务边界 | archive |
| [数据模型 Data Model](数据模型-data-model.md) | 旧数据表和领域模型 | archive |
| [架构决策 Architecture Decisions](架构决策-DECISIONS.md) | 旧 ADR 和架构判断 | archive |
| [MCP 接口契约 MCP API Contract](MCP接口契约-mcp-api-contract.md) | MCP 预览接口 | freeze |
| [OpenClaw / Hermes 接入说明](2026-05-23-OpenClaw-Hermes接入说明-WikiForge-openclaw-hermes-mcp-integration.md) | OpenClaw / Hermes MCP 集成 | archive |
| [MVP 审核报告](2026-05-23-MVP审核报告-WikiForge-mvp-audit-report.md) | 旧阶段审核 | archive |
| [MVP 实施计划](2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan.md) | 旧阶段计划 | archive |
| [MVP2 发布前自检](2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist.md) | 旧发布检查 | archive |
| [参考项目清单](2026-05-23-参考项目清单-WikiForge-reference-projects.md) | 旧参考项目 | archive |
| [项目整体计划](2026-05-24-项目整体计划-WikiForge-project-roadmap.md) | 旧路线 | superseded |
| [开发者日志](2026-05-24-开发者日志-WikiForge-developer-log.md) | 旧阶段日志 | archive |
| [版本更新记录](2026-05-24-版本更新记录-WikiForge-release-notes.md) | 旧版本记录 | archive |
| [legacy-docs](legacy-docs/README.md) | 旧过程材料、旧 Work Order、旧项目内 AI Skill、旧并行 Agent 状态收件箱 | archive |

## 冲突处理

如果本目录内容与当前 MVP0 基座冲突，以当前 MVP0 基座为准，不直接按旧文档继续开发。
