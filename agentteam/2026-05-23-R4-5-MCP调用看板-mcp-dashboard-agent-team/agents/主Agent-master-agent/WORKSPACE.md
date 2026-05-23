# 主Agent Workspace

## 允许修改

- `AGENTS.md`
- `WORKFLOW.md`
- `agentteam/**`
- `docs/current/**`
- `docs/archive/2026-05-23/**`
- `docs/ai-skills/wikiforge-development/**`
- 集成阶段必要的代码文件

## 禁止修改

- 不直接覆盖专业 Agent 未审查的代码改动。
- 不删除远程分支，除非用户明确确认。
- 不提交 `node_modules/`、`dist/`、`target/`、`.env`、Vault、Raw Sources、运行日志。

## 当前高冲突串行区

- Flyway migration
- 共享 DTO 和错误码
- `.github/workflows/**`
- `deploy/docker-compose*.yml`
- Roadmap、开发者日志、归档索引、Release Notes
