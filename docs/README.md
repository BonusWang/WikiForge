# WikiForge 文档目录 Docs Index

## 入口规则

当前项目按 MVP0 重新开始。新任务默认只读当前基座，历史文档仅在追溯旧实现时读取。

建议阅读顺序：

1. `../AGENTS.md`
2. `../WORKFLOW.md`
3. [MVP0 基座](rebuild/2026-05-24-mvp0-baseline/README.md)
4. [现状基线](rebuild/2026-05-24-mvp0-baseline/2026-05-24-现状基线-WikiForge-pre-rebuild-baseline.md)
5. 当前任务需要的需求、架构、路线或资源盘点文档

## 当前 MVP0 基座

- [MVP0 基座 README](rebuild/2026-05-24-mvp0-baseline/README.md)
- [现状基线 Pre-Rebuild Baseline](rebuild/2026-05-24-mvp0-baseline/2026-05-24-现状基线-WikiForge-pre-rebuild-baseline.md)
- [MVP0 需求文档 PRD](rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构需求文档-WikiForge-rebuild-prd.md)
- [四层架构设计 Four-Layer Architecture](rebuild/2026-05-24-mvp0-baseline/2026-05-24-四层架构设计-WikiForge-four-layer-architecture.md)
- [MVP0 前端设计方案 Frontend Design](rebuild/2026-05-24-mvp0-baseline/2026-05-24-前端设计方案-WikiForge-mvp0-frontend-design.md)
- [MVP0 后端设计方案 Backend Design](rebuild/2026-05-24-mvp0-baseline/2026-05-24-后端设计方案-WikiForge-mvp0-backend-design.md)
- [MVP0 数据库设计方案 Data Design](rebuild/2026-05-24-mvp0-baseline/2026-05-24-数据库设计方案-WikiForge-mvp0-data-design.md)
- [MVP0 API 契约设计 API Contract](rebuild/2026-05-24-mvp0-baseline/2026-05-24-API契约设计-WikiForge-mvp0-api-contract.md)
- [MVP0 Obsidian LLM Wiki 设计](rebuild/2026-05-24-mvp0-baseline/2026-05-24-Obsidian-LLM-Wiki设计-WikiForge-mvp0-obsidian-llm-wiki-design.md)
- [四层架构图 Drawio](rebuild/2026-05-24-mvp0-baseline/diagrams/2026-05-24-WikiForge-four-layer-architecture.drawio)
- [现有资源盘点 Reusable Assets Inventory](rebuild/2026-05-24-mvp0-baseline/2026-05-24-现有资源盘点-WikiForge-reusable-assets-inventory.md)
- [MVP0 路线图 Roadmap](rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构路线-WikiForge-refactor-roadmap.md)

## 全项目强约定

- [项目架构强约定 Project Architecture Conventions](current/项目架构强约定-WikiForge-project-architecture-conventions.md)

## 文档隔离

```text
docs/
  current/   # 公共规则，不放产品主线
  rebuild/   # MVP0 基座和后续设计
  archive/   # 历史材料
```

## 历史归档

以下内容已经退出当前主线：

- 旧 PRD、旧技术架构、旧数据模型和旧架构决策。
- MCP / OpenClaw / Hermes 接入说明。
- 旧 MVP 实施计划、审核报告、发布检查。
- 旧项目计划、开发者日志、版本记录和参考项目清单。
- 旧过程材料、旧 Work Order、旧项目内 AI Skill、旧并行 Agent 状态收件箱。

入口：

- [2026-05-24 归档索引 Archive Index](archive/2026-05-24/2026-05-24-归档索引-archive-index-v0.7.md)
- [重构前文档归档索引 Pre-Rebuild Docs Index](archive/2026-05-24/pre-rebuild-docs/2026-05-24-重构前文档归档索引-WikiForge-pre-rebuild-docs-index.md)

## 本机实施信息

- Obsidian Vault 宿主机路径：`E:\WikiForgeVault`
- Docker 容器内 Vault 路径：`/data/wikiforge/obsidian-vault`
- 不提交 Vault、Raw Sources、运行数据或本地 `.env`。
