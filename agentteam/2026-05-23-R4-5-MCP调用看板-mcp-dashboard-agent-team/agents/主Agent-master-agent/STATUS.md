# 主Agent Status

| 字段 | 内容 |
| --- | --- |
| 任务ID | R4-5-5 |
| 状态 | Done |
| 分支 | `codex/r4-5-mcp-dashboard-connectors` |
| 当前目标 | R4-5 发布收口完成 |
| 已完成 | MCP 只读看板、calls 查询测试、OpenClaw / Hermes 接入说明、发布候选验证、Release Notes、main 合并、tag 和 GitHub Release |
| 待完成 | MVP 阶段结束；后续 V1 需新切片 |
| 验证命令 | 后端全量 Maven 测试、前端构建、Compose config、Git 卫生、敏感信息扫描和禁止路径扫描均通过 |
| 风险 | 完整 MCP transport、MCP Client、多用户权限、办公室视图和向量库属于后续阶段 |

## 发布结果

- Main commit: `e8c5bec`
- Tag: `0.08-preview.3`
- Release: https://github.com/BonusWang/WikiForge/releases/tag/0.08-preview.3

## 会话续接提示

如果主 Agent 会话中断，新会话先读本目录 `CONTEXT.md`，再读本文件。
