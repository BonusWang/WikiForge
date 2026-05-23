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

因此，WikiForge 需要升级工作模式。用户已进一步确认：WikiForge 需要自己的长期开发编排辅助工程，但不照搬 Symphony Elixir 服务端。

## 3. 采用方案

采用 D 方案：吸收 Symphony 编排思想，并落地为 WikiForge 自己的辅助工程。

保留：

- `AGENTS.md`
- `docs/current/`
- `docs/archive/YYYY-MM-DD/`
- `docs/superpowers/plans/`
- `docs/ai-skills/wikiforge-development/`

新增：

- `WORKFLOW.md`：Agent 工作流控制入口。
- `.github/ISSUE_TEMPLATE/wikiforge-agent-task.yml`：GitHub Issue 风格任务卡。
- `wikiforge-orchestration-service`：独立 Java / Spring Boot 编排状态服务。
- `wikiforge-orchestration-ui`：独立 Vue / Vite 编排控制台。
- Skill / reference 更新：明确 Issue 控制平面、任务状态、Handoff 和完成定义。

暂不引入：

- Symphony Elixir 服务端。
- 自动拉起 Codex runner。
- 全自动创建/关闭 GitHub Issue 的后台服务。
- 自动执行本机命令的 runner。

第一版边界：

- 先做只读任务清单、任务详情、任务状态统计和 UI 控制台。
- 任务数据先由服务内置种子和后续配置/文件导入提供。
- 不让辅助工程阻塞 WikiForge 主业务 MVP。
- 后续再增加 GitHub Issue 同步、worktree 管理、Agent runner、重试和审计。

## 4. 工作模式变更

旧模式：

```text
用户指令 -> Codex 读 docs/current 和 archive -> 写 Work Order -> 开发 -> 文档归档
```

新模式：

```text
用户目标
  -> Project Roadmap 阶段节点
  -> Orchestration Service / UI 任务控制台
  -> GitHub Issue 风格任务卡
  -> Work Order / Parallel Work Order
  -> 分支或 worktree 隔离开发
  -> Handoff Packet
  -> 主编排 Agent 集成验证
  -> 文档归档和提交推送
```

## 5. 当前结论

- 当前不照搬 Symphony 项目技术栈，不引入 Elixir 服务端。
- 当前新增 WikiForge 自己的 Orchestration 辅助工程，作为长期开发迭代控制台。
- MCP 开发继续，但在计划上后移到 Orchestration 基础服务和 UI 骨架之后。
- Orchestration 第一版只做可视化和状态管理，不做自动命令执行，先把可观察性和任务边界建立起来。
