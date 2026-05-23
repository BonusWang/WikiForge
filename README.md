# 知识熔炉 WikiForge

知识熔炉 WikiForge 是一个本地优先的个人知识与资料整理系统。

MVP 的第一目标不是做完整 RAG，也不是做复杂多 Agent 平台，而是先解决一个现实问题：

> 本地资料太乱，需要先把散落源文件归集整理起来，并打通最小 Obsidian 归档闭环。

## MVP 闭环

```text
指定本地路径
  -> 扫描文件
  -> 复制归集到 WikiForge_RawSources
  -> 建立 MySQL 索引
  -> 选择少量文件进入处理
  -> 生成 Source Note Markdown 草案
  -> 人工审核
  -> 写入 Obsidian Vault
  -> Web UI 可查看状态并打开 Obsidian 文件
```

## 技术方向

- 后端：Java 21/17 + Spring Boot 3.x
- 前端：Vue 3 + Vite + TypeScript
- 数据库：MySQL 8.x
- 知识沉淀：Obsidian Markdown Vault
- 文件归集：本地 Raw Sources 目录
- 后续拓展：MCP、向量库、在线文档连接器、个人记录、办公室视图

## 文档

- [文档入口 Docs Index](docs/README.md)
- [需求文档 PRD](docs/current/需求文档-knowledge-base-prd.md)
- [技术架构 Technical Architecture](docs/current/技术架构-technical-architecture.md)
- [数据模型 Data Model](docs/current/数据模型-data-model.md)
- [MVP 实施计划 Implementation Plan](docs/current/2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan.md)
- [参考项目清单 Reference Projects](docs/current/2026-05-23-参考项目清单-WikiForge-reference-projects.md)
- [开发者日志 Developer Log](docs/current/2026-05-23-开发者日志-WikiForge-developer-log.md)
- [版本更新记录 Release Notes](docs/current/2026-05-23-版本更新记录-WikiForge-release-notes.md)

## 当前状态

项目已完成 MVP2.1 可用性加固，当前内部开发主线准备进入 MVP3 文档解析。GitHub 版本标签和 Release 由用户单独确认，不阻塞开发分支提交、推送和下一工作流递进。
