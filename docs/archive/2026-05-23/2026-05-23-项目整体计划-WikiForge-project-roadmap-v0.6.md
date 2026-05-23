# 2026-05-23 WikiForge 项目整体计划 Project Roadmap

## 版本信息

- 文档版本：v0.6
- 当前分支：`codex/mvp2.1-usability-hardening`
- 当前工程阶段：MVP2.1 可用性加固已实现并验证；版本标签和 GitHub Release 由用户定义但不阻塞开发；下一内部开发节点为 MVP3 文档解析
- 当前产品主线：先把杂乱资料收集、整理、归档，再逐步进入 AI 提炼、MCP、个人记录和向量库

## 阅读规则

新参与的 AI 或开发者先读：

1. `AGENTS.md`
2. `docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-v2.4.md`
3. 本文档
4. 当前执行节点对应的计划文档或 Work Order

## Agent 进度识别规则 Progress Marking

为便于后续 Agent 快速识别当前进展，本文档同时使用复选框和单选框：

- 复选框 `- [x]` 表示阶段或节点已完成。
- 复选框 `- [ ]` 表示阶段或节点未完成。
- 单选框 `(x)` 表示当前唯一执行位置。
- 单选框 `( )` 表示候选但非当前执行位置。
- 完成任一阶段或节点后，必须同步更新本文档、对应自检清单、开发者日志和归档索引。

当前执行指针：

- ( ) S0 需求与架构基线
- ( ) S1 MVP0 工程骨架
- ( ) S2 MVP1 源文件归集
- ( ) S3 / R0 MVP2 Obsidian Source Note 内部开发基线
- ( ) S4 / R1 MVP2.1 可用性加固
- (x) S5 / R2 MVP3 文档解析
- ( ) S6 / R3 MVP4 AI 辅助整理与审核
- ( ) S7 / R4 MVP5 轻量 MCP
- ( ) S8 / R5 V1 在线资料与个人记录
- ( ) S9 / R6 V2 知识运行层

## 状态标记 Status

| 状态 | 含义 |
| --- | --- |
| Done | 已完成并验证 |
| In Progress | 当前正在执行 |
| Next | 下一批应执行 |
| Later | 后续阶段执行 |
| Blocked | 受外部条件阻塞 |
| External Pending | 对外发布或宣导待用户定义，不阻塞内部开发 |

## 总体路线

WikiForge 采用“先整理，再提炼，再运行”的节奏。

```text
需求与架构基线
  -> MVP0 工程骨架
  -> MVP1 本地源文件归集
  -> MVP2 Obsidian Source Note
  -> MVP2.1 发布收束与可用性加固
  -> MVP3 文档解析与 Source Note 丰富化
  -> MVP4 AI 辅助整理与审核
  -> MVP5 轻量 MCP 预览
  -> V1 在线资料与个人记录
  -> V2 向量库与知识运行层
```

## 阶段一览

| 完成 | 阶段 | 状态 | 目标 | 版本建议 | 验收重点 |
| --- | --- | --- | --- | --- | --- |
| [x] | S0 需求与架构基线 | Done | 明确产品定位、技术栈、服务边界、文档规则 | 0.01 | 需求、架构、数据模型、开发者日志完成 |
| [x] | S1 MVP0 工程骨架 | Done | 建立 Core / Worker / UI / MySQL / Docker / CI 基线 | 0.02 | 后端测试、前端构建、Compose config |
| [x] | S2 MVP1 源文件归集 | Done | 指定路径扫描、复制 Raw Sources、去重、任务状态、UI 列表 | 0.03 | Docker 端到端导入任务完成 |
| [x] | S3 MVP2 Obsidian Source Note | Done | Source File 生成 Source Note、写入 Vault、预览、打开 Obsidian | 0.04 | Docker 重建、Vault 写入、UI 可操作 |
| [x] | S4 MVP2.1 可用性加固 | Done | 让 MVP2 更像日常可用工具 | 0.05 | 可查看已写 Note、错误可见、重复写入策略清晰 |
| [ ] | S5 MVP3 文档解析 | Next | 从文件元数据走向正文内容抽取 | 0.06 | Markdown / TXT / PDF / Word 样例可抽取 |
| [ ] | S6 MVP4 AI 辅助整理 | Later | 接入国内模型，生成摘要、标签、分类建议和审核队列 | 0.07 | 人工审核后写入 Obsidian |
| [ ] | S7 MVP5 轻量 MCP | Later | 让 OpenClaw / Hermes / 外部 Agent 能调用 WikiForge | 0.08 | MCP 工具可创建记录、查询 Source、读取 Note |
| [ ] | S8 V1 在线资料与个人记录 | Later | 飞书/腾讯文档、邮件、账单、消费、人际关系等记录接入 | 1.0 | 多源记录进入统一整理系统 |
| [ ] | S9 V2 知识运行层 | Later | 向量库、混合检索、维护 Agent、办公室视图 | 2.0 | 知识可被 Agent 长期调用和维护 |

