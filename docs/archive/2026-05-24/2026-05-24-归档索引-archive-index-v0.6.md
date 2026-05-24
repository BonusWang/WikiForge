# 2026-05-24 WikiForge 归档索引 Archive Index

## 版本信息

- 文档版本：v0.6
- 最新阶段：R6-version-api / WikiForge 版本 API 小版本已完成，R6-2 真实向量库和 Hybrid Search 继续挂起
- 推荐阅读：新 Agent 先读本索引，再读 `docs/current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md` 和当前 Work Order。

## 今日最新快照

- [项目整体计划 Project Roadmap](../../current/2026-05-24-项目整体计划-WikiForge-project-roadmap.md)
- [开发者日志 Developer Log](../../current/2026-05-24-开发者日志-WikiForge-developer-log.md)
- [版本更新记录 Release Notes](../../current/2026-05-24-版本更新记录-WikiForge-release-notes.md)
- [V1 在线资料与个人记录 Work Order](../../superpowers/plans/2026-05-24-V1在线资料与个人记录-WikiForge-v1-lifeos-work-order.md)
- [V2 向量导出契约 Work Order](../../superpowers/plans/2026-05-24-V2向量导出契约-WikiForge-r6-vector-export-contract.md)
- [V2 知识维护巡检 Work Order](../../superpowers/plans/2026-05-24-V2知识维护巡检-WikiForge-r6-maintenance-lint-agent.md)
- [V2 知识维护处理闭环 Work Order](../../superpowers/plans/2026-05-24-V2知识维护处理闭环-WikiForge-r6-maintenance-issue-workflow.md)

## 当前结论

- R4 / MVP5 Orchestration + MCP 已完成，R5 / V1 首版闭环已落地。
- R6-1 / V2 向量导出契约已落地：`vector_export_jobs`、`content_chunks`、JSONL 导出和 Dashboard 入口完成。
- R6-3 / V2 知识维护巡检首版已落地：`knowledge_maintenance_runs`、`knowledge_maintenance_items`、维护巡检 API 和 Dashboard 入口完成。
- R6-3.1 / V2 知识维护处理闭环已完成：维护问题支持已解决、忽略和重新打开，首版仍不自动修复。
- R6-UI-1 / Console 暗色开发者控制台主题已完成：主 UI 改为深黑蓝背景、霓虹绿色主强调、紫色和橙色辅助状态，并写入 PRD 与技术架构。
- R6-version-api / WikiForge 版本 API 小版本已完成：Core Service 新增 `GET /api/v1/version`，当前小版本推进到 `2.0-v2-preview.4`，本轮不创建 tag / GitHub Release。
- V1 不直接做真实飞书/腾讯 OAuth 抓取，先落地链接资料统一收集契约。
- 个人记录使用 `personal_records` 作为第一版统一账本，已支持 REST / UI / MCP 数据互通和 Obsidian 归档。
- R6-2 真实向量库与 Hybrid Search、R6-4 办公室视图和 R6-5 长期记忆继续挂起到后续节点。
- 本轮开发必须保持前后端分离、Java + Spring Boot + MySQL 主技术栈和本地 Obsidian Vault 私有化存储。

## 验证记录

- R6-3 后端定向 Maven 测试：通过，`KnowledgeMaintenanceApiIntegrationTests` 和 `MigrationSqlCompatibilityTests` 合计 10 个测试，0 失败。
- R6-3 前端构建：通过。
- R6-3 后端全量 Maven 测试：通过，5 个模块合计 62 个测试，0 失败。
- R6-3 Docker Compose config：生产与开发配置均通过。
- R6-3 Git 卫生、密钥和禁止路径扫描：通过。
- R6-3 页面检查：`http://localhost:3000/` 返回 200 且包含 Vue app 挂载节点；当前会话缺少 Playwright 模块，未完成自动截图检查。
- R6-3.1 后端定向 Maven 测试：通过，`KnowledgeMaintenanceApiIntegrationTests` 和 `MigrationSqlCompatibilityTests` 合计 13 个测试，0 失败。
- R6-3.1 后端全量 Maven 测试：通过，5 个模块合计 65 个测试，0 失败。
- R6-3.1 前端构建：通过，保留既有 Rollup 大 chunk 和 VueUse PURE 注释 warning。
- R6-3.1 Docker Compose config：生产与开发配置均通过。
- R6-3.1 Git 卫生、密钥和禁止路径扫描：通过。
- R6-3.1 页面检查：Vite preview `http://127.0.0.1:3003/` 返回 200；构建产物包含维护问题状态更新和重新打开操作。
- R6-UI-1 前端构建：通过，保留既有 Rollup 大 chunk 和 VueUse PURE 注释 warning。
- R6-UI-1 页面检查：`http://127.0.0.1:3000/` 返回 200 且包含 Vue app 挂载节点。
- R6-UI-1 Git whitespace 检查：通过。
- R6-version-api RED 定向测试：预期失败，`/api/v1/version` 返回 404。
- R6-version-api GREEN 定向测试：通过，`WikiForgeCoreApplicationTests` 合计 2 个测试，0 失败。
- R6-version-api Core 相关 Maven 测试：通过，`wikiforge-common` + `wikiforge-core-service` 合计 46 个测试，0 失败。
- R6-version-api 工单验证命令：`echo wikiforge-test-ok` 返回 `wikiforge-test-ok`。
- R6-1 后端全量 Maven 测试：通过，5 个模块合计 58 个测试，0 失败。
- R6-1 Docker Compose config：通过。
- R6-1 Git 卫生、密钥和禁止路径扫描：通过。
- 浏览器自动化：当前会话未能加载 Playwright 模块，未完成截图验证。

## 版本记录 Version History

### v0.6

- 增加 R6-version-api / WikiForge 版本 API 小版本结论。
- 记录 `GET /api/v1/version`、`2.0-v2-preview.4` 和 ForgeOps Bridge 后续发布边界。
- 标记 RED / GREEN 定向测试、Core 相关 Maven 测试和工单验证命令通过。

### v0.5

- 增加 R6-UI-1 Console 暗色开发者控制台主题结论。
- 记录 PRD、技术架构和 `frontend/src/styles/main.css` 的设计规范同步。
- 标记前端构建、页面 HTTP 检查和 Git whitespace 检查通过。

### v0.4

- 增加 R6-3.1 知识维护处理闭环结论和验证记录。
- 新增 R6-3.1 Work Order 链接。
- 标记当前任务从“只读巡检”推进到“问题可处理队列”并完成。

### v0.3

- 增加 R6-3 知识维护巡检结论。
- 新增 R6-3 Work Order 链接。
- 标记 R6-2 Hybrid Search 继续受向量库选型阻塞。

### v0.2

- 增加 R6-1 向量导出契约结论。
- 新增 V2 Work Order 链接。
- 标记真实向量库和知识运行高级能力为后续挂起事项。

### v0.1

- 记录 R5 / V1 在线资料与个人记录首版闭环完成。
