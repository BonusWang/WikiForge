# 知识熔炉 WikiForge

知识熔炉 WikiForge 是一个本地优先的个人知识与资料整理系统。它先把散落的文件、在线资料和个人记录收集整理起来，再逐步沉淀到 Obsidian、MySQL 运行账本和后续向量库。

项目当前原则不是一上来做完整 RAG，也不是直接搭复杂多 Agent 平台，而是先解决一个现实问题：

> 资料太乱，需要先统一收集、整理、归档，再把稳定结果交给 AI、MCP 和向量库复用。

## 当前阶段

- 当前版本：`2.0-v2-preview.3`
- 当前阶段：R6-3.1 / V2 知识维护处理闭环
- 已完成：本地源文件归集、Obsidian Source Note、正文解析、AI 审核、MCP Preview、LifeOS 个人记录、JSONL chunk 导出、知识维护巡检、维护问题处理闭环
- 未完成：真实向量库接入、Hybrid Search、办公室视图、定时总结和长期记忆

## 产品闭环

```text
本地文件 / 在线链接 / 个人记录
  -> Raw Sources 和 MySQL 运行账本
  -> 正文解析和 Source Note
  -> AI 辅助整理与人工审核
  -> Obsidian Vault 归档
  -> MCP / UI / JSONL chunks 复用
  -> 维护巡检发现空正文、重复内容和未归档记录
  -> 人工标记已解决、忽略或重新打开维护问题
  -> 后续导入真实向量库
```

## 技术方向

- 后端：Java 21/17 + Spring Boot 3.x
- 前端：Vue 3 + Vite + TypeScript
- 数据库：MySQL 8.x
- 知识沉淀：Obsidian Markdown Vault
- 文件归集：本地 Raw Sources 目录
- 当前运行层：MCP HTTP Preview、JSONL Vector Export、Knowledge Maintenance
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

R6-3.1 已完成维护问题处理闭环。R6-2 Hybrid Search 仍等待向量库选型和部署方式；下一步可继续做 R6-3.2 修复建议模板、R6-4 办公室视图或 R6-5 定时总结。
