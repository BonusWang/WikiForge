# 2026-05-23 WikiForge 文档归档索引 v3.3

## 版本索引 Version Index

- 最新版本：v3.3
- 最新小节：`当前阶段结论`
- 推荐阅读：新 AI 开始工作时，先读本索引、命名规则和当前阶段结论；再按任务读取对应最新文件。
- 历史版本：v0.1-v0.9 仅在追溯需求演进、架构评审或归档规则变化时阅读。

## 归档目的

本目录用于保存 WikiForge 在 2026-05-23 的需求基线、技术方案、开发者日志和参考资料快照。同一天产生的归档文件统一放在本日期目录下。

主文档会继续迭代；当天最新归档文件可按滚动版本规则追加记录，已沉淀的旧版本文件不再直接修改，用于追踪完整需求演进过程。

## 命名规则

归档文件采用：

```text
日期-中文名-EnglishName-版本号.md
```

示例：

```text
2026-05-23-需求文档-knowledge-base-prd-v0.2.md
```

`README.md` 作为项目入口文件保留英文通用命名；归档快照可继续使用 `YYYY-MM-DD-README-vX.Y.md`。

如果同一类文档存在多个版本，优先阅读版本号最大的文件。

从 v0.9 开始，同一天同一类文档采用滚动版本规则：优先更新当天最新版本文件，并在文件内部追加版本记录；文件名版本号按 `v0.1 -> ... -> v0.9 -> v1.0` 递增，避免同一天产生过多重复快照。

从 v1.0 开始，长归档文件采用“版本索引 / Version Index”：AI 默认只读版本索引和最新版本小节，只有需要追溯历史时再读取旧版本内容。

## 本次归档文件