## 递进测试门禁 Progressive Test Gates

测试按阶段和节点递进，不按每个小功能碎片重复跑完整链路。每个 Work Order 先声明本节点需要到哪一层门禁，完成后再更新勾选状态。

当前测试层级单选：

- ( ) T0 文档与 Git 卫生：只改文档、规则、计划时使用。
- ( ) T1 契约与单元测试：改 DTO、Service、Mapper、基础逻辑时使用。
- (x) T2 构建验证：完成一个可构建代码切片时使用。
- ( ) T3 Docker 节点烟测：完成跨服务节点或发布候选时使用。
- ( ) T4 阶段级端到端验收：完成一个 MVP 阶段时使用。

递进顺序：

- [x] T0：`git diff --check`，确认文档、空白和 Git 卫生。
- [x] T1：后端 Maven 测试、前端类型/构建相关检查，按改动范围执行。
- [x] T2：后端/前端构建通过。
- [x] T3：Docker Compose config、重建、健康检查、节点级 API 烟测。
- [ ] T4：用户授权发布前，按版本候选执行阶段级复核、提交、标签和推送。

执行规则：

- 文档-only 节点只需 T0。
- 单服务代码节点至少到 T1。
- 前后端联动节点至少到 T2。
- 涉及 Core / Worker / UI / Docker 的节点至少到 T3。
- 一个 MVP 阶段发布前才执行 T4，不为每个小功能重复做阶段级全量验收。

## 当前执行主线

### R0：MVP2 收束与发布准备

目标：把当前已经实现的 MVP2 从“代码完成”收束成“可复核、可发布、可继续开发”的稳定节点。

当前 R0 节点指针：

- ( ) R0-1 项目整体路线图
- ( ) R0-2 版本更新记录
- ( ) R0-3 Docker 端到端烟测
- ( ) R0-4 发布前自检清单
- ( ) R0-5 提交推送
- ( ) R0-6 版本标签和 GitHub Release 确认

| 节点 | 状态 | 事项 | 输出 | 验证 |
| --- | --- | --- | --- | --- |
| R0-1 | Done | 输出项目整体路线图，并加入 docs 入口和归档 | 本文档、归档快照、docs README 链接 | `git diff --check` |
| R0-2 | Done | 补充版本更新记录 0.04 草案 | `docs/current/2026-05-23-版本更新记录-WikiForge-release-notes.md` | 人工可读，边界清晰 |
| R0-3 | Done | 用 Docker 栈跑一次真实 MVP2 Source Note 端到端烟测 | 创建导入任务、写入 Source Note、预览 Markdown | Core / Worker / UI healthy，API 成功 |
| R0-4 | Done | 输出发布前自检清单 | `docs/current/2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist.md` | `git status --short` 未发现编译产物和本地运行数据 |
| R0-5 | Done | 提交并推送当前分支 | commit `a987780`，push `origin/codex/mvp2-obsidian-source-note` | 提交前 Git 卫生检查通过，远程分支已创建 |
| R0-6 | External Pending | 版本标签、GitHub Release 和正式发布定义 | tag/release 暂不执行 | 等待用户确认，不阻塞 R1 开发 |

### R1：MVP2.1 可用性加固

目标：让 MVP2 从“能写入”变成“好使用、好检查、可重复操作”。

当前 R1 节点指针：

- ( ) R1-0 契约冻结
- ( ) R1-1 已写 Note 状态与读取已有 Note
- ( ) R1-2 Vault 状态面板
- ( ) R1-3 重复写入策略
- ( ) R1-4 MVP2.1 回归验证记录
- ( ) R1-5 文档归档和提交推送

| 节点 | 状态 | 事项 | 主要文件 | 验收 |
| --- | --- | --- | --- | --- |
| R1-1 | Done | Source Files 表格展示是否已有 Obsidian Note | Core API、UI 表格 | 已写入 Note 的文件有可见状态 |
| R1-2 | Done | 支持读取已存在 Note，不必重复写入 | Core Service、UI API | 点击 Source Note 可打开已有预览 |
| R1-3 | Done | 明确重复写入默认策略：已有则预览，不自动重复写入 | PRD、Service、UI | 系统有稳定默认策略 |
| R1-4 | Done | Vault 状态面板：路径、目录、最近写入、错误提示 | UI Dashboard | 用户能判断 Vault 是否可用 |
| R1-5 | Done | 增加集成测试和回归验证记录 | backend tests / docs | 一条命令可验证 MVP2.1 关键链路 |

