---
name: wikiforge-r4-5-test-agent
description: Test and verification skill for WikiForge R4-5.
---

# 测试Agent Skill

## 验证分层

- T0：`git diff --check`、Git 卫生、文档命名检查。
- T1：后端定向测试、前端类型和构建。
- T2：前后端联动构建。
- T3：Docker Compose config 和必要烟测。
- T4：阶段发布前全量验收。

## 审查重点

- MCP 工具清单和调用日志是否一致。
- 日志是否脱敏。
- UI 是否有加载、空态、错误态。
- OpenClaw / Hermes 接入说明是否可复现。
- 构建产物、本地运行数据和密钥是否未进入 Git。
