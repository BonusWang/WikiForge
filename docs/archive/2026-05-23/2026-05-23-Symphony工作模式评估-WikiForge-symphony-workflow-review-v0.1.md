# 2026-05-23 WikiForge Symphony 工作模式评估 Symphony Workflow Review

## 1. 评估背景

用户提出需要学习 OpenAI 发布的 Codex Orchestration: Symphony，并判断 WikiForge 是否需要重构当前工作模式。

参考资料：

- OpenAI 文章：[Open-source Codex orchestration with Symphony](https://openai.com/zh-Hans-CN/index/open-source-codex-orchestration-symphony/)
- GitHub 仓库：[openai/symphony](https://github.com/openai/symphony)
- 规格文档：[SPEC.md](https://raw.githubusercontent.com/openai/symphony/main/SPEC.md)
- 原型说明：[elixir/README.md](https://raw.githubusercontent.com/openai/symphony/main/elixir/README.md)

## 2. 对 WikiForge 的判断

Symphony 解决的核心问题是：当多个 Codex / Agent 会话并行工作时，人工管理任务状态、上下文切换、重试和交接会变成瓶颈。

WikiForge 当前已经遇到相似问题：

- 用户多次要求“不要停下来，按计划递进”。
- 项目已有 `AGENTS.md`、归档索引、Work Order 和多 Agent 规则，但缺少统一任务控制入口。
- 长期会接入 OpenClaw / Hermes / MCP / 多 Agent，后续任务数量会明显增加。

因此，WikiForge 需要升级工作模式，但不需要立即引入 Symphony 服务端。

## 3. 采用方案

采用 C 方案：轻量吸收 Symphony 编排思想。

保留：

- `AGENTS.md`
- `docs/current/`
- `docs/archive/YYYY-MM-DD/`
- `docs/superpowers/plans/`
- `docs/ai-skills/wikiforge-development/`

新增：

- `WORKFLOW.md`：Agent 工作流控制入口。
- `.github/ISSUE_TEMPLATE/wikiforge-agent-task.yml`：GitHub Issue 风格任务卡。
- Skill / reference 更新：明确 Issue 控制平面、任务状态、Handoff 和完成定义。

暂不引入：

- Symphony Elixir 服务端。
- 独立任务调度数据库。
- 自动拉起 Codex runner。
- 全自动创建/关闭 GitHub Issue 的后台服务。

## 4. 工作模式变更

旧模式：

```text
用户指令 -> Codex 读 docs/current 和 archive -> 写 Work Order -> 开发 -> 文档归档
```

新模式：

```text
用户目标
  -> Project Roadmap 阶段节点
  -> GitHub Issue 风格任务卡
  -> Work Order / Parallel Work Order
  -> 分支或 worktree 隔离开发
  -> Handoff Packet
  -> 主编排 Agent 集成验证
  -> 文档归档和提交推送
```

## 5. 当前结论

- 当前不重构 WikiForge 技术架构。
- 当前重构 WikiForge 开发协作模式。
- MVP5 MCP 开发继续，但必须先完成工作模式升级和计划清单更新。
- 后续当并行 Agent 数量、任务失败重试、自动调度需求明显增加时，再评估是否实现 WikiForge 自己的 Orchestration Service。
