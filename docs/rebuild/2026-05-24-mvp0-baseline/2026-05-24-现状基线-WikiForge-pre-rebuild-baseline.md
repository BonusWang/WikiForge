# 2026-05-24 WikiForge MVP0 现状基线 MVP0 Baseline

## 目的

本文件定义 MVP0 从哪里重新开始，以及哪些历史材料只作为参考。

结论：旧文档归档留存，旧代码暂不移动；MVP0 只围绕个人知识库最小闭环重新设计。

## 文档基线

旧主文档已经迁入：

```text
docs/archive/2026-05-24/pre-rebuild-docs/
```

统一索引：

- [重构前文档归档索引](../../archive/2026-05-24/pre-rebuild-docs/2026-05-24-重构前文档归档索引-WikiForge-pre-rebuild-docs-index.md)

读取规则：

- 默认只读 MVP0 基座。
- 需要解释旧模块、旧接口或旧数据表时，再读归档。
- 旧阶段状态不代表当前计划。

## 代码基线

代码仍在原目录，本节点不移动、不删除。

重点参考资源：

| 方向 | 当前资源 | MVP0 处理 |
| --- | --- | --- |
| 路径安全 | `PathSafety` | 复用 |
| 错误码和统一响应 | `ErrorCode`、`ApiResponse`、`PageResult` | 复用 / 适配 |
| 路径导入账本 | `ImportJob`、`SourceFile`、`SourceContent` | 适配 |
| 本地扫描与复制 | `LocalFileScanner`、`LocalImportJobRunner` | 适配 |
| 正文抽取 | `TextContentExtractor` | 复用 |
| Obsidian 写入 | `ObsidianVaultService`、`WikiCompileService` | 适配 |
| 前端单页 | `DashboardView.vue` | 已迁移并退役删除 |
| 辅助开发工程 | `wikiforge-orchestration-service`、`orchestration-ui` | 退役 |

详细清单以 [现有资源盘点](2026-05-24-现有资源盘点-WikiForge-reusable-assets-inventory.md) 为准。

## 隔离策略

```text
MVP0 基座：docs/rebuild/2026-05-24-mvp0-baseline/
公共规则：AGENTS.md / WORKFLOW.md / docs/README.md / docs/current/
历史材料：docs/archive/2026-05-24/pre-rebuild-docs/
代码实现：原目录保留，按 MVP0 后续节点小步迁移
```

## 后续节点

- MVP0-1：需求和前后端设计对齐。
- MVP0-2：前端主流程骨架。
- MVP0-3：Core / Worker 最小收纳链路。
- MVP0-4：Obsidian LLM Wiki 写入。
- MVP0-5：浏览器上传入口。
- MVP0-6：历史高级能力和辅助工程退场整理。
