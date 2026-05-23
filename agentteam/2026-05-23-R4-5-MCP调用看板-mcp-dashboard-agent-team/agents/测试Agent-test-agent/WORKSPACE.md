# 测试Agent Workspace

## 允许修改

- 本目录 `STATUS.md`
- 如主 Agent 明确授权，可新增测试报告草稿到本 Agent 目录

## 默认只读检查范围

- `backend/**`
- `frontend/**`
- `deploy/**`
- `.github/**`
- `docs/current/**`
- `agentteam/**`

## 禁止修改

- 业务实现文件
- 正式 Roadmap、开发者日志、归档索引、Release Notes
- 其他 Agent 目录下的 `STATUS.md`

## 建议分支

```text
codex/r4-5-test-review
```

## 验证命令

```powershell
git diff --check
cd E:\github\WikiForge\backend; mvn test
cd E:\github\WikiForge\frontend; npm run build
cd E:\github\WikiForge; docker compose -f deploy/docker-compose.yml config
```
