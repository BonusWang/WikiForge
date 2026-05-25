# WikiForge 分支管理策略 Branch Strategy

## 版本信息

- 文档版本：v0.4
- 生效日期：2026-05-25
- 当前适用阶段：MVP0 重新开始

## 核心原则

- `main` 是稳定主干。
- 日常开发使用短生命周期任务分支。
- 当前 MVP0 重构分支必须与重构前历史分支分开命名。
- 历史分支只用于追溯，不作为当前阶段依据。
- 不自动删除远程历史分支。

## 分支类型

| 类型 | 命名 | 用途 |
| --- | --- | --- |
| 主干 | `main` | 稳定主线 |
| MVP0 任务 | `codex/mvp0-*` | 当前重构、设计、功能开发 |
| 修复 | `codex/hotfix-*` | 紧急修复 |
| 实验 | `codex/exp-*` | 明确的短期验证 |
| 本地历史归档 | `archive/pre-rebuild/*` | 重构前分支的本地归档入口 |

## 当前分支边界

- 当前 MVP0 稳定入口：`main` / `origin/main`。
- 当前 MVP0 日常任务：只使用 `codex/mvp0-*`、`codex/hotfix-*` 或 `codex/exp-*`。
- MVP0 reset 源分支：`codex/mvp0-reset-baseline-source`，只用于追溯本次重置来源，不作为新任务起点。
- 重构前本地归档入口：`archive/pre-rebuild/*`。
- 重构前远端分支：仍保留在 `origin/codex/mvp*`、`origin/codex/r*`、`origin/codex/v1-*` 等原位置，只用于追溯。

## 使用规则

- 新任务默认从 `origin/main` 创建 `codex/mvp0-{topic}`，除非用户明确要求沿用当前工作分支。
- 大改动可创建 `codex/mvp0-{topic}`。
- 不从 `archive/pre-rebuild/*` 或重构前远端分支继续开发。
- 提交前必须运行本节点要求的验证命令。
- 合入 `main` 前必须确认没有 Raw Sources、Obsidian Vault、本地数据或 `.env`。

## 历史分支

已有 `codex/mvp*`、`codex/r*`、`codex/v1-*`、`codex/wo-*` 或其他重构前历史分支只保留为追溯材料。是否删除或改名远端分支必须由用户确认。

本地可将重构前分支重命名到 `archive/pre-rebuild/*`。远端清理按以下顺序执行：

1. 先确认每个历史分支的归档目标。
2. 创建远端归档引用。
3. 再删除原远端历史分支。
4. 删除前后都执行 `git fetch --all --prune --tags` 和 `git branch --all --verbose --no-abbrev` 复核。
