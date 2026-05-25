# 2026-05-24 WikiForge MVP0 基座

## 节点定位

本目录是 WikiForge 从现在开始的需求、设计、架构和计划基座。

MVP0 不继承旧阶段路线，不沿用 R 系列编号。旧文档只作为参考材料，当前开发只在本目录基础上递增。

## MVP0 目标

用最少系统完成个人知识库工具的第一条闭环：

```text
文件入口
  -> Raw Sources 收纳
  -> SourceFile 账本
  -> 正文抽取
  -> Obsidian LLM Wiki 写入
  -> index.md / log.md 更新
```

## 阅读顺序

1. [现状基线 Pre-Rebuild Baseline](2026-05-24-现状基线-WikiForge-pre-rebuild-baseline.md)
2. [MVP0 需求文档 PRD](2026-05-24-整体重构需求文档-WikiForge-rebuild-prd.md)
3. [四层架构设计 Four-Layer Architecture](2026-05-24-四层架构设计-WikiForge-four-layer-architecture.md)
4. [四层架构图 Drawio](diagrams/2026-05-24-WikiForge-four-layer-architecture.drawio)
5. [MVP0 前端设计方案 Frontend Design](2026-05-24-前端设计方案-WikiForge-mvp0-frontend-design.md)
6. [MVP0 后端设计方案 Backend Design](2026-05-24-后端设计方案-WikiForge-mvp0-backend-design.md)
7. [MVP0 数据库设计方案 Data Design](2026-05-24-数据库设计方案-WikiForge-mvp0-data-design.md)
8. [MVP0 API 契约设计 API Contract](2026-05-24-API契约设计-WikiForge-mvp0-api-contract.md)
9. [MVP0 Obsidian LLM Wiki 设计](2026-05-24-Obsidian-LLM-Wiki设计-WikiForge-mvp0-obsidian-llm-wiki-design.md)
10. [项目架构强约定 Project Architecture Conventions](../../current/项目架构强约定-WikiForge-project-architecture-conventions.md)
11. [现有资源盘点 Reusable Assets Inventory](2026-05-24-现有资源盘点-WikiForge-reusable-assets-inventory.md)
12. [MVP0 路线图 Roadmap](2026-05-24-整体重构路线-WikiForge-refactor-roadmap.md)
13. [MVP0 代码重构执行计划 Implementation Plan](2026-05-24-MVP0代码重构执行计划-WikiForge-mvp0-implementation-plan.md)
14. [MVP0 新起点交付记录 Starting Point](2026-05-25-MVP0新起点交付记录-WikiForge-mvp0-starting-point.md)

## 执行规则

- 单线推进，不做多 Agent 并行。
- 先设计对齐，再改代码。
- 蓝图阶段已完成；代码实现阶段只允许在本节点基线和强约定基础上推进。
- 后续新功能、新服务、新原子能力、新 API、新表结构必须先登记到项目架构强约定。
- 旧归档文档与本节点冲突时，以本节点为准。
- Orchestration 辅助开发工程不进入 MVP0，源码、Dockerfile 和 `agentteam/` 工作区已退役删除。

## 当前代码落地进度

2026-05-24 已完成 MVP0 第一轮代码落地，2026-05-25 已作为新起点完成运行验收：

- 前端主入口收敛为“收纳 / 资料箱 / Wiki / 日志 / 设置”。
- 浏览器上传入口已接入 Raw Sources 收纳和 SourceFile 账本登记。
- Core API 补齐中文状态字段、字典查询、Wiki ingest 运行记录。
- `system_dictionaries`、`wiki_ingest_runs` 进入 MVP0 最小表集合。
- Wiki ingest 已能写入 Obsidian `WikiForge/` 来源页、`index.md` 和 `log.md`。
- Orchestration 辅助工程已退出默认 Maven、Docker Compose 和 CI 路径，源码与独立前端已删除。
- 浏览器上传后的 `md/txt/pdf/docx` 已接入正文抽取。
- 本机验收 Vault 固定为 `E:\WikiForgeVault`。

## 交付物

| 交付物 | 作用 |
| --- | --- |
| 现状基线 | 说明旧文档与旧代码如何作为参考材料读取 |
| MVP0 PRD | 固定产品目标和功能边界 |
| 四层架构设计 | 固定轻应用层、决策层、指令执行层、原子能力层 |
| 前端设计方案 | 固定五入口、页面职责、样式基线和中文状态展示 |
| 后端设计方案 | 固定 Core、Worker、Common、API 和任务边界 |
| 数据库设计方案 | 固定最小表集合、中文状态字典和历史表处理策略 |
| API 契约设计 | 固定前端、Core、Worker 的请求响应字段和错误边界 |
| Obsidian LLM Wiki 设计 | 固定 Vault 托管目录、页面类型、托管区块、index/log 写入规则 |
| Drawio 图纸 | 展示业务能力、前后端架构、数据流和原子能力 |
| 资源盘点 | 标记复用、适配、冻结、退役 |
| MVP0 路线图 | 将后续开发拆成小步验证节点 |
| MVP0 代码重构执行计划 | 将代码重构拆成可执行、可验证任务 |
| MVP0 新起点交付记录 | 固定本次合入 main 的运行口径、验收结果和后续边界 |
