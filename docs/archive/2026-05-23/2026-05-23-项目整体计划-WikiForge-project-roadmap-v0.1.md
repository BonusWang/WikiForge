# 2026-05-23 WikiForge 项目整体计划 Project Roadmap

## 版本信息

- 文档版本：v0.1
- 当前分支：`codex/mvp2-obsidian-source-note`
- 当前工程阶段：MVP2 已实现，本地验证通过，待发布收束
- 当前产品主线：先把杂乱资料收集、整理、归档，再逐步进入 AI 提炼、MCP、个人记录和向量库

## 阅读规则

新参与的 AI 或开发者先读：

1. `AGENTS.md`
2. `docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-v1.9.md`
3. 本文档
4. 当前执行节点对应的计划文档或 Work Order

## 状态标记

| 状态 | 含义 |
| --- | --- |
| Done | 已完成并验证 |
| In Progress | 当前正在执行 |
| Next | 下一批应执行 |
| Later | 后续阶段执行 |
| Blocked | 受外部条件阻塞 |

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

| 阶段 | 状态 | 目标 | 版本建议 | 验收重点 |
| --- | --- | --- | --- | --- |
| S0 需求与架构基线 | Done | 明确产品定位、技术栈、服务边界、文档规则 | 0.01 | 需求、架构、数据模型、开发者日志完成 |
| S1 MVP0 工程骨架 | Done | 建立 Core / Worker / UI / MySQL / Docker / CI 基线 | 0.02 | 后端测试、前端构建、Compose config |
| S2 MVP1 源文件归集 | Done | 指定路径扫描、复制 Raw Sources、去重、任务状态、UI 列表 | 0.03 | Docker 端到端导入任务完成 |
| S3 MVP2 Obsidian Source Note | In Progress | Source File 生成 Source Note、写入 Vault、预览、打开 Obsidian | 0.04 | Docker 重建、Vault 写入、UI 可操作 |
| S4 MVP2.1 可用性加固 | Next | 让 MVP2 更像日常可用工具 | 0.05 | 可查看已写 Note、错误可见、重复写入策略清晰 |
| S5 MVP3 文档解析 | Next | 从文件元数据走向正文内容抽取 | 0.06 | Markdown / TXT / PDF / Word 样例可抽取 |
| S6 MVP4 AI 辅助整理 | Later | 接入国内模型，生成摘要、标签、分类建议和审核队列 | 0.07 | 人工审核后写入 Obsidian |
| S7 MVP5 轻量 MCP | Later | 让 OpenClaw / Hermes / 外部 Agent 能调用 WikiForge | 0.08 | MCP 工具可创建记录、查询 Source、读取 Note |
| S8 V1 在线资料与个人记录 | Later | 飞书/腾讯文档、邮件、账单、消费、人际关系等记录接入 | 1.0 | 多源记录进入统一整理系统 |
| S9 V2 知识运行层 | Later | 向量库、混合检索、维护 Agent、办公室视图 | 2.0 | 知识可被 Agent 长期调用和维护 |

## 当前执行主线

### R0：MVP2 收束与发布准备

目标：把当前已经实现的 MVP2 从“代码完成”收束成“可复核、可发布、可继续开发”的稳定节点。

| 节点 | 状态 | 事项 | 输出 | 验证 |
| --- | --- | --- | --- | --- |
| R0-1 | In Progress | 输出项目整体路线图，并加入 docs 入口和归档 | 本文档、归档快照、docs README 链接 | `git diff --check` |
| R0-2 | Next | 补充版本更新记录 0.04 草案 | `docs/current/2026-05-23-版本更新记录-WikiForge-release-notes.md` | 人工可读，边界清晰 |
| R0-3 | Next | 用 Docker 栈跑一次真实 MVP2 Source Note 端到端烟测 | 创建导入任务、写入 Source Note、预览 Markdown | Core / Worker / UI healthy，API 成功 |
| R0-4 | Next | 输出发布前自检清单 | 文件变更、验证结果、风险项 | `git status --short` 无编译产物 |
| R0-5 | Blocked | 合并、提交、打标签、推送 | commit、tag `0.04` | 需要用户明确授权 |

### R1：MVP2.1 可用性加固

目标：让 MVP2 从“能写入”变成“好使用、好检查、可重复操作”。

| 节点 | 状态 | 事项 | 主要文件 | 验收 |
| --- | --- | --- | --- | --- |
| R1-1 | Next | Source Files 表格展示是否已有 Obsidian Note | Core API、UI 表格 | 已写入 Note 的文件有可见状态 |
| R1-2 | Next | 支持读取已存在 Note，不必重复写入 | Core Service、UI API | 点击 Source Note 可打开已有预览 |
| R1-3 | Next | 明确重复写入策略：覆盖、另存、版本化三选一 | PRD、Service、UI | 用户能选择或系统有稳定默认 |
| R1-4 | Next | Vault 状态面板：路径、目录、最近写入、错误提示 | UI Dashboard | 用户能判断 Vault 是否可用 |
| R1-5 | Next | 增加端到端测试脚本或集成测试用例 | backend tests / docs | 一条命令可验证 MVP2 主链路 |

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

1. 完成 R0-1 路线图文档。
2. 完成 R0-2 版本更新记录 0.04 草案。
3. 完成 R0-3 Docker 端到端烟测。
4. 输出 R0-4 发布前自检清单。
5. 等用户确认后再执行 R0-5 提交、标签和推送。

### 第 2 轮：MVP2.1 可用性加固

1. 冻结“已写 Note 状态”和“重复写入策略”契约。
2. 后端补充按 `fileUid` 查询最近 Note。
3. 前端展示已写入状态和打开/预览入口。
4. 增加端到端测试。
5. 更新文档和归档。

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
