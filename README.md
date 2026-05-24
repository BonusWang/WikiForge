# 知识熔炉 WikiForge

WikiForge 是一个本地优先的个人私有知识库工具。

MVP0 从减法开始：先把散落文件统一收纳到 Raw Sources，再把可整理内容按 LLM Wiki 规则写入 Obsidian。向量检索、MCP、LifeOS、知识体检和辅助开发工程都不进入 MVP0 主线。

## 当前阶段

- 当前阶段：MVP0 重新开始
- 当前基座：`docs/rebuild/2026-05-24-mvp0-baseline/`
- 核心目标：统一入口、原始文件规整收纳、Obsidian LLM Wiki 自动归档
- 数据库原则：只保留 MVP0 最小表集合，后续按需求新增
- 不做事项：多 Agent 并行、Orchestration 辅助工程、真实向量库、文档问答、在线文档 OAuth 抓取

## MVP0 闭环

```text
本地路径扫描 / 浏览器上传
  -> Raw Sources 复制收纳
  -> hash 去重和类型识别
  -> SourceFile 账本
  -> 正文抽取
  -> LLM Wiki 规则整理
  -> Obsidian Wiki 页面写入
  -> index.md / log.md 更新
```

## 技术方向

- 后端：Java + Spring Boot
- 前端：Vue 3 + Vite + TypeScript
- 数据库：MySQL
- 知识库：Obsidian Markdown Vault
- 文件源：本地 Raw Sources

## 文档入口

- [文档目录 Docs Index](docs/README.md)
- [MVP0 基座](docs/rebuild/2026-05-24-mvp0-baseline/README.md)
- [现状基线 Pre-Rebuild Baseline](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-现状基线-WikiForge-pre-rebuild-baseline.md)
- [MVP0 需求文档 PRD](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构需求文档-WikiForge-rebuild-prd.md)
- [四层架构设计 Four-Layer Architecture](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-四层架构设计-WikiForge-four-layer-architecture.md)
- [MVP0 前端设计方案 Frontend Design](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-前端设计方案-WikiForge-mvp0-frontend-design.md)
- [MVP0 后端设计方案 Backend Design](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-后端设计方案-WikiForge-mvp0-backend-design.md)
- [MVP0 数据库设计方案 Data Design](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-数据库设计方案-WikiForge-mvp0-data-design.md)
- [MVP0 API 契约设计 API Contract](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-API契约设计-WikiForge-mvp0-api-contract.md)
- [MVP0 Obsidian LLM Wiki 设计](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-Obsidian-LLM-Wiki设计-WikiForge-mvp0-obsidian-llm-wiki-design.md)
- [项目架构强约定 Project Architecture Conventions](docs/current/项目架构强约定-WikiForge-project-architecture-conventions.md)
- [MVP0 路线图 Roadmap](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构路线-WikiForge-refactor-roadmap.md)
- [MVP0 新起点交付记录 Starting Point](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-25-MVP0新起点交付记录-WikiForge-mvp0-starting-point.md)
- [现有资源盘点 Reusable Assets Inventory](docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-现有资源盘点-WikiForge-reusable-assets-inventory.md)
- [重构前文档归档索引 Pre-Rebuild Docs Index](docs/archive/2026-05-24/pre-rebuild-docs/2026-05-24-重构前文档归档索引-WikiForge-pre-rebuild-docs-index.md)

## 当前状态

MVP0 基座已进入代码闭环：路径扫描、浏览器上传、Raw Sources 收纳、正文抽取、Obsidian LLM Wiki 写入和 index/log 更新已经接入。后续所有开发都在 MVP0 基座上递增。

本地验收入口：`http://127.0.0.1:5174/capture`。Obsidian Vault 按本机验收口径指向 `E:\WikiForgeVault`。
