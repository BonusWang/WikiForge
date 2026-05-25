# WikiForge 项目架构强约定 Project Architecture Conventions

## 1. 适用范围

本约定覆盖 WikiForge 全项目和后续全流程，不限于 MVP0。

任何新功能、新页面、新服务、新 API、新表结构、新后台任务、新原子能力，都必须先接入本约定并登记，再进入设计或开发。

## 2. 总原则

- 所有能力都按四层架构口径登记。
- 四层架构是业务能力拆分，不替代代码 DDD 分层。
- 新能力必须先证明属于当前阶段目标，不能默认加入主流程。
- 新服务必须先证明 Core / Worker / Common 无法承载。
- 新数据库表必须先证明当前最小表集合无法表达。
- 历史能力默认不回主线，除非重新出设计并通过本约定。

## 3. 架构登记台账

本文件同时作为架构登记台账。新增、调整或退役能力时，必须更新本节。

| ID | 类型 | 名称 | 四层归属 | 承载位置 | 状态 | 维护要求 |
| --- | --- | --- | --- | --- | --- | --- |
| CAP-001 | 功能 | 文件收纳入口 | 轻应用 / 决策 / 指令执行 / 原子能力 | Frontend Capture + Core + Worker | 主流程 | 变更入口、任务字段或状态时更新 |
| CAP-002 | 原子能力 | 路径安全校验 | 原子能力 | Common | 复用 | 变更路径规则、Vault 逃逸规则时更新 |
| CAP-003 | 原子能力 | Raw Sources 复制收纳 | 原子能力 | Worker | 主流程 | 变更复制、命名、去重规则时更新 |
| CAP-004 | 原子能力 | hash 去重 | 原子能力 | Worker | 主流程 | 变更 hash 算法或重复策略时更新 |
| CAP-005 | 原子能力 | 正文抽取 | 原子能力 | Worker；浏览器上传同步抽取由 Core 复用同一抽取策略 | 主流程 | 新增文件类型或抽取策略时更新 |
| CAP-006 | 功能 | Obsidian LLM Wiki 写入 | 决策 / 指令执行 / 原子能力 | Core + Obsidian adapter | 主流程 | 变更 schema、index、log、写入结果时更新 |
| CAP-007 | 数据表 | MVP0 最小收纳表 | 指令执行 | MySQL | 主流程 | 新增、退役、迁移表时更新 |
| CAP-008 | 服务 | Gateway | 轻应用入口代理 | 未启用 | 冻结 | 启用前必须补设计和服务准入 |
| CAP-009 | 服务 | Orchestration 辅助工程 | 非 MVP0 主线 | 已删除 | 退役 | 源码、Dockerfile、`agentteam/` 工作区和配置样例已从 MVP0 移除 |
| CAP-010 | 约定 | 前端样式基线 | 轻应用层 | Frontend styles | 主流程 | 新页面、新组件、状态标签变化时更新 |
| CAP-011 | 约定 | 后端服务边界 | 决策 / 指令执行 | Core / Worker / Common | 主流程 | 新 API、新任务、新包边界时更新 |
| CAP-012 | 数据表 | 状态字典表 | 指令执行 | MySQL `system_dictionaries` | 主流程 | 新状态、颜色、中文说明变化时更新 |
| CAP-013 | 约定 | Obsidian LLM Wiki 结构 | 决策 / 指令执行 / 原子能力 | Obsidian Vault `WikiForge/` | 主流程 | Vault 目录、页面模板、index/log 或托管区块规则变化时更新 |
| CAP-014 | 约定 | MVP0 API 契约 | 轻应用 / 指令执行 | Core API / Worker Internal API | 主流程 | 新增、修改、退役 API 或响应字段时更新 |
| CAP-015 | API | 浏览器上传收纳接口 | 轻应用 / 指令执行 / 原子能力 | Frontend Capture + Core `/api/v1/upload-sources` + Raw Sources | 主流程 | 上传字段、文件命名、落盘状态或大小限制变化时更新 |
| CAP-016 | 历史能力 | 高级能力历史接口集合 | 非 MVP0 主线 | AI Review / MCP Preview / Vector Export / LifeOS / Knowledge Maintenance / Wiki Compile / Source Note / Link Source | 退役 | MVP0 代码、前端封装、迁移和测试已移除；恢复必须重新出需求、API、表设计和验证 |

状态说明：

- 主流程：当前阶段主流程能力。
- 复用：可直接复用的基础能力。
- 冻结：保留但当前阶段不进入主流程。
- 退役：已清理出入口、构建、API 或数据库；历史材料仅在归档中留存。

## 4. 四层登记规则

每个新能力必须写清楚归属：

| 层级 | 必填说明 |
| --- | --- |
| 轻应用层 | 用户从哪个入口看到它，是否需要新增页面或导航 |
| 决策层 | 它依赖哪些规则、策略、AI 判断或兜底逻辑 |
| 指令执行层 | 它对应哪些任务、状态流转、失败记录和重试方式 |
| 原子能力层 | 它拆成哪些最小动作，是否可独立测试 |

如果一个能力无法说清四层归属，不进入开发。

## 5. 代码分层规则

后端代码仍遵守：

```text
interfaces -> application -> domain <- infrastructure
```

要求：

- `interfaces` 只处理 REST、DTO、请求响应转换。
- `application` 负责编排用例和事务边界。
- `domain` 放核心模型、状态、规则，不依赖框架。
- `infrastructure` 放数据库、文件系统、模型适配器、Obsidian 写入实现。
- Worker 只执行扫描、复制、hash、抽取等任务，不提供用户查询接口。
- Common 只放通用能力，不放业务流程。

