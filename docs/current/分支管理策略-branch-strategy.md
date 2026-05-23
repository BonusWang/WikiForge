# WikiForge 分支管理策略 Branch Strategy

## 版本信息

- 文档版本：v0.1
- 生效日期：2026-05-23
- 当前适用阶段：MVP5 `0.08-preview.1` 之后

## 核心原则

分支要服务于开发节奏，而不是替代版本管理。

- `main` 是可运行、可发布、可对外说明的主干。
- 阶段开发分支只承载当前阶段实现，不长期承担“版本历史”职责。
- 版本历史以 tag、release notes、归档文档和提交记录为准。
- 历史分支清理需要人工确认，不由 Agent 自动删除远程分支。

## 分支分类

| 类型 | 命名 | 用途 | 生命周期 |
| --- | --- | --- | --- |
| 主干分支 Mainline | `main` | 稳定主线，所有阶段成果最终合入这里 | 长期保留 |
| 阶段分支 Milestone | `codex/mvp5-mcp-preview` | 一个 MVP 或阶段的集成分支 | 合入 main 后可归档保留或经确认删除 |
| 任务分支 Task | `codex/r4-4-mcp-personal-record` | 单个任务节点、Bugfix、文档调整 | 合入阶段分支或 main 后删除 |
| 实验分支 Experiment | `codex/exp-*` | 技术验证、Spike、方案对比 | 结论沉淀后删除 |
| 修复分支 Hotfix | `codex/hotfix-*` | 从 main 拉出的紧急修复 | 合入 main 后删除 |

## 当前分支处理建议

当前已有多个 `codex/mvp*` 分支，属于阶段演进遗留，暂时按历史阶段分支处理：

- `codex/mvp0-project-skeleton`
- `codex/mvp1-source-ingestion`
- `codex/mvp2-obsidian-source-note`
- `codex/mvp2.1-usability-hardening`
- `codex/mvp3-document-parsing`
- `codex/mvp4-ai-review`
- `codex/mvp5-mcp-preview`

这些分支可以先保留，便于追溯每个 MVP 的演进。后续如果主干、标签和归档文档都确认完整，再按清单逐个删除远程历史分支。

## 后续开发推荐

从 `0.08-preview.1` 之后，建议减少长期阶段分支数量：

- 小任务直接从 `main` 创建短生命周期任务分支。
- 多 Agent 并行时使用任务分支或 worktree，完成后由主编排 Agent 合入集成分支或 `main`。
- 阶段较大时只保留一个当前阶段集成分支，不为每个小节点保留长期远程分支。
- 每次阶段发布必须合入 `main`，否则 GitHub 默认分支看不到最新成果。

## 合并规则

- 发布候选通过验证后，必须合入 `main` 并推送远程。
- 如果是快进合并，优先使用 fast-forward，保持历史线性。
- 如果需要保留阶段上下文，可使用普通 merge commit，但提交信息必须说明阶段范围。
- 合入 `main` 后再确认是否创建 tag / GitHub Release。
- 标签和 GitHub Release 由用户确认；合入 `main` 是内部交付动作，不应被阻塞。

## 清理规则

Agent 默认不得自动删除远程分支。

允许自动执行：

- 列出本地和远程分支。
- 标记分支类别。
- 判断分支是否已合入 `main`。
- 生成建议清理清单。

需要用户确认后才能执行：

- 删除远程分支。
- 删除未合入 `main` 的本地分支。
- 重命名已有远程分支。

