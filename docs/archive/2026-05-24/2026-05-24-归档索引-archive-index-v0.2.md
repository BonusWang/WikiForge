# 2026-05-24 WikiForge 归档索引 Archive Index

## 版本信息

- 文档版本：v0.2
- 最新阶段：R6-1 / V2 向量导出契约已完成，真实向量库接入挂起到后续节点
- 推荐阅读：新 Agent 先读本索引，再读 `docs/current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md` 和当前 Work Order。

## 今日最新快照

- [项目整体计划 Project Roadmap](../../current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md)
- [开发者日志 Developer Log](../../current/2026-05-24-开发者日志-WikiForge-developer-log.md)
- [版本更新记录 Release Notes](../../current/2026-05-24-版本更新记录-WikiForge-release-notes.md)
- [V1 在线资料与个人记录 Work Order](../../superpowers/plans/2026-05-24-V1在线资料与个人记录-WikiForge-v1-lifeos-work-order.md)
- [V2 向量导出契约 Work Order](../../superpowers/plans/2026-05-24-V2向量导出契约-WikiForge-r6-vector-export-contract.md)

## 当前结论

- R4 / MVP5 Orchestration + MCP 已完成，R5 / V1 首版闭环已落地。
- R6-1 / V2 向量导出契约已落地：`vector_export_jobs`、`content_chunks`、JSONL 导出和 Dashboard 入口完成。
- V1 不直接做真实飞书/腾讯 OAuth 抓取，先落地链接资料统一收集契约。
- 个人记录使用 `personal_records` 作为第一版统一账本，已支持 REST / UI / MCP 数据互通和 Obsidian 归档。
- R6 后续真实向量库、Hybrid Search、Lint / Maintain Agent、办公室视图和长期记忆继续挂起到后续节点。
- 本轮开发必须保持前后端分离、Java + Spring Boot + MySQL 主技术栈和本地 Obsidian Vault 私有化存储。

## 验证记录

- 后端全量 Maven 测试：通过，5 个模块合计 58 个测试，0 失败。
- 前端构建：通过。
- Docker Compose config：通过。
- Git 卫生、密钥和禁止路径扫描：通过。
- 浏览器自动化：当前会话未能加载 Playwright 模块，未完成截图验证。

## 版本记录 Version History

### v0.2

- 增加 R6-1 向量导出契约结论。
- 新增 V2 Work Order 链接。
- 标记真实向量库和知识运行高级能力为后续挂起事项。

### v0.1

- 记录 R5 / V1 在线资料与个人记录首版闭环完成。
