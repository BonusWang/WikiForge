# 知识熔炉 WikiForge

知识熔炉 WikiForge 是一个本地优先的个人知识与资料整理系统。它先把散落的本地文件和手工链接资料收集整理起来，再由 AI 编译为可维护的 Obsidian Topic / Project Wiki 页面；MySQL 负责索引、状态、运行账本和集成记录。

项目当前原则不是一上来做完整 RAG，也不是直接搭复杂多 Agent 平台，而是先解决一个现实问题：

> 资料太乱，需要先统一收集、整理、归档，再把稳定结果交给 AI、MCP 和向量库复用。

## 当前阶段

- 当前版本：`2.0-v2-preview.4`
- 当前阶段：R6-UI-2 / 路线与信息架构纠偏，进入最小 Wiki 编译闭环
- 已完成：本地源文件归集、链接资料收集、Obsidian Source Note、正文解析、AI 审核、MCP Preview、LifeOS 个人记录、JSONL chunk 导出、知识维护巡检、维护问题处理闭环、最小 Wiki 页面注册与 Wiki 更新审核/自动写入账本
- 未完成：真实向量库接入、Hybrid Search、办公室视图、定时总结和长期记忆

## 产品闭环

```text
本地文件 / 手工链接资料
  -> Raw Sources 和 MySQL 运行账本
  -> 正文解析和 Source Note 溯源层
  -> AI 编译为 Topic / Project Wiki 更新建议
  -> 普通资料自动追加 WikiForge Updates 托管区块
  -> 敏感、低置信度、冲突或缺目标页进入审核队列
  -> Obsidian Wiki 页面成为长期知识正文
  -> MCP / UI / JSONL chunks / Knowledge Health 作为高级运行能力复用
```

## 技术方向

- 后端：Java 21/17 + Spring Boot 3.x
- 前端：Vue 3 + Vite + TypeScript
- 数据库：MySQL 8.x
- 知识沉淀：Obsidian Markdown Vault
- 文件归集：本地 Raw Sources 目录
- 当前运行层：Wiki Compile、Review Queue、MCP HTTP Preview、JSONL Vector Export、Knowledge Maintenance
- 后续拓展：真实向量库、在线文档连接器、办公室视图、长期记忆

## 文档

- [文档入口 Docs Index](docs/README.md)
- [需求文档 PRD](docs/current/需求文档-knowledge-base-prd.md)
- [技术架构 Technical Architecture](docs/current/技术架构-technical-architecture.md)
- [数据模型 Data Model](docs/current/数据模型-data-model.md)
- [项目整体计划 Project Roadmap](docs/current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md)
- [R6-1 向量导出契约 Work Order](docs/superpowers/plans/2026-05-24-V2向量导出契约-WikiForge-r6-vector-export-contract.md)
- [R6-3 知识维护巡检 Work Order](docs/superpowers/plans/2026-05-24-V2知识维护巡检-WikiForge-r6-maintenance-lint-agent.md)
- [R6-3.1 维护问题处理闭环 Work Order](docs/superpowers/plans/2026-05-24-V2知识维护处理闭环-WikiForge-r6-maintenance-issue-workflow.md)
- [参考项目清单 Reference Projects](docs/current/2026-05-23-参考项目清单-WikiForge-reference-projects.md)
- [开发者日志 Developer Log](docs/current/2026-05-24-开发者日志-WikiForge-developer-log.md)
- [版本更新记录 Release Notes](docs/current/2026-05-24-版本更新记录-WikiForge-release-notes.md)

## 当前状态

R6-UI-2 已完成路线与信息架构纠偏：主线从 “Source Note 终点” 调整为 “Source Note 溯源层 -> Topic / Project Wiki 页面”。当前实现了最小 `wiki_pages` / `wiki_integrations` 账本、Wiki 编译 API、自动追加托管区块和审核通过/拒绝接口。R6-2 Hybrid Search、完整向量库、办公室视图、长期记忆和个人记录周期总结继续挂起。
