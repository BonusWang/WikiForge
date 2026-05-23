# WikiForge Agent Team

本目录用于保存 WikiForge 每一轮可并行开发的 Agent Team 配置。

## 设计来源

参考 `E:\github\claude-agent-examples` 后，WikiForge 吸收三点：

- Todolist：任务必须结构化拆解，并且同一时间只有一个主执行指针。
- Subagent：一次性探索和只读审查可以交给独立上下文，主 Agent 只接收 Handoff。
- Agent Team：长期项目需要固定角色、状态文件、任务边界和可追踪的工作空间。

WikiForge 不直接照搬 Python 运行时实现，当前先落地为文件化协作规范。后续 Orchestration Service / UI 可读取这些目录，逐步演进成可视化团队控制台。

## 目录规则

每个 Agent Team 必须是独立目录：

```text
agentteam/{date}-{task-id}-{team-name}/
```

每个 Agent 必须是独立目录：

```text
agentteam/{team}/agents/{agent-name}/
  README.md
  PROMPT.md
  SKILL.md
  WORKSPACE.md
  STATUS.md
```

约定文件名 `README.md`、`PROMPT.md`、`SKILL.md`、`WORKSPACE.md`、`STATUS.md` 保持英文生态命名；文件内容必须包含中文说明。

## 协作边界

- 主 Agent 负责计划、派工、状态跟踪、集成、验证、正式文档和发布。
- 专业 Agent 只能修改自己 `WORKSPACE.md` 中允许的文件。
- 专业 Agent 完成后更新自己的 `STATUS.md`，不要直接修改 Roadmap、开发者日志、归档索引和发布说明。
- 高冲突文件仍由主 Agent 串行处理。