| 文件 | 版本 | 说明 |
| --- | --- | --- |
| `2026-05-23-需求文档-knowledge-base-prd-v0.2.md` | v0.2 | 当前 PRD 需求基线 |
| `2026-05-23-技术架构-technical-architecture-v0.2.md` | v0.2 | 当前技术架构基线 |
| `2026-05-23-技术架构-technical-architecture-v0.3.md` | v0.3 | 补充 CI/CD、Docker 打包和部署架构后的技术架构 |
| `2026-05-23-技术架构-technical-architecture-v0.4.md` | v0.4 | 补充少服务微服务架构、Core/Worker 服务边界和 AI 开发 Skill 分层后的技术架构 |
| `2026-05-23-技术架构-technical-architecture-v0.5.md` | v0.5 | 补充服务拆分落地后的 Core/Worker/UI Docker 与 CI 说明 |
| `2026-05-23-技术架构-technical-architecture-v0.6.md` | v0.6 | 补充 Obsidian Vault 宿主机映射变量和用户本机 Vault 路径 |
| `2026-05-23-技术架构-technical-architecture-v0.7.md` | v0.7 | 补充 MVP2 Obsidian Source Note 已实现 API、Vault 写入、安全预览和 UI 抽屉 |
| `2026-05-23-技术架构-technical-architecture-v0.8.md` | v0.8 | 补充 MVP3 文档解析依赖版本：Apache PDFBox 3.0.7、Apache POI 5.5.1 |
| `2026-05-23-技术架构-technical-architecture-v0.9.md` | v0.9 | 补充 MVP4 MiniMax OpenAI-compatible 接入、环境变量密钥边界、AI 审核 API 和 Dashboard 审核队列 |
| `2026-05-23-技术架构-technical-architecture-v1.0.md` | v1.0 | 补充通用 AI Provider 配置约定，支持 MiniMax、DeepSeek、CC Switch 等 OpenAI-compatible Provider 切换 |
| `2026-05-23-技术架构-technical-architecture-v1.1.md` | v1.1 | 补充 R3-5 审核通过写入 Obsidian Source Note API 和 UI 操作入口 |
| `2026-05-23-技术架构-technical-architecture-v1.2.md` | v1.2 | 补充 WikiForge Orchestration Service / UI 辅助工程、端口、API 和 Docker / CI 边界 |
| `2026-05-23-技术架构-technical-architecture-v1.3.md` | v1.3 | 补充 R4-2 MCP HTTP Preview 冻结 API、首批工具和权限边界 |
| `2026-05-23-数据模型-data-model-v0.2.md` | v0.2 | 当前数据模型基线 |
| `2026-05-23-数据模型-data-model-v0.3.md` | v0.3 | 架构评审后收敛 MVP DDL、状态枚举和索引设计的数据模型 |
| `2026-05-23-数据模型-data-model-v0.4.md` | v0.4 | 补充少服务微服务下的表归属和 Core/Worker 数据边界 |
| `2026-05-23-数据模型-data-model-v0.5.md` | v0.5 | 对齐 MVP2 实现后的 `obsidian_notes` 字段、类型和前缀索引 |
| `2026-05-23-数据模型-data-model-v0.6.md` | v0.6 | 补充 MVP3 `source_contents` 正文内容表、服务归属、字段和索引 |
| `2026-05-23-数据模型-data-model-v0.7.md` | v0.7 | 补充 Markdown / TXT / PDF / Word 均进入 `source_contents` 基础正文抽取 |
| `2026-05-23-数据模型-data-model-v0.8.md` | v0.8 | 补充 MVP4 `agent_runs`、`agent_steps`、`review_items` 实际落地字段和 Core Service 归属 |
| `2026-05-23-数据模型-data-model-v0.9.md` | v0.9 | 补充 MVP5 `mcp_tool_calls`、`personal_records` 最小表和敏感日志脱敏边界 |
| `2026-05-23-归档索引-archive-index-v0.2.md` | v0.2 | 补充外部 AI 架构评审目录和最终结论后的归档索引 |
| `2026-05-23-归档索引-archive-index-v0.3.md` | v0.3 | 补充 aruis/codex-cookbook 参考后的归档索引 |
| `2026-05-23-归档索引-archive-index-v0.4.md` | v0.4 | 补充 MVP 0 工程骨架启动和验证结果后的归档索引 |
| `2026-05-23-归档索引-archive-index-v0.5.md` | v0.5 | 补充少服务微服务选择和 WikiForge AI 开发 Skill 后的归档索引 |
| `2026-05-23-归档索引-archive-index-v0.6.md` | v0.6 | 补充多人协作角色 Prompt、Work Order 模板和 Skill v0.2 后的归档索引 |
| `2026-05-23-归档索引-archive-index-v0.7.md` | v0.7 | 补充 Git 提交规则，明确 node_modules、dist、target 等编译产物不提交 |
| `2026-05-23-归档索引-archive-index-v1.6.md` | v1.6 | 补充版本 0.03 发布说明、main 合并准备和 MVP1 源文件归集整理闭环版本边界 |
| `2026-05-23-归档索引-archive-index-v1.7.md` | v1.7 | 补充 CodeGraph 代码知识图谱技术参考、V2+ 后续路线和归档快照 |
| `2026-05-23-归档索引-archive-index-v1.8.md` | v1.8 | 补充 docs 目录整理、Obsidian Vault 本机路径、MVP2 Source Note 开发计划和 init 自检结果 |
| `2026-05-23-归档索引-archive-index-v1.9.md` | v1.9 | 补充 MVP2 Obsidian Source Note 后端、前端、Docker 和浏览器验证结果 |
| `2026-05-23-归档索引-archive-index-v2.0.md` | v2.0 | 补充项目整体路线图、0.04 发布草案和 R0-3 端到端烟测结果 |
| `2026-05-23-归档索引-archive-index-v2.1.md` | v2.1 | 补充复选框/单选框进度识别规则、递进测试门禁和 R0-4 发布前自检清单 |
| `2026-05-23-归档索引-archive-index-v2.2.md` | v2.2 | 补充提交推送自动授权规则，以及版本标签和 GitHub Release 需用户确认的边界 |
| `2026-05-23-归档索引-archive-index-v2.3.md` | v2.3 | 补充 R0-5 提交推送完成，远程分支已创建，R0-6 版本发布确认待用户处理 |
| `2026-05-23-归档索引-archive-index-v2.4.md` | v2.4 | 补充发布边界修正、MVP2.1 可用性加固完成、文档一致性自检、开发者日志日期修正、中文+EnglishName 规则和约定文件名例外 |
| `2026-05-23-归档索引-archive-index-v2.5.md` | v2.5 | 补充 MVP3 文档解析首轮完成、source_contents、Markdown/TXT 解析、Source Note 正文摘录和下一轮 PDF/Word 节点 |
| `2026-05-23-归档索引-archive-index-v2.6.md` | v2.6 | 补充 MVP3 PDF / Word 基础解析完成、依赖版本、测试结果和下一节点进入 MVP4 |
| `2026-05-23-归档索引-archive-index-v2.7.md` | v2.7 | 补充 MVP4 AI 辅助整理与审核首轮完成、MiniMax 环境变量适配、审核队列 UI 和下一节点 R3-5 |
| `2026-05-23-归档索引-archive-index-v2.8.md` | v2.8 | 补充 AI Provider 配置化、通用环境变量命名、Dashboard Provider 配置行和验证结果 |
| `2026-05-23-归档索引-archive-index-v2.9.md` | v2.9 | 补充 R3-5 审核通过写入 Obsidian Source Note、前后端并行开发结果和下一节点 MVP5 MCP |
| `2026-05-23-归档索引-archive-index-v3.0.md` | v3.0 | 补充 R4-0 Symphony-inspired 工作模式升级、WORKFLOW、Issue 任务卡和 Skill 更新 |
| `2026-05-23-归档索引-archive-index-v3.1.md` | v3.1 | 补充 R4-1 WikiForge Orchestration 辅助工程骨架、独立服务、独立 UI、Docker / CI 和验证结果 |
| `2026-05-23-归档索引-archive-index-v3.2.md` | v3.2 | 补充 R4-1 后置自检、执行指针说明和副手 Agent 启动准备 |
| `2026-05-23-归档索引-archive-index-v3.3.md` | v3.3 | 补充 R4-2 MCP 契约冻结、首批工具、权限边界、日志脱敏和下一节点 R4-3 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.1.md` | v0.1 | MVP 实施计划 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.2.md` | v0.2 | 架构评审后补充 MVP 0、CI/CD、Docker 和分阶段执行规则的实施计划 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.3.md` | v0.3 | 补充 MVP 0 少服务微服务骨架、Core/Worker 拆分和新开发顺序 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.4.md` | v0.4 | 补充 V2+ 代码知识图谱与代码类资料治理后续规划 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.5.md` | v0.5 | 补充 MVP2 Obsidian Source Note 使用本机 Vault 路径的实施约束 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.6.md` | v0.6 | 补充 MVP2 已实现状态、验证结果和 index/log 顺延边界 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.7.md` | v0.7 | 修正 current 日期前缀为 2026-05-23，并补充双语标题 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.8.md` | v0.8 | 将 Source Note 模板示例日期改为占位符，避免 current 文档出现过期具体日期 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.9.md` | v0.9 | 对齐当前实际路线：MVP3 文档解析、MVP4 AI 审核、MVP5 Orchestration + MCP |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v0.1.md` | v0.1 | 新增项目整体阶段路线图，明确 R0-R6 任务节点和近期三轮执行计划 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v0.2.md` | v0.2 | 补充 R0-1、R0-2、R0-3 执行状态和端到端烟测记录 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v0.3.md` | v0.3 | 补充 Agent 可读复选框、当前执行单选指针和 T0-T4 递进测试门禁 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v0.4.md` | v0.4 | 补充 R0-5 提交推送已获授权、版本标签和 GitHub Release 仍需用户确认 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v0.5.md` | v0.5 | 补充 R0-5 提交推送已完成，R0-6 版本标签和 GitHub Release 等待用户确认 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v0.6.md` | v0.6 | 修正 R0-6 外部发布边界，补充 MVP2.1 已写 Note 状态、Vault 状态面板、默认重复写入策略、文档一致性修正和下一内部开发节点 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v0.7.md` | v0.7 | 补充 MVP3 文档解析首轮完成，R2-1/R2-2/R2-5/R2-6 Done，下一轮进入 PDF/Word 解析 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v0.8.md` | v0.8 | 标记 MVP3 文档解析 Done，下一内部开发节点移动到 MVP4 AI 辅助整理与审核 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v0.9.md` | v0.9 | 标记 MVP4 R3-1 到 R3-4 Done，下一节点为 R3-5 审核通过后更新 Obsidian Source Note |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v1.0.md` | v1.0 | 补充可配置 AI Provider 适配和 Dashboard Provider / Model / Base URL 配置行 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v1.1.md` | v1.1 | 标记 MVP4 主闭环完成，下一内部开发节点移动到 MVP5 轻量 MCP |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v1.3.md` | v1.3 | 补充 R4-0 工作模式升级，下一节点进入 MVP5 MCP 工具和权限边界冻结 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v1.4.md` | v1.4 | 调整 MVP5 为 Orchestration 辅助工程 + MCP，标记 R4-1 完成并移动到 R4-2 |
| `2026-05-23-项目整体计划-WikiForge-project-roadmap-v1.5.md` | v1.5 | 标记 R4-2 MCP 契约冻结完成，当前指针移动到 R4-3 Source 类工具实现 |
| `2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist-v0.1.md` | v0.1 | MVP2 发布前自检清单，包含 R0 节点勾选状态、测试门禁和风险边界 |
| `2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist-v0.2.md` | v0.2 | 补充 R0-5 提交推送授权状态，以及标签和 GitHub Release 发布边界 |
| `2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist-v0.3.md` | v0.3 | 补充 R0-5 commit `a987780` 和远程分支推送结果 |
| `2026-05-23-MVP2发布前自检-WikiForge-mvp2-release-checklist-v0.4.md` | v0.4 | 明确标签和 GitHub Release 等待用户对外发布定义，但不阻塞 R1/R2 内部开发递进 |
| `2026-05-23-MVP0项目骨架-WikiForge-mvp0-project-skeleton-v0.1.md` | v0.1 | MVP 0 工程骨架开发规格、执行清单和验证结果 |
| `2026-05-23-MVP0服务拆分-WikiForge-mvp0-service-split-v0.1.md` | v0.1 | MVP0 后端拆分为 common/core/worker 的执行计划和 Work Order |
| `2026-05-23-微服务架构与AI开发Skill设计-WikiForge-microservice-ai-skill-design-v0.1.md` | v0.1 | 微服务 B 方案、CDP 范式吸收点、WikiForge AI 开发 Skill 和多人协作约定 |
| `2026-05-23-微服务架构与AI开发Skill设计-WikiForge-microservice-ai-skill-design-v0.2.md` | v0.2 | 补充 AI 角色 Prompt 和开发 Work Order 模板 |
| `2026-05-23-需求完整度自检-WikiForge-requirements-completeness-review-v0.1.md` | v0.1 | 需求完整度自检 |
| `2026-05-23-参考项目清单-WikiForge-reference-projects-v0.1.md` | v0.1 | 参考项目清单 |
| `2026-05-23-参考项目清单-WikiForge-reference-projects-v0.2.md` | v0.2 | 补充 aruis/codex-cookbook 开发实施方法参考 |
| `2026-05-23-参考项目清单-WikiForge-reference-projects-v0.3.md` | v0.3 | 补充 colbymchenry/codegraph 代码知识图谱和 MCP Agent 查询参考 |
| `2026-05-23-参考项目清单-WikiForge-reference-projects-v0.4.md` | v0.4 | 修正 current 日期前缀为 2026-05-23，并补充双语标题 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.1.md` | v0.1 | 开发者日志 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.2.md` | v0.2 | 补充外部 AI 架构评审归档和最终结论后的开发者日志 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.3.md` | v0.3 | 补充 aruis/codex-cookbook 开发实施参考后的开发者日志 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.4.md` | v0.4 | 补充 MVP 0 工程骨架启动、验证结果和环境记录后的开发者日志 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.5.md` | v0.5 | 补充少服务微服务架构选择、CDP AI 范式学习和 WikiForge AI 开发 Skill |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.6.md` | v0.6 | 补充多人协作角色 Prompt 和 Work Order 模板 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.7.md` | v0.7 | 补充 Git 提交规则，禁止提交 node_modules、dist、target、本地配置和运行数据 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v1.6.md` | v1.6 | 补充 MVP1 契约冻结、并行派工、QA 契约收口、Core-Worker 导入闭环、本地与 Docker 端到端烟测修复和版本 0.03 发布准备 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v1.7.md` | v1.7 | 补充 CodeGraph 技术参考纳入后续路线的判断和同步文档 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v1.8.md` | v1.8 | 补充 init 自检、docs 目录整理、Vault 路径配置和 MVP2 计划启动 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v1.9.md` | v1.9 | 补充 MVP2 Obsidian Source Note 全面开发、测试、Docker 重建和 UI 浏览器检查 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.0.md` | v2.0 | 补充项目整体计划、R0 执行启动和 R0-3 Docker 端到端烟测结果 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.1.md` | v2.1 | 补充 R0-4 发布前自检、复选框/单选框进度规则和递进测试门禁 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.2.md` | v2.2 | 补充日常提交推送已授权、提交信息要求和版本发布需用户确认 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.3.md` | v2.3 | 补充 R0-5 commit、远程分支、PR URL 和 R0-6 发布确认边界 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.4.md` | v2.4 | 补充发布边界修正、MVP2.1 可用性加固完成、文档一致性规则修正、约定文件名例外和 current 日期前缀修正 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.5.md` | v2.5 | 补充 MVP3 文档解析首轮完成、验证结果、下一轮 PDF/Word 计划和活跃文档日期占位符修正 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.6.md` | v2.6 | 补充 PDFBox / POI 基础解析完成、验证结果和 MVP4 下一节点 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.7.md` | v2.7 | 补充 MVP4 AI 辅助整理与审核首轮完成、敏感密钥处理边界、验证结果和下一节点 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.8.md` | v2.8 | 补充 AI Provider 配置化、非 MiniMax provider RED/GREEN 测试和前端配置入口 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v2.9.md` | v2.9 | 补充 R3-5 审核通过写入 Obsidian、并行 UI Worker 结果和验证记录 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v3.0.md` | v3.0 | 补充 R4-0 Symphony-inspired Agent 工作模式升级和 Skill / WORKFLOW 更新 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v3.1.md` | v3.1 | 补充 R4-1 WikiForge Orchestration 辅助工程骨架、验证结果和本地访问入口 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v3.2.md` | v3.2 | 补充 R4-1 后置自检、敏感信息检查、执行指针说明和副手 Agent 启动准备 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v3.3.md` | v3.3 | 补充 R4-2 MCP 契约冻结、服务归属、工具清单、日志脱敏和下一节点 |
| `2026-05-23-版本更新记录-WikiForge-release-notes-v0.2.md` | v0.2 | 版本 0.02 与 0.03 的更新内容、验证结果和版本边界 |
| `2026-05-23-版本更新记录-WikiForge-release-notes-v0.3.md` | v0.3 | 补充版本 0.04 MVP2 Obsidian Source Note 闭环发布说明草案 |
| `2026-05-23-版本更新记录-WikiForge-release-notes-v0.4.md` | v0.4 | 补充 0.04 端到端烟测 JobUid、NoteUid 和 Vault 文件路径 |
| `2026-05-23-版本更新记录-WikiForge-release-notes-v0.5.md` | v0.5 | 补充 0.04 发布前自检通过和 Git 卫生结论 |
| `2026-05-23-版本更新记录-WikiForge-release-notes-v0.6.md` | v0.6 | 补充双语标题一致性修正 |
| `2026-05-23-MVP1契约冻结并行工作单-WikiForge-mvp1-contract-freeze-parallel-work-order-v0.3.md` | v0.3 | MVP1 API、DTO、完整 DDL、状态枚举、路径安全、内部 API token、Core-Worker 派发闭环、去重归属、MySQL 兼容字段和随机 jobUid 样式 |
| `2026-05-23-MVP2-Obsidian-Source-Note-WikiForge-mvp2-obsidian-source-note-v0.1.md` | v0.1 | MVP2 Obsidian Source Note 开发计划，包含 API、DDL、Vault 写入、UI 预览和 Docker 验证 |
| `2026-05-23-MVP2-Obsidian-Source-Note-WikiForge-mvp2-obsidian-source-note-v0.2.md` | v0.2 | 补充 MVP2 实现状态、ApiResponse code 约定和 MySQL 前缀索引修正 |
| `2026-05-23-MVP2.1可用性加固-WikiForge-mvp2.1-usability-hardening-v0.1.md` | v0.1 | MVP2.1 可用性加固 Work Order，记录 API、UI、默认重复写入策略、验证门禁和文档一致性修正 |
| `2026-05-23-MVP3文档解析-WikiForge-mvp3-document-parsing-v0.2.md` | v0.2 | MVP3 文档解析 Work Order，记录 source_contents、Markdown/TXT 解析、Source Note 摘录、验证结果和后续 PDF/Word 节点 |
| `2026-05-23-MVP3文档解析-WikiForge-mvp3-document-parsing-v0.3.md` | v0.3 | MVP3 文档解析 Work Order，记录 PDF / Word 基础解析完成和全量后端测试通过 |
| `2026-05-23-MVP4-AI辅助整理审核-WikiForge-mvp4-ai-review-v0.2.md` | v0.2 | MVP4 AI 辅助整理审核 Work Order，记录账本、审核队列、MiniMax 环境变量适配、UI 和验证结果 |
| `2026-05-23-MVP4-AI辅助整理审核-WikiForge-mvp4-ai-review-v0.3.md` | v0.3 | MVP4 Work Order 补充通用 AI Provider 配置约定和后续多模型切换边界 |
| `2026-05-23-MVP4-AI辅助整理审核-WikiForge-mvp4-ai-review-v0.4.md` | v0.4 | 补充 R3-5 approve API、审核状态更新、Obsidian 写入和 UI 操作入口 |
| `2026-05-23-MVP5轻量MCP-WikiForge-mvp5-mcp-preview-v0.2.md` | v0.2 | MVP5 轻量 MCP 预览 Work Order，补充 R4-0 / R4-1 工具契约初稿 |
| `2026-05-23-MVP5轻量MCP-WikiForge-mvp5-mcp-preview-v0.3.md` | v0.3 | 调整 MCP 节点顺序，MCP 契约冻结后移到 Orchestration 辅助工程之后 |
| `2026-05-23-MVP5轻量MCP-WikiForge-mvp5-mcp-preview-v0.4.md` | v0.4 | 补充 `(x)` 仅表示当前执行指针，完成状态以节点清单 `[x]` 为准 |
| `2026-05-23-MVP5轻量MCP-WikiForge-mvp5-mcp-preview-v0.5.md` | v0.5 | 标记 R4-2 Done，冻结 MCP API、工具 schema、权限边界和并行 Work Order |
| `2026-05-23-MVP5编排辅助工程-WikiForge-orchestration-console-v0.2.md` | v0.2 | R4-1 Orchestration Service / UI 骨架实施计划、完成定义和验证记录 |
| `2026-05-23-MVP5编排辅助工程-WikiForge-orchestration-console-v0.3.md` | v0.3 | 补充 `(x)` 仅表示当前执行指针，避免后续 Agent 误读 |
| `2026-05-23-MVP5编排辅助工程-WikiForge-orchestration-console-v0.4.md` | v0.4 | 标记 R4-2 完成并移动到 R4-3 Source 类 MCP Preview 工具 |
| `2026-05-23-架构决策-DECISIONS-v0.1.md` | v0.1 | 架构决策记录 |
| `2026-05-23-架构决策-DECISIONS-v0.2.md` | v0.2 | 架构评审后的最终决策记录 |
| `2026-05-23-架构决策-DECISIONS-v0.3.md` | v0.3 | 补充少服务微服务选择和 AI 开发 Skill 决策 |
| `2026-05-23-架构决策-DECISIONS-v0.4.md` | v0.4 | 补充服务镜像命名收敛为 Core、Worker、UI |
| `2026-05-23-架构决策-DECISIONS-v0.5.md` | v0.5 | 补充双语标题一致性修正 |
| `2026-05-23-架构决策-DECISIONS-v0.6.md` | v0.6 | 将 current 架构决策标题日期统一为 2026-05-23 |
| `2026-05-23-架构决策-DECISIONS-v0.7.md` | v0.7 | 将 MVP3 决策收敛为 `source_contents`，Agent 账本顺延到 MVP4，MCP 顺延到 MVP5 |
| `2026-05-23-架构决策-DECISIONS-v0.8.md` | v0.8 | 确认 MVP4 模型调用契约、MiniMax 环境变量密钥边界、Agent 账本和人工审核后再改写知识层 |
| `2026-05-23-架构决策-DECISIONS-v0.9.md` | v0.9 | 补充 MVP5 MCP HTTP Preview API、工具 schema 和权限边界 |
| `2026-05-23-架构决策-DECISIONS-v1.0.md` | v1.0 | 补充 Orchestration 辅助工程决策，确认独立服务和独立 UI |
| `2026-05-23-架构决策-DECISIONS-v1.1.md` | v1.1 | 补充 R4-2 MCP 契约冻结决策，确认首批工具、migration 和 `mcp_servers` 延后 |
| `2026-05-23-AI开发规则-AGENTS-v0.1.md` | v0.1 | 开发 AI 前置规则 |
| `2026-05-23-AI开发规则-AGENTS-v0.2.md` | v0.2 | 补充“读取最高版本归档索引”的开发 AI 前置规则 |
| `2026-05-23-AI开发规则-AGENTS-v0.3.md` | v0.3 | 补充读取 WikiForge 项目内 AI 开发 Skill 的规则 |
| `2026-05-23-AI开发规则-AGENTS-v0.7.md` | v0.7 | 补充并行开发前置规则、高冲突串行区和主编排 Agent 要求 |
| `2026-05-23-AI开发规则-AGENTS-v0.8.md` | v0.8 | 补充 docs/current、docs/process 等目录分层读取规则 |
| `2026-05-23-AI开发规则-AGENTS-v0.9.md` | v0.9 | 补充阶段节点复选框/单选框规则和 T0-T4 递进测试门禁 |
| `2026-05-23-AI开发规则-AGENTS-v1.0.md` | v1.0 | 补充日常开发可自动提交推送、提交信息需说明改动点、版本发布需用户确认 |
| `2026-05-23-AI开发规则-AGENTS-v1.1.md` | v1.1 | 明确标签和 GitHub Release 不得被识别为内部开发阻塞项，并补充中文+EnglishName 文档规则、current 日期前缀同步规则和 README/AGENTS/SKILL 例外 |
| `2026-05-23-AI开发规则-AGENTS-v1.2.md` | v1.2 | 补充 WORKFLOW、Issue 风格任务卡和轻量 Symphony-inspired 读取顺序 |
| `2026-05-23-AI开发规则-AGENTS-v1.3.md` | v1.3 | 补充 WikiForge Orchestration 辅助工程规则、独立服务 / UI 和只读边界 |
| `2026-05-23-架构评审材料-WikiForge-architecture-review-v0.1.md` | v0.1 | MVP 编码前架构评审材料 |
| `2026-05-23-架构评审材料-WikiForge-architecture-review-v0.2.md` | v0.2 | 补充 CI/CD、Docker 打包和部署评审项后的架构评审材料 |
| `架构评审-architecture-review/` | - | 外部 AI 架构评审材料、评审索引和最终结论 |
| `架构评审-architecture-review/2026-05-23-架构评审索引-architecture-review-index-v0.1.md` | v0.1 | 外部评审材料目录索引 |
| `架构评审-architecture-review/2026-05-23-架构评审结论-WikiForge-architecture-review-conclusion-v1.0.md` | v1.0 | 架构评审最终结论 |
| `2026-05-23-README-v0.1.md` | v0.1 | 项目入口说明 |
| `2026-05-23-README-v0.2.md` | v0.2 | 更新当前状态为 MVP 0 工程骨架阶段的项目入口说明 |
| `2026-05-23-README-v0.3.md` | v0.3 | 更新当前状态为少服务微服务目标架构 |
| `2026-05-23-README-v0.5.md` | v0.5 | 补充版本更新记录入口和当前版本标签 0.02 |
| `2026-05-23-README-v0.6.md` | v0.6 | 更新当前状态为 MVP1 本地源文件归集整理闭环和版本标签 0.03 |
| `2026-05-23-README-v0.7.md` | v0.7 | 更新文档入口链接到 docs/current 和 docs/README.md |
| `2026-05-23-文档目录-docs-index-v0.1.md` | v0.1 | 新增 docs 目录入口，说明 current、process、archive、plans、ai-skills 分层 |
| `2026-05-23-文档目录-docs-index-v0.2.md` | v0.2 | 补充项目整体计划 Project Roadmap 入口 |
| `2026-05-23-文档目录-docs-index-v0.3.md` | v0.3 | 补充 MVP2 发布前自检 Release Checklist 入口 |
| `2026-05-23-文档目录-docs-index-v0.4.md` | v0.4 | 修正 current 文档入口日期，并补充文档一致性自检入口 |
| `2026-05-23-文档目录-docs-index-v0.5.md` | v0.5 | 补充 MCP 接口契约入口 |
| `2026-05-23-文档一致性自检-WikiForge-docs-consistency-check-v0.1.md` | v0.1 | 记录文档日期、标题、Release 边界和约定文件名例外检查结果 |
| `2026-05-23-Symphony工作模式评估-WikiForge-symphony-workflow-review-v0.1.md` | v0.1 | 评估 OpenAI Symphony 工作模式，初始结论为轻量吸收任务控制平面 |
| `2026-05-23-Symphony工作模式评估-WikiForge-symphony-workflow-review-v0.2.md` | v0.2 | 按用户新要求调整为自建 Orchestration 辅助工程，不照搬 Symphony Elixir 服务端 |
| `2026-05-23-工作流-WORKFLOW-v0.1.md` | v0.1 | 新增 WikiForge Agent 任务控制入口 |
| `2026-05-23-工作流-WORKFLOW-v0.2.md` | v0.2 | 补充 Orchestration Service / UI 作为长期任务控制台 |
| `2026-05-23-需求文档-knowledge-base-prd-v0.3.md` | v0.3 | 补充双语标题一致性修正 |
| `2026-05-23-需求文档-knowledge-base-prd-v0.4.md` | v0.4 | 将 Source frontmatter 示例日期改为占位符，避免 current 文档出现过期具体日期 |
| `2026-05-23-需求文档-knowledge-base-prd-v0.5.md` | v0.5 | 补充 MVP5 轻量 MCP Preview 工具和权限边界 |
| `2026-05-23-MCP接口契约-mcp-api-contract-v0.1.md` | v0.1 | 新增 MCP HTTP Preview API、工具 JSON Schema、日志表和错误码冻结契约 |
| `AI开发Skill-WikiForge-development-skill-v0.1/` | v0.1 | WikiForge 项目内 AI 开发 Skill 及 references 快照 |
| `AI开发Skill-WikiForge-development-skill-v0.2/` | v0.2 | 补充 AI 角色 Prompt 和开发工作流模板后的 Skill 快照 |
| `AI开发Skill-WikiForge-development-skill-v0.6/` | v0.6 | 补充主编排 Agent、Parallel Work Order v2、专家矩阵和 Handoff Packet 后的 Skill 快照 |
| `AI开发Skill-WikiForge-development-skill-v0.7/` | v0.7 | 补充 WORKFLOW、Issue 风格任务卡和轻量 Symphony-inspired 协作规则 |
| `AI开发Skill-WikiForge-development-skill-v0.8/` | v0.8 | 补充 Orchestration 辅助工程、独立服务 / UI 和只读控制台边界 |

## 当前阶段结论

当前已完成 MVP5 R4-2 MCP 契约冻结，包含独立契约文档、HTTP Preview API、首批 5 个工具、权限边界、调用日志脱敏规则、migration 编号和后续并行 Work Order。下一内部开发节点为 R4-3：实现 `create_source` / `search_sources` / `get_source`。

- MVP 先做本地源文件归集整理。
- MVP 打通最小 Obsidian Source Note 归档闭环。
- 飞书/腾讯文档、完整 MCP、向量库、个人记录、办公室视图放到 V1/V2。
- 技术栈采用 Java + Spring Boot + Vue + MySQL。
- 架构评审需覆盖 CI/CD、Docker 镜像打包、Docker Compose 发布、volume 挂载和健康检查。
- 架构评审最终结论为：可以进入 MVP 0 项目骨架阶段。
- MVP 0 工程骨架已创建后端、前端、CI、Docker Compose、Flyway 与健康检查基线。
- 本地验证已通过后端测试、后端打包、前端构建和 Compose 配置校验；Docker 镜像实构建受本机 Docker Desktop Linux engine 未启动影响暂未完成。
- 用户已选择 B 方案：少服务微服务。
- MVP 0/1 目标服务调整为 `wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui`、`mysql`。
- 已建立 WikiForge 项目内 AI 开发 Skill，用于约束多人和多 AI 后续开发。
- 已补充 Architect、Core、Worker、UI、DevOps、Docs、Review 等角色 Prompt。
- 已补充 Work Order 模板，要求每个开发切片先声明目标服务、目标文件、依赖契约和验证命令。
- Git 提交规则已明确：`node_modules/`、`dist/`、`.vite/`、`target/`、`.env`、运行数据和本地知识库数据不提交；`package-lock.json` 可提交。
- MVP0 服务拆分已落地：后端为 `wikiforge-common`、`wikiforge-core-service`、`wikiforge-worker-service` 三个 Maven 模块。
- Docker Compose 已调整为 `mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui`。
- 本地验证已通过后端多模块测试、后端打包、前端构建和 Compose 配置校验；Docker 镜像实构建仍受本机 Docker Desktop Linux engine 未启动影响。
- 同日归档规则已调整：开发日志和归档快照优先滚动更新当天最新版本文件，在文件内部追加版本记录，文件名按 `v0.1 -> ... -> v0.9 -> v1.0` 递增。
- 长归档文件已增加版本索引规则：AI 默认先读版本索引和最新版本小节，避免因为历史记录过长占用过多上下文。
- 并行开发 init 检查已完成：后端、前端和部署骨架验证通过，但进入 MVP1 并行开发前必须先提交或冻结当前骨架，并串行冻结 API / DTO / DDL / 状态枚举 / 路径安全契约。
- 已补充专家编排规则：主编排 Agent 负责 Parallel Work Order v2、专家选择、文件边界、高冲突串行区、Handoff Packet、最终集成验证和归档更新。
- 本地 `main` 已快进合并 MVP0 工程骨架提交，版本标签规划为 `0.02`。
- 已新增版本更新记录，明确 `0.02` 是工程基线版本，不包含 MVP1 文件扫描、归集、解析和 Obsidian 写入业务。
- `main` 和标签 `0.02` 已推送到远程仓库。
- 已创建开发分支 `codex/mvp1-source-ingestion`。
- 已创建 MVP1 契约冻结 Parallel Work Order，冻结 API、DTO、DDL、状态枚举、路径安全规则和并行任务边界；正式并行实现前必须先完成契约确认和 common 串行区。
- MVP1 已正式派发 Core、Worker、UI、DevOps、Test/Review 多路并行任务。
- QA 已提前发现并推动收口 P1 契约缺口：去重归属、完整 DDL、路径安全、内部 API token、环境变量对齐和大文件策略。
- MVP1 并行成果已进入主编排集成：Core 创建任务后会派发 Worker，Worker 回调 Core 写入状态和 Source Files。
- Compose 已补齐 Worker -> Core 的 `WIKIFORGE_CORE_SERVICE_BASE_URL`，Core / Worker 配置已与冻结契约对齐。
- 本地 MySQL 容器 + Core / Worker Jar 已跑通 MVP1 端到端烟测：创建导入任务、Worker 扫描样例目录、复制 Raw Sources、识别重复文件、回写任务状态和 Source Files。
- 本轮烟测已修复 MySQL 8 保留字 `recursive`、MyBatis 别名、Worker `PATCH` 回调和服务重启后 `jobUid` 碰撞问题。
- Docker Hub 连接恢复后，已完成基础镜像拉取、Core / Worker / UI 镜像构建、Compose 启动和容器级端到端导入验收。
- Docker Compose 当前运行状态：`mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui` 全部 healthy，访问入口为 `http://localhost:3000`。
- 版本 `0.03` 已补充发布说明，定位为 MVP1 本地源文件归集整理闭环版本。
- colbymchenry/codegraph 已纳入技术参考清单和后续路线：适合作为 V2+ 代码仓库 Source Parser、代码知识图谱和 MCP Agent 代码上下文查询参考，不进入 MVP1/MVP2 核心范围。
- docs 目录已整理为 `docs/current/` 当前主线文档、`docs/process/` 过程材料、`docs/archive/` 日期快照、`docs/superpowers/plans/` 开发计划、`docs/ai-skills/` 项目内 Skill。
- 用户确认本机 Obsidian Vault 地址为 `E:\WikiForgeVault`；Docker 使用 `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH` 将宿主机路径挂载到容器内 `/data/wikiforge/obsidian-vault`。
- 下一阶段开发计划已创建为 MVP2 Obsidian Source Note，目标是初始化 Vault、生成 Source Note Markdown、写入 `obsidian_notes`、Web UI 预览和安全打开 Obsidian。
- 本次自检验证已通过：`git diff --check`、`docker compose -f deploy/docker-compose.yml config`、前端 `npm run build`、后端 `mvn test`；Maven 需临时 settings 绕过不可达 mirror，并使用 JDK 25 编译 Java 21 release。
- MVP2 后端已落地：新增 `obsidian_notes`、Obsidian 领域/仓储/服务/控制器、Source Note 草案、Vault 写入、预览和路径逃逸防护。
- MVP2 前端已落地：Dashboard 支持初始化 Vault、生成 Source Note、编辑 Markdown、写入 Vault、读取预览和打开 Obsidian。
- MySQL 兼容性调整：`idx_obsidian_notes_vault_path` 使用 `vault_path(255)` 前缀索引，避免 `utf8mb4` 下完整索引过长。
- Docker Compose 已使用 `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH=E:/WikiForgeVault` 重建验证，Core / Worker health 为 UP，UI 入口 `http://localhost:3000` 返回 200。
- `POST /api/v1/obsidian/init` 已成功在本机 `E:\WikiForgeVault` 下创建混合目录结构：`00_Inbox_收集箱`、`Sources_来源`、`10_Wiki_主题库`、`20_Projects_项目`、`30_Resources_资源`、`90_System_系统`。
- 浏览器检查通过：本地 UI 可见 MVP2、初始化 Vault 和 Source Note 操作，console error 为空。
- 已新增项目整体计划 Roadmap，当前执行主线为 R0：MVP2 收束与发布准备。
- 已开始执行 R0 节点：R0-1 路线图文档、R0-2 版本 0.04 发布说明草案、R0-3 端到端烟测、R0-4 发布前自检已完成。
- R0-3 烟测结果：`job_20260523_0ae896c3e383` 完成导入，`note_20260523_1cf541982149` 写入 `E:\WikiForgeVault\00_Inbox_收集箱\Sources_来源`，预览内容包含标题。
- 项目整体计划已补充 Agent 可读进度规则：复选框 `- [x]` 表示完成，`- [ ]` 表示未完成，单选框 `(x)` 表示当前唯一执行位置。
- 测试策略已调整为节点递进门禁：T0 文档/Git 卫生、T1 契约与单元测试、T2 构建验证、T3 Docker 节点烟测、T4 阶段级端到端验收。
- 后续开发不得为每个小功能重复跑完整阶段验收，应按节点声明测试层级并逐层推进。
- `AGENTS.md` 已同步该规则，后续 AI 必须在完成阶段或节点时更新路线图、自检清单、开发者日志和归档索引。
- 当前 R0 节点单选指针已移动到 R0-5，当前测试门禁单选指针已移动到 T4。
- R0-4 补充 Git 卫生检查：本地存在 `frontend/node_modules`、`frontend/dist`、`backend/**/target`、`data` 等 ignored artifacts，但 `.gitignore` 已忽略，且 `git ls-files` 确认禁止路径未被跟踪。
- R0-4 补充边界：GitHub Actions CI 需推送后看远程结果，Obsidian URI 生成已验证，真实唤起客户端依赖本机协议注册。
- 用户已授权日常开发完成后可以自动提交并推送当前开发分支。
- 每次提交信息必须简要说明本次改动点。
- 版本标签、GitHub Release 和正式版本发布定义仍需用户确认后执行，但属于外部发布宣导事项，不阻塞内部开发分支、提交推送和下一工作流递进。
- R0-5 提交推送已完成：commit `a987780 feat: add obsidian source note mvp2` 已推送到 `origin/codex/mvp2-obsidian-source-note`。
- GitHub 返回 PR 创建入口：`https://github.com/BonusWang/WikiForge/pull/new/codex/mvp2-obsidian-source-note`。
- R0-6 版本标签 `0.04`、GitHub Release 和正式发布定义等待用户确认；当前不创建标签，不创建 Release，且不作为开发阻塞项。
- 当前开发分支为 `codex/mvp3-document-parsing`。
- MVP2.1 已完成 Vault 状态面板、Source Files 已写 Note 状态、读取已有 Note、默认重复写入策略和集成测试补充。
- 当前开发者日志主文件已修正为 `docs/current/2026-05-23-开发者日志-WikiForge-developer-log.md`。
- current 中仍在维护的 MVP 实施计划和参考项目清单已从 2026-05-22 前缀修正为 2026-05-23 前缀。
- 文档命名规则已明确：项目文档使用中文名 + EnglishName；`README.md`、`AGENTS.md`、`SKILL.md` 等约定文件名保持原名，不进行中文化改名。
- MVP3 文档解析已完成：`source_contents` 表已落地，Markdown / TXT / PDF / Word 基础正文抽取已打通，Source Note 草案已展示正文摘录。
- 本轮验证通过：Worker 解析器目标测试和后端 Maven 多模块测试。
- MVP4 AI 辅助整理与审核首轮已完成：`agent_runs`、`agent_steps`、`review_items` 已落地，Core API 可生成待审核草案，Dashboard 已展示审核队列。
- AI Provider 已配置化：通用约定为 `WIKIFORGE_MODEL_<PROVIDER>_TYPE`、`WIKIFORGE_MODEL_<PROVIDER>_API_KEY`、`WIKIFORGE_MODEL_<PROVIDER>_BASE_URL`、`WIKIFORGE_MODEL_<PROVIDER>_MODEL`。
- MiniMax、DeepSeek、CC Switch 等 OpenAI-compatible Provider 后续可通过环境变量和 Web UI 请求参数切换；真实密钥只通过环境变量读取，不写入仓库。
- 本轮验证通过：MVP4 目标集成测试、后端 Maven 多模块测试、前端构建。
- R3-5 审核通过写入 Obsidian Source Note 已完成：`POST /api/v1/review-items/{reviewUid}/approve` 可将待审核 AI 草案写入 Vault，并把审核项标记为 `approved`。
- Dashboard 审核详情抽屉已支持 `通过并写入 Obsidian`，成功后刷新审核队列并打开 Source Note 预览。
- R4-0 工作模式升级已完成：新增 `WORKFLOW.md`、Issue 任务卡、Skill 规则和 Symphony 工作模式评估。
- R4-1 Orchestration 辅助工程骨架已完成：新增 `wikiforge-orchestration-service`、`orchestration-ui`、Docker / CI 配置和本地访问入口。
- MVP5 计划文档已补充指针说明：`(x)` 表示当前唯一执行位置，不表示完成；完成状态看节点清单 `[x]`。
- R4-2 MCP 契约冻结已完成：新增 `docs/current/MCP接口契约-mcp-api-contract.md`，冻结首批工具、权限边界、调用日志和 migration 编号。
- 下一内部开发节点为 R4-3：实现 Source 类 MCP Preview 工具。

## 后续归档建议

后续每次完成关键迭代时，新增一个日期目录，例如：

```text
docs/archive/2026-06-01/
```

不要把归档文件直接堆在 `docs/archive/` 根目录下；`docs/archive/` 根目录只保留归档说明文件。

每个归档目录应至少包含：

- PRD
- 技术架构
- 数据模型
- 实施计划或迭代计划
- 开发者日志
- 归档索引