### R2：MVP3 文档解析与 Source Note 丰富化

目标：从“文件元数据归档”升级到“能读取正文并生成更有用的 Source Note”。

| 节点 | 状态 | 事项 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- |
| R2-1 | Next | 冻结 `source_contents` 或等效正文存储契约 | 数据模型、Flyway | 文本内容、分块、hash、解析状态有表结构 |
| R2-2 | Next | Worker 支持 Markdown / TXT 解析 | Worker Service | `.md/.txt` 正文可入库 |
| R2-3 | Next | Worker 支持 PDF 基础文本抽取 | Worker Service | 样例 PDF 可抽取文本或返回明确失败原因 |
| R2-4 | Next | Worker 支持 Word 基础文本抽取 | Worker Service | `.docx` 样例可抽取文本 |
| R2-5 | Next | Source Note 模板加入正文摘录和来源片段 | Core Service、UI | Markdown 不只包含元数据 |

### R3：MVP4 AI 辅助整理与审核

目标：让系统开始“提炼”，但仍保留人工审核。

| 节点 | 状态 | 事项 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- |
| R3-1 | Later | 冻结模型调用契约，支持 DeepSeek / MiniMax / CC Switch 切换 | Core / Agent 设计 | 配置可切换 provider |
| R3-2 | Later | 新增 `agent_runs`、`agent_steps`、`review_items` 最小表 | 数据模型、Flyway | 每次 AI 输出有运行账本 |
| R3-3 | Later | 生成摘要、标签、分类建议、风险标记 | Agent / Core | AI 输出结构化保存 |
| R3-4 | Later | Web UI 增加审核队列 | UI | 用户可批准、拒绝、修改 |
| R3-5 | Later | 审核通过后更新 Obsidian Source Note | Core Service | 人工确认后才改写知识层 |

### R4：MVP5 轻量 MCP 预览

目标：让 OpenClaw / Hermes / 外部 Agent 能调用 WikiForge 做记录和查询。

| 节点 | 状态 | 事项 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- |
| R4-1 | Later | 设计 MCP 工具清单和权限边界 | 技术架构、数据模型 | 工具不越权，不直接暴露本地路径 |
| R4-2 | Later | 实现 `create_source` / `search_sources` / `get_source` | MCP Service | 外部客户端可调用 |
| R4-3 | Later | 实现 `get_obsidian_note` / `create_personal_record` | MCP Service | Agent 可读 Note、写个人记录 |
| R4-4 | Later | 记录 MCP 调用日志 | MySQL | 每次调用可追踪 |
| R4-5 | Later | 编写 OpenClaw / Hermes 接入说明 | docs | 本机可按说明接入 |

### R5：V1 在线资料与个人记录

目标：把 WikiForge 从“文件知识库”扩展为个人 LifeOS 底座。

| 节点 | 状态 | 事项 | 验收 |
| --- | --- | --- | --- |
| R5-1 | Later | 飞书文档链接读取 PoC | 输入飞书 URL 后可创建 Source |
| R5-2 | Later | 腾讯文档 / 网页收藏连接器设计 | 链接类 Source 有统一契约 |
| R5-3 | Later | 个人记录服务：消费、账单、邮件、人际关系、事件 | 结构化记录可入库 |
| R5-4 | Later | 个人记录 Obsidian 页面模板 | 可按日期、人物、主题沉淀 |
| R5-5 | Later | 周报、月报、关系复盘、消费总结 | 系统能持续总结自己 |

### R6：V2 知识运行层

目标：让沉淀后的知识进入长期运行和复用。

| 节点 | 状态 | 事项 | 验收 |
| --- | --- | --- | --- |
| R6-1 | Later | 向量库导出契约 | Source Note / chunk 可批量导出 |
| R6-2 | Later | Hybrid Search：MySQL 条件 + 向量 + rerank | 查询更稳定 |
| R6-3 | Later | Lint / Maintain Agent | 可发现重复、过期、孤立知识 |
| R6-4 | Later | 办公室视图 | Agent 状态和任务流可视化 |
| R6-5 | Later | 定时总结和长期记忆 | 知识可持续演进 |

## 近期三轮执行计划

### 第 1 轮：MVP2 发布收束

1. 完成 R0-1 路线图文档。Done
2. 完成 R0-2 版本更新记录 0.04 草案。Done
3. 完成 R0-3 Docker 端到端烟测。Done
4. 输出 R0-4 发布前自检清单。Done
5. 执行 R0-5 提交和推送。Done
6. R0-6 版本标签、GitHub Release 和正式发布定义由用户确认，作为外部发布宣导事项，不阻塞第 2 轮开发。

