# 2026-05-24 WikiForge 归档索引 Archive Index

## 版本信息

- 文档版本：v0.1
- 最新阶段：R5 / V1 在线资料与个人记录首版闭环已完成
- 推荐阅读：新 Agent 先读本索引，再读 `docs/current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md` 和当前 Work Order。

## 今日最新快照

- [项目整体计划 Project Roadmap](../../current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md)
- [开发者日志 Developer Log](../../current/2026-05-24-开发者日志-WikiForge-developer-log.md)
- [版本更新记录 Release Notes](../../current/2026-05-24-版本更新记录-WikiForge-release-notes.md)
- [V1 在线资料与个人记录 Work Order](../../superpowers/plans/2026-05-24-V1在线资料与个人记录-WikiForge-v1-lifeos-work-order.md)

## 当前结论

- R4 / MVP5 Orchestration + MCP 已完成，R5 / V1 首版闭环已落地。
- V1 不直接做真实飞书/腾讯 OAuth 抓取，先落地链接资料统一收集契约。
- 个人记录使用 `personal_records` 作为第一版统一账本，已支持 REST / UI / MCP 数据互通和 Obsidian 归档。
- 本轮开发必须保持前后端分离、Java + Spring Boot + MySQL 主技术栈和本地 Obsidian Vault 私有化存储。

## 验证记录

- 后端全量 Maven 测试：通过。
- 前端构建：通过。
- Docker Compose config：通过。
- Git 卫生、密钥和禁止路径扫描：通过。
- 浏览器自动化：当前会话未能加载 Playwright 模块，未完成截图验证。
