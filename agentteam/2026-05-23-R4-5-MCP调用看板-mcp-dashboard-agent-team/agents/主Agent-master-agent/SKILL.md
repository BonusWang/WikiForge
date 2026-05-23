---
name: wikiforge-r4-5-master-agent
description: Master coordination skill for WikiForge R4-5 MCP dashboard work.
---

# 主Agent Skill

## 工作原则

- 先读最新快照，再读 current 文档，最后读本 Agent Team 目录。
- 每轮只推进一个主执行指针，但可以让专业 Agent 并行处理独立文件范围。
- 专业 Agent 的状态以其 `STATUS.md` 为准，正式项目文档由主 Agent 统一更新。
- 合并前检查文件边界、测试结果、敏感信息和 Git 状态。

## 集成检查

1. 阅读专业 Agent `STATUS.md`。
2. 检查实际 diff 是否越界。
3. 运行对应验证命令。
4. 更新 `任务计划-team-plan.md`。
5. 更新正式文档和归档。
6. 提交、推送、必要时发布。