## 6. 前端结构规则

MVP0 前端只允许五个一级入口：

```text
收纳 / 资料箱 / Wiki / 日志 / 设置
```

目标目录：

```text
frontend/src/
  shell/
  pages/
  features/
  api/
  types/
  styles/
```

要求：

- 不新增高级能力入口。
- 不新增总览大 Dashboard。
- 不把多个页面逻辑继续堆进单体页面。
- 新页面必须先说明属于五入口中的哪一个。
- 新页面必须复用前端样式基线。
- 用户可见状态必须通过字典映射为中文。
- 修改全局样式必须说明影响范围。

前端样式基线以 MVP0 前端设计方案为准：

```text
docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-前端设计方案-WikiForge-mvp0-frontend-design.md
```

## 7. 服务准入规则

MVP0 不新增独立 Gateway，不恢复 Orchestration。

新增服务必须同时满足：

1. Core / Worker / Common 无法清晰承载。
2. 有独立生命周期、部署边界和失败隔离价值。
3. 有明确 API 契约和数据归属。
4. 不会让前端直接依赖多个服务地址。
5. 已写入架构设计文档并通过用户确认。

MVP0 默认服务边界：

```text
Frontend -> Core API -> Worker/Internal
```

Gateway 只保留未来兼容开口：API 版本化、前端单 `API_BASE_URL`、Worker 不暴露给前端。

## 8. API 契约规则

MVP0 API 以 API 契约设计文档为准：

```text
docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-API契约设计-WikiForge-mvp0-api-contract.md
```

规则：

- 前端只调用 Core API。
- Worker 内部 API 不暴露给前端。
- 新增、修改或退役 API 必须同步维护本文件和 API 契约设计。
- 用户可见状态必须返回中文码值、中文名称和中文说明。
- API 不返回非必要宿主机敏感绝对路径。

## 9. 数据库准入规则

MVP0 数据库最小表集合：

| 表 | 用途 |
| --- | --- |
| `import_jobs` | 收纳任务账本 |
| `source_files` | Raw Sources 文件账本 |
| `source_contents` | 正文抽取结果 |
| `wiki_ingest_runs` | Obsidian LLM Wiki 写入结果 |
| `system_dictionaries` | 中文状态码、中文说明和颜色映射 |

规则：

- 不为未来能力预建表。
- 不复用 `agent_runs` / `review_items` 承载 Wiki ingest。
- `sources`、MCP、向量、LifeOS、知识体检、AI Review、旧 Source Note、旧 Wiki Compile、Link Source、Orchestration 相关表不进入 MVP0 主流程。
- MVP0 新库不创建历史表；已有本地库如已执行历史迁移，需按单独清理方案重建或迁移。
- 新表必须说明所属四层能力、归属服务、生命周期和删除策略。
- 状态码必须先进入 `system_dictionaries`，前端不得硬编码英文状态。

## 10. Obsidian 写入规则

MVP0 Obsidian 写入只允许落在 Vault 内 `WikiForge/` 托管目录。

规则：

- 不写、不改、不删 `WikiForge/` 之外的用户笔记。
- Raw Sources 不放入 Obsidian Vault。
- 来源页、主题页、项目页、`index.md`、`log.md` 的目录结构以 Obsidian LLM Wiki 设计为准。
- 用户手写内容必须放在托管区块外，系统不得覆盖托管区块外内容。
- 修改 Vault 目录、页面模板、index/log 或托管区块规则时，必须同步维护架构登记台账。

设计文档：

```text
docs/rebuild/2026-05-24-mvp0-baseline/2026-05-24-Obsidian-LLM-Wiki设计-WikiForge-mvp0-obsidian-llm-wiki-design.md
```

## 11. 能力状态规则

能力状态只使用四类中文码值，并且必须同步到“架构登记台账”：

| 状态 | 含义 |
| --- | --- |
| 主流程 | 当前主流程能力 |
| 复用 | 可直接复用的基础能力 |
| 冻结 | 保留但不进入当前阶段 |
| 退役 | 已清理出入口、构建或数据库，或仅在归档中留存 |

当前退役能力必须登记在台账中：

- Orchestration Service / UI（源码和 Dockerfile 已删除）
- 并行 Agent 团队制度（`agentteam/` 已删除）
- 历史 Work Order 启动路径
- AI Review / Review Items
- MCP Preview
- Vector Export
- LifeOS / Personal Records
- Knowledge Maintenance
- 旧 Source Note / `obsidian_notes`
- 旧 Wiki Compile / Wiki Integrations
- Link Source

## 12. 设计准入清单

任何新功能进入开发前，必须回答：

1. 它解决 MVP0 哪个问题？
2. 它属于五入口中的哪个入口？
3. 它在四层架构中分别是什么？
4. 它由 Core、Worker、Common 还是未来服务承载？
5. 它是否需要新 API？
6. 它是否需要新表？为什么现有表不够？
7. 它的失败状态如何记录？
8. 它的验证命令是什么？
9. 它是否引入历史包袱或未来能力预建设？

未通过清单，不进入开发。

## 13. 维护要求

以下变更必须同步维护本文件：

- 新增功能。
- 新增服务。
- 新增或删除原子能力。
- 新增 API。
- 新增、退役或迁移数据库表。
- 页面入口或导航变化。
- 前端样式基线变化。
- 状态字典变化。
- 能力从“冻结”进入“主流程”。
- 能力从“主流程”退回“冻结”或“退役”。

每次维护至少更新：

1. 架构登记台账。
2. 对应四层归属说明。
3. 承载位置。
4. 状态。
5. 维护要求或验证命令。
