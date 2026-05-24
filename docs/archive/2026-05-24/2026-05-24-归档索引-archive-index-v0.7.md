# 2026-05-24 WikiForge 归档索引 Archive Index

## 版本信息

- 文档版本：v0.7
- 最新阶段：MVP0 重新开始，重构前 `docs/current/` 历史文档已迁入 `pre-rebuild-docs/`。
- 推荐阅读：新任务先读 `docs/README.md` 和 `docs/rebuild/2026-05-24-mvp0-baseline/`；仅追溯旧路线时读取本归档索引和 `pre-rebuild-docs/`。

## 当前 MVP0 基座

- [MVP0 基座 README](../../rebuild/2026-05-24-mvp0-baseline/README.md)
- [现状基线 Baseline](../../rebuild/2026-05-24-mvp0-baseline/2026-05-24-现状基线-WikiForge-pre-rebuild-baseline.md)
- [MVP0 需求文档 PRD](../../rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构需求文档-WikiForge-rebuild-prd.md)
- [MVP0 四层架构设计](../../rebuild/2026-05-24-mvp0-baseline/2026-05-24-四层架构设计-WikiForge-four-layer-architecture.md)
- [四层架构图 Drawio](../../rebuild/2026-05-24-mvp0-baseline/diagrams/2026-05-24-WikiForge-four-layer-architecture.drawio)
- [MVP0 资源盘点](../../rebuild/2026-05-24-mvp0-baseline/2026-05-24-现有资源盘点-WikiForge-reusable-assets-inventory.md)
- [MVP0 路线图](../../rebuild/2026-05-24-mvp0-baseline/2026-05-24-整体重构路线-WikiForge-refactor-roadmap.md)
- [分支管理策略](../../current/分支管理策略-branch-strategy.md)

## 本轮归档

- [重构前文档归档索引 Pre-Rebuild Docs Index](pre-rebuild-docs/2026-05-24-重构前文档归档索引-WikiForge-pre-rebuild-docs-index.md)

## 当前结论

- WikiForge 从 MVP0 重新开始，不继承旧阶段路线。
- 主流程固定为：路径扫描 / 浏览器上传 -> Raw Sources 复制收纳 -> hash 去重和类型识别 -> 正文抽取 -> Obsidian Wiki 写入 -> `index.md` / `log.md` 更新。
- MCP、向量导出、LifeOS、知识体检冻结，不进入 MVP0 主流程。
- Orchestration 辅助开发工程退役，后续从构建和文档入口清理。
- 本轮只建立 MVP0 基座和文档隔离，不移动业务代码、不改 API、不改数据库。

## 版本记录 Version History

### v0.7

- 记录项目从历史阶段抽离，按 MVP0 重新开始。
- 记录重构前历史主文档从 `docs/current/` 迁入 `pre-rebuild-docs/`。
- 记录公共规则收敛：单线推进，不做多 Agent 并行，不继续规划辅助开发工程。

### v0.6

- 历史阶段索引，已不再作为当前开发主线。
