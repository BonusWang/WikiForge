# 2026-05-23 WikiForge MVP2 发布前自检 MVP2 Release Checklist

## 版本信息

- 文档版本：v0.1
- 当前分支：`codex/mvp2-obsidian-source-note`
- 发布候选标签：`0.04`
- 对应路线图节点：R0-4
- 当前结论：MVP2 主链路已验证通过，提交、打标签和推送仍需用户明确授权

## 发布范围

本次 `0.04` 只发布 MVP2 Obsidian Source Note 闭环：

- 初始化 Obsidian Vault 混合目录。
- 基于 Source File 生成 Source Note Markdown 草案。
- 写入 `obsidian_notes` 映射记录。
- 将 Markdown 写入宿主机 `E:\WikiForgeVault`。
- Web UI 支持编辑、写入、预览和打开 Obsidian。

本次不包含：

- 文档正文解析。
- AI 摘要、标签和分类。
- MCP 服务。
- 向量库导入。
- 飞书、腾讯文档、邮件、账单、人际关系连接器。
- 办公室 Agent 视图。

## R0 节点状态

当前节点单选：

- ( ) R0-1 项目整体路线图
- ( ) R0-2 版本更新记录
- ( ) R0-3 Docker 端到端烟测
- ( ) R0-4 发布前自检清单
- (x) R0-5 提交、标签和推送

| 完成 | 节点 | 状态 | 结果 |
| --- | --- | --- | --- |
| [x] | R0-1 | Done | 项目整体路线图已创建，并加入 `docs/README.md` |
| [x] | R0-2 | Done | 版本更新记录已补充 `0.04` 发布说明草案 |
| [x] | R0-3 | Done | Docker 端到端 Source Note 烟测通过 |
| [x] | R0-4 | Done | 本发布前自检清单已输出 |
| [ ] | R0-5 | Blocked | 提交、标签 `0.04`、推送需要用户授权 |

## 测试门禁

本清单采用按节点递进测试，不按每个功能重复执行完整验收。

当前测试层级单选：

- ( ) T0 文档与 Git 卫生
- ( ) T1 契约与单元测试
- ( ) T2 构建验证
- ( ) T3 Docker 节点烟测
- (x) T4 阶段级端到端验收

| 完成 | 门禁 | 适用范围 | 本轮结果 |
| --- | --- | --- | --- |
| [x] | T0 | 文档、空白、Git 卫生 | 通过 |
| [x] | T1 | 后端和前端基础验证 | 通过 |
| [x] | T2 | 构建验证 | 通过 |
| [x] | T3 | Docker 节点烟测 | 通过 |
| [ ] | T4 | 阶段级发布验收 | 等待 R0-5 授权 |

## 验证结果

| 检查项 | 结果 | 备注 |
| --- | --- | --- |
| `git diff --check` | Pass | 当前改动无空白错误 |
| 后端 Maven 多模块测试 | Pass | 使用临时 Maven settings 绕过不可达 mirror 后通过 |
| 前端 `npm run build` | Pass | 存在 Vite / Rollup 非阻塞 warning |
| `docker compose -f deploy/docker-compose.yml config` | Pass | Compose 配置可解析 |
| Docker Compose 重建启动 | Pass | `up -d --build` 已完成 |
| 容器健康检查 | Pass | `mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui` 均 healthy |
| Obsidian 初始化 | Pass | `POST /api/v1/obsidian/init` 成功 |
| Source Note 端到端烟测 | Pass | 导入、草案、写入、预览均通过 |
| 浏览器检查 | Pass | `http://localhost:3000` 可见 MVP2 / Vault / Source Note，console error 为空 |
| ignored artifacts 检查 | Pass | 本地存在编译产物和运行数据，但已被 `.gitignore` 忽略 |
| tracked forbidden paths 检查 | Pass | `git ls-files --error-unmatch` 未发现禁止路径被跟踪 |

端到端烟测记录：

```text
JobUid: job_20260523_0ae896c3e383
JobStatus: completed
SourceFileUid: file_e6a481e4083449579246366252a410a5
SourceUid: src_f916b6b2d202461a8d49bfa721532290
NoteUid: note_20260523_1cf541982149
Host note path: E:\WikiForgeVault\00_Inbox_收集箱\Sources_来源\roadmap-source-note.md-src_f916b6b2d202461a8d49bfa721532290.md
Preview contains title: true
```

## Git 卫生检查

发布前需要保持以下内容不进入 Git：

- `node_modules/`
- `frontend/dist/`
- `frontend/.vite/`
- `target/`
- `.env`
- `logs/`
- `data/`
- `WikiForge_RawSources`
- `WikiForge_Vault`
- `E:\WikiForgeVault`

本轮检查结论：

- 当前 `git status --short` 未显示 `node_modules`、`dist`、`.vite`、`target`、真实 `.env`、`data`、Vault 或 Raw Sources 内容。
- 广义模式检查会命中 `.env.example`，这是允许提交的配置模板，不等同于本地 `.env`。
- 本地确实存在被忽略目录：`frontend/node_modules`、`frontend/dist`、`backend/target`、`backend/wikiforge-common/target`、`backend/wikiforge-core-service/target`、`backend/wikiforge-worker-service/target`、`data`。
- `git check-ignore -v` 显示上述目录和 `.env` 已由 `.gitignore` 规则忽略，不能使用 `git add -f` 强制提交。
- `git ls-files --error-unmatch` 对上述禁止路径返回 `No forbidden paths tracked`。
- docs 目录存在文件移动产生的 delete + add，属于此前目录整理和归档规则调整范围。

## 风险与边界

- 当前清单是本地发布前验证，GitHub Actions CI 是否通过需要在推送后以远程 CI 结果为准。
- Flyway 已覆盖 fresh DB 建表路径；从 0.03 升级到 0.04 的核心风险在 `V20260523_003__create_obsidian_notes.sql`，当前通过迁移 SQL 兼容性测试和 Docker MySQL 重建验证。
- Obsidian 打开能力依赖用户本机已注册 `obsidian://open` 协议。
- 本轮验证确认服务端生成 `obsidian://open` URI；真实唤起 Obsidian 客户端属于本机协议注册行为，不纳入 Docker 自动验收。
- PowerShell JSON 输出中中文路径可能显示为乱码，但宿主机 Vault 实际目录和文件名正常。
- Source Note 当前以元数据和占位段落为主，正文摘录、AI 摘要和标签属于后续 MVP3 / MVP4。
- 已写 Note 的状态展示、重复写入策略和 Vault 状态面板属于 MVP2.1。
- `WIKIFORGE_INTERNAL_API_TOKEN=change-me` 仅适合本地开发，正式部署需要覆盖为真实 token。

## 下一步

- [ ] 用户复核当前 `0.04` 发布范围。
- [ ] 用户授权后执行 R0-5：提交、创建标签 `0.04`、推送远程。
- [ ] 进入 R1：MVP2.1 可用性加固。
