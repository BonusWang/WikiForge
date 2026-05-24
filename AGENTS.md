# AGENTS.md

本文件用于约束参与 WikiForge 的 AI Agent。当前项目从历史阶段抽离，按 **MVP0 重新开始**。

## 当前基座

- 当前阶段：MVP0。
- 当前目标：做减法，重建个人私有知识库工具的最小可用闭环。
- 当前事实来源：`docs/rebuild/2026-05-24-mvp0-baseline/`。
- 历史文档位置：`docs/archive/2026-05-24/pre-rebuild-docs/`。
- 历史文档只作为参考，不直接驱动开发。

## 开始工作前

进行代码开发、文档修改、需求分析或架构设计前，按顺序阅读：

1. `WORKFLOW.md`
2. `docs/README.md`
3. `docs/rebuild/2026-05-24-mvp0-baseline/README.md`
4. 当前任务需要的 PRD、四层架构、路线图或资源盘点
5. 涉及新功能、新服务、新原子能力、新 API 或新表时，阅读并更新 `docs/current/项目架构强约定-WikiForge-project-architecture-conventions.md`

只有在追溯旧实现、解释旧接口、复用旧模块时，才读取 `docs/archive/`。

## 优先级

1. 用户当前明确指令。
2. MVP0 基座文档。
3. 当前代码事实。
4. 历史归档材料。

如果历史材料与 MVP0 基座冲突，以 MVP0 基座为准。

## 当前不做

- 不建立多 Agent 并行体系。
- 不维护并行协作目录体系。
- 不把 Orchestration 辅助开发工程作为产品能力继续规划。
- 不把 MCP、向量、LifeOS、知识体检提前塞回 MVP0 主流程。
- 不恢复旧 `DashboardView.vue` 单体页面。
- 不为未来能力预建数据库表。

## 文档结构

```text
docs/current/        # 少量仍有效的公共规则
docs/rebuild/        # MVP0 需求、设计、架构和计划基座
docs/archive/        # 历史文档归档
```

新增重构文档优先放入：

```text
docs/rebuild/2026-05-24-mvp0-baseline/
```

除 `README.md`、`AGENTS.md`、`WORKFLOW.md` 等生态约定文件外，项目文档采用中文名 + EnglishName：

```text
YYYY-MM-DD-中文名-EnglishName.md
```

## 开发原则

- 先做 MVP0 最小闭环，再谈扩展能力。
- 文件收纳、Raw Sources、Obsidian LLM Wiki 是主线。
- 数据库只保留 MVP0 当前需要的最小表集合，后续按需求新增。
- 新增功能、服务、原子能力、API、数据库表和状态码必须同步登记到项目架构强约定。
- 用户可见状态必须使用中文码值和中文说明，由字典表统一维护。
- 新增或修改 API 时必须同步维护 MVP0 API 契约设计。
- Obsidian 写入只能发生在 Vault 内 `WikiForge/` 托管目录。
- 代码改动要小步、可验证、可回滚。
- 不静默覆盖用户手工修改。
- 不提交 Raw Sources、Obsidian Vault、本地 `.env`、运行日志或数据库数据。

## Git 规则

- `main` 是稳定主干。
- 日常任务使用短生命周期分支，建议命名 `codex/mvp0-*`。
- 历史分支只用于追溯，不自动删除。
- 提交前检查 `git status --short` 和 `git diff --check`。
