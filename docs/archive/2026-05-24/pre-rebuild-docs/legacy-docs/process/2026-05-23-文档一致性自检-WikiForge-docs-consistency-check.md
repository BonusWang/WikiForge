# 2026-05-23 WikiForge 文档一致性自检 Docs Consistency Check

## 检查目标

本次自检用于避免后续 Agent 因旧文件名、旧状态或纯英文标题误判项目进度。

检查范围：

- `README.md`
- `AGENTS.md`
- `docs/README.md`
- `docs/current/`
- `docs/superpowers/plans/`
- `docs/archive/2026-05-23/` 中当天最新快照

## 结论

- `README.md`、`AGENTS.md`、`SKILL.md` 等生态约定文件保持原名，不做中文化改名。
- `docs/current/` 当前主线文档已统一为中文名 + EnglishName 的文件名或标题。
- 当前开发者日志主文件已修正为 `docs/current/2026-05-23-开发者日志-WikiForge-developer-log.md`。
- 版本标签和 GitHub Release 已统一为 `External Pending` 外部发布事项，不作为内部开发阻塞项。
- 历史归档旧版本保留原貌，只修正最新快照和当前主线文档。

## 已修正规则

- 项目文档文件名采用 `中文名-EnglishName.md` 或 `YYYY-MM-DD-中文名-EnglishName.md`。
- 项目文档一级标题必须包含中文语义和英文标识。
- 当前主线文档如果日期前缀与最新迭代日期不一致，完成本轮修改时必须同步改名并更新入口链接。
- `README.md`、`AGENTS.md`、`SKILL.md`、`.gitignore`、`.env.example` 属于约定文件名例外，不改名。

## 本轮修正

- `docs/current/2026-05-22-开发者日志-WikiForge-developer-log.md` 改名为 `docs/current/2026-05-23-开发者日志-WikiForge-developer-log.md`。
- `docs/current/2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md` 改名为 `docs/current/2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan.md`。
- `docs/current/2026-05-22-参考项目清单-WikiForge-reference-projects.md` 改名为 `docs/current/2026-05-23-参考项目清单-WikiForge-reference-projects.md`。
- 修正 `docs/superpowers/plans/` 中纯英文标题，补充中文语义。
- 更新 `README.md` 和 `docs/README.md` 的 current 文档入口链接。
- 更新 `AGENTS.md` 文档命名规则，明确约定文件名例外。
