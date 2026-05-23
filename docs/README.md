# WikiForge 文档目录 Docs Index

## 入口规则

新参与 WikiForge 的 AI Agent 或开发者，建议按以下顺序阅读：

1. `../AGENTS.md`：项目协作、归档和 Git 规则。
2. `../WORKFLOW.md`：任务控制平面、Issue 风格任务卡和完成定义。
3. `archive/` 最新日期目录下的最高版本归档索引。
4. `current/` 当前主文档。
5. `ai-skills/wikiforge-development/` 项目专用开发 Skill。
6. `superpowers/plans/` 当前开发切片或并行工作单。

当前 R6 / V2 执行计划：

- [V2 向量导出契约 Vector Export Work Order](superpowers/plans/2026-05-24-V2向量导出契约-WikiForge-r6-vector-export-contract.md)
- [V1 在线资料与个人记录 LifeOS Work Order](superpowers/plans/2026-05-24-V1在线资料与个人记录-WikiForge-v1-lifeos-work-order.md)

## 目录结构

```text
docs/
  README.md                         # 本文档入口
  current/                          # 当前主线文档，开发和需求以这里为准
  process/                          # 过程性材料、评审、阶段设计
  superpowers/plans/                # 可执行开发计划和 Parallel Work Order
  ai-skills/wikiforge-development/  # WikiForge 项目内 AI 开发 Skill
  archive/YYYY-MM-DD/               # 按日期归档的需求、日志、方案快照
```

## 当前主线文档

- [需求文档 PRD](current/需求文档-knowledge-base-prd.md)
- [技术架构 Technical Architecture](current/技术架构-technical-architecture.md)
- [数据模型 Data Model](current/数据模型-data-model.md)
- [架构决策 Architecture Decisions](current/架构决策-DECISIONS.md)
- [MCP 接口契约 MCP API Contract](current/MCP接口契约-mcp-api-contract.md)
- [OpenClaw / Hermes 接入说明 OpenClaw Hermes MCP Integration](current/2026-05-23-OpenClaw-Hermes接入说明-WikiForge-openclaw-hermes-mcp-integration.md)
- [MVP 审核报告 MVP Audit Report](current/2026-05-23-MVP审核报告-WikiForge-mvp-audit-report.md)
- [MVP 实施计划 Implementation Plan](current/2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan.md)
- [项目整体计划 Project Roadmap](current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md)
- [MVP2 发布前自检 MVP2 Release Checklist](current/2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist.md)
- [参考项目清单 Reference Projects](current/2026-05-23-参考项目清单-WikiForge-reference-projects.md)
- [开发者日志 Developer Log](current/2026-05-24-开发者日志-WikiForge-developer-log.md)
- [版本更新记录 Release Notes](current/2026-05-24-版本更新记录-WikiForge-release-notes.md)

## 过程材料

- [需求完整度自检 Requirements Review](process/2026-05-22-需求完整度自检-WikiForge-requirements-completeness-review.md)
- [微服务架构与 AI 开发 Skill 设计](process/2026-05-23-微服务架构与AI开发Skill设计-WikiForge-microservice-ai-skill-design.md)
- [架构评审材料 Architecture Review](process/2026-05-23-架构评审材料-WikiForge-architecture-review.md)
- [架构评审结论 Architecture Review Conclusion](process/2026-05-23-架构评审结论-WikiForge-architecture-review-conclusion.md)
- [文档一致性自检 Docs Consistency Check](process/2026-05-23-文档一致性自检-WikiForge-docs-consistency-check.md)
- [Symphony 工作模式评估 Symphony Workflow Review](process/2026-05-23-Symphony工作模式评估-WikiForge-symphony-workflow-review.md)

## 当前本机实施信息

- 用户确认的 Obsidian Vault 宿主机路径：`E:\WikiForgeVault`
- R6-1 向量导出宿主机默认路径：`data/vector-exports`，可通过 `WIKIFORGE_HOST_VECTOR_EXPORT_ROOT` 和 `WIKIFORGE_VECTOR_EXPORT_ROOT` 配置。
- Docker 容器内 Vault 路径仍使用：`/data/wikiforge/obsidian-vault`
- Docker 容器内向量导出路径默认使用：`/data/wikiforge/vector-exports`
- 不能把 Vault 内容、Raw Sources、运行数据或本地 `.env` 提交到 Git。