## 执行记录

### R0-3 Docker 端到端烟测

执行时间：2026-05-23

```text
JobUid: job_20260523_0ae896c3e383
JobStatus: completed
SourceFileUid: file_e6a481e4083449579246366252a410a5
SourceUid: src_f916b6b2d202461a8d49bfa721532290
NoteUid: note_20260523_1cf541982149
Host note path: E:\WikiForgeVault\00_Inbox_收集箱\Sources_来源\roadmap-source-note.md-src_f916b6b2d202461a8d49bfa721532290.md
Preview contains title: true
```

结论：MVP2 从导入任务到 Source Note 写入、预览的主链路通过。PowerShell JSON 输出中中文路径显示为乱码，但宿主机 Vault 内实际目录和文件名为正常中文。

### R0-4 发布前自检清单

执行时间：2026-05-23

输出文件：

```text
docs/current/2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist.md
```

自检结论：

- `git diff --check` 通过。
- Docker Compose 当前 `mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui` 均 healthy。
- R0-3 烟测写入的 Source Note 在 `E:\WikiForgeVault` 中存在。
- 当前 `git status --short` 未发现 `node_modules`、`dist`、`.vite`、`target`、真实 `.env`、`data`、Vault 或 Raw Sources 内容。
- 广义 `.env` 模式会命中 `.env.example`，这是允许提交的配置模板。
- `git check-ignore` 确认本地 `frontend/node_modules`、`frontend/dist`、`backend/**/target`、`data` 和 `.env` 已被 `.gitignore` 忽略。
- `git ls-files --error-unmatch` 确认上述禁止路径未被 Git 跟踪。

结论：R0-4 完成。R0-5 提交和推送已完成；标签 `0.04`、GitHub Release 和正式发布定义仍需用户确认。

### R0-5 提交推送

执行时间：2026-05-23

结果：

```text
Commit: a987780 feat: add obsidian source note mvp2
Remote branch: origin/codex/mvp2-obsidian-source-note
Pull request URL: https://github.com/BonusWang/WikiForge/pull/new/codex/mvp2-obsidian-source-note
```

说明：

- 本次只推送开发分支。
- 未创建标签 `0.04`。
- 未创建 GitHub Release。
- 版本发布定义等待用户确认，但不阻塞后续开发。

### 第 2 轮：MVP2.1 可用性加固

当前分支：`codex/mvp2.1-usability-hardening`

执行规则：版本标签和 GitHub Release 属于对外宣导，由用户确认；内部开发分支、提交推送和下一工作流递进不等待该外部发布事项。

1. 冻结“已写 Note 状态”和“重复写入策略”契约。
2. 后端补充按 `fileUid` 查询最近 Note。
3. 前端展示已写入状态和打开/预览入口。
4. 增加集成测试和回归验证记录。
5. 更新文档和归档。

本轮已完成：

- 新增 Vault 状态查询 API 和 Dashboard 状态面板。
- Source Files 列表展示 Obsidian Note 写入状态。
- 已写入文件默认预览已有 Note，不自动重复写入。
- 后端集成测试覆盖 Vault 状态、最近 Note 查询和列表状态字段。
- 前端构建通过，后端 Maven 测试通过。

### 第 3 轮：MVP3 文档解析

1. 冻结正文存储表。
2. 先实现 Markdown / TXT。
3. 再实现 PDF / Word。
4. Source Note 增加正文摘录。
5. 再考虑 AI 摘要，不提前耦合。

## 并行开发规则

- 进入 R1 及以后，每个开发切片都要先创建具体 Work Order。
- 涉及 DDL、共享 DTO、错误码、Docker Compose、CI 的任务串行处理。
- Core、Worker、UI 可在契约冻结后并行。
- 主编排 Agent 负责最终集成、验证、文档和归档。

## 当前不可提前做的事

- 不在 MVP2.1 前引入大模型总结。
- 不在文档解析前引入向量库。
- 不在 MCP 工具清单冻结前实现 MCP 服务。
- 不在个人记录契约冻结前写账单、邮件、人际关系表。
- 不把 `E:\WikiForgeVault`、Raw Sources、运行数据或 `.env` 提交到 Git。

## 发布与版本建议

| 标签 | 内容 |
| --- | --- |
| `0.04` | MVP2 Obsidian Source Note 闭环 |
| `0.05` | MVP2.1 可用性加固 |
| `0.06` | MVP3 文档解析 |
| `0.07` | MVP4 AI 辅助整理 |
| `0.08` | MVP5 轻量 MCP |
| `1.0` | 在线资料 + 个人记录可用版 |
| `2.0` | 向量库 + 知识运行层 |
