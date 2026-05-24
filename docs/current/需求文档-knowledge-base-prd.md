# 知识熔炉 WikiForge 需求文档 PRD v0.2

## 1. 产品背景

个人知识目前分散在多个系统和载体中，包括个人看板、密码文档、飞书在线文档、腾讯文档、印象笔记、本地文档、微信收藏、B站收藏、知乎文章、项目文档和杂乱文件。

核心痛点不是单纯“数据源很多”，而是：

- 有很多数据源，但没有统一收集。
- 收集之后没有整理，文件和资料长期散落。
- 没有稳定归档规则，导致资料找不到、用不上。
- 没有提炼机制，资料停留在收藏和堆积状态。
- 没有复用机制，过去看过、存过、写过的内容难以进入后续项目、决策和知识问答。

因此，现有问题不是缺少存储位置，而是缺少从收集、整理、归档、提炼到复用的完整闭环。

本系统命名为 **知识熔炉 WikiForge**。目标是先把分散、杂乱的源文件和资料收集汇总到统一位置，形成有秩序的原始资料库；然后再由本地数据库、Obsidian Vault 和多 Agent 流水线决定是否进行提炼、加工和归档，最终形成可追溯、可演化、可搜索的个人知识库。

同时，WikiForge 不只整理各种文档，也需要支持持续记录用户自身的数字生活数据，例如消费记录、账单、邮件、人际关系和个人事件。用户希望通过 OpenClaw 或 Hermes 机器人调用本项目 MCP，把这些记录写入 WikiForge，再由系统进行整理、归纳和总结。

后续规划会将整理后的知识和个人记录结果批量导入向量库，用于语义检索、知识问答和智能 Agent 调用。

## 2. 产品定位

产品定位为一个本地优先的 LLM Wiki / Agentic Knowledge Base。

产品口号：把杂乱资料锻造成可演化知识库。

WikiForge 采用 **LLM Wiki + GBrain 融合路线**：

- LLM Wiki 负责知识表达层：把原始资料编译成稳定、可读、可编辑、可维护的 Obsidian Wiki。
- GBrain 思路负责知识运行层：让知识可以被 Agent、MCP、向量库、定时任务和外部机器人持续调用、更新和复用。

关键判断：

- 传统 RAG 解决的是“找得到”，但没有解决“沉淀成体系”。
- WikiForge 第一阶段要先把知识沉淀下来，形成稳定的表达层。
- WikiForge 后续要让知识跑起来，形成可持续运行的个人知识操作系统。

系统通过独立 UI 看板接收资料，由 Agent 流水线进行收集、清洗、分类、整合、审核和归档，最终沉淀为：

- 可追溯的 Source Note
- 可演化的 Obsidian Wiki 页面
- 可持续积累的个人记录
- 可检索的 MySQL 结构化索引
- 可操作的个人/项目看板

其中 Obsidian 是知识正文的长期沉淀层，MySQL 是控制平面、索引库和 Agent 流程账本，不替代 Obsidian 成为最终知识库。

## 3. 参考方向

- Karpathy LLM Wiki：参考 Raw Sources / Wiki / Schema 的分层思想，强调原始资料只读、Wiki 可演化、规则显式化。
- Superset：参考多 Agent 编排、任务状态、执行日志和人类审核体验。
- LangGraph：参考状态流转、人类介入和可恢复工作流。
- Khoj / Onyx：参考个人知识库、知识源接入和检索体验。

## 4. 第一版目标

第一版优先跑通“从混乱到可复用”的知识处理闭环：

资料进入系统 -> 源文件归集整理 -> 建立索引和分类 -> 决定是否进入 Agent 提炼加工 -> 生成建议和草案 -> 自动归档或人工审核 -> 写入 MySQL 和 Obsidian -> UI 看板追踪状态。

核心目标：

- 支持多来源资料统一进入。
- 支持指定本地路径扫描、解析和导入文件。
- 支持源文件整理模式，把散落文件统一归集到规范目录。
- 支持先整理源文件，后续再决定是否进行提炼加工。
- 支持归档规则，让资料从“堆积”变成“可查找、可维护”。
- 支持提炼流程，让重要资料从“收藏”变成“可复用知识”。
- 支持 MySQL 保存结构化索引、状态、标签、任务和 Agent 输出。
- 支持 Obsidian 作为主阅读、编辑和长期沉淀界面。
- 支持独立 Web UI 看板，不需要登录。
- 支持 Web UI 内预览 Obsidian Markdown，并一键打开本地 Obsidian 文件。
- 支持 Markdown 主存储，以及 HTML 预览和报告导出。
- 支持国内模型调用，并可通过 CC Switch 切换模型供应商。
- 支持低风险资料自动归档，高价值、不确定、敏感资料进入人工审核。
- 已开始为后续批量导入向量库落地内容分块、embedding 状态和导出流程；R6-1 首版先提供 JSONL chunk 导出，不强依赖真实向量库。
- 采用前后端分离架构，主要开发语言和后端技术栈为 Java。
- 技术选型优先采用国内开发主流、成熟、维护活跃的框架和依赖，避免生僻冷门技术。

长期拓展目标：

- 支持在线文档链接采集，例如输入飞书文档、腾讯文档等地址后，由后台连接器读取文档内容。
- 支持办公室视图展示和操作 Agent 状态。
- 支持 MCP 能力，便于后续接入外部 Agent、工具和资料源。
- 支持通过 OpenClaw / Hermes 机器人调用 MCP 写入个人记录，如消费、账单、邮件、人际关系和个人事件。

## 4.1 MVP 实施边界

用户已确认：MVP 需要优先保证可行性，先整理，再拓展。

MVP 第一目标：

> 先把本地杂乱源文件归集整理起来，并打通最小 Obsidian 归档闭环。

MVP 核心闭环：

```text
指定本地路径
  -> 扫描文件
  -> 复制归集到 WikiForge_RawSources
  -> 建立 MySQL 索引
  -> 选择少量文件进入处理
  -> 生成 Source Note Markdown 草案
  -> 人工审核
  -> 写入 Obsidian Vault
  -> Web UI 可查看状态并打开 Obsidian 文件
```

MVP 必须包含：

- 本地路径扫描。
- 文件复制归集。
- hash 去重。
- 文件基础分类。
- MySQL Source/File/ImportJob 索引。
- Obsidian Vault 目录初始化。
- Source Note 模板和 frontmatter。
- Web UI 基础后台。
- Markdown 预览和 `obsidian://open`。
- 单 LLM 多步骤的轻量 AI 辅助整理。

MVP 暂缓：

- 飞书 / 腾讯文档自动读取。
- OpenClaw / Hermes 自动写入。
- 复杂办公室等距 UI。
- 完整 MCP Server。
- 个人消费、账单、邮件、人际关系记录。
- 向量库和 hybrid search。
- Lint Agent / 维护 Agent。
- 多模态音视频处理。

MVP 预留但不完整实现：

- 在线文档连接器接口。
- MCP tool schema。
- Personal Record 数据模型。
- Vector Export 数据模型。
- 办公室视图状态模型。

## 4.2 阶段规划

### MVP 0：项目骨架

目标：系统能本地跑起来。

- Java Spring Boot 后端。
- Vue 前端。
- MySQL。
- 基础配置。
- Obsidian Vault 路径配置。
- Raw Sources 路径配置。

### MVP 1：源文件归集

目标：先解决“我的东西太乱”。

- 指定路径扫描。
- 文件复制归集。
- hash 去重。
- 文件类型识别。
- Source/File 索引。
- Web UI 文件列表。

### MVP 2：Obsidian Source Note

目标：让资料能进入可读、可编辑的知识层。

- Source Note 模板。
- frontmatter。
- index / log。
- Obsidian 文件映射。
- Markdown 预览。
- 一键打开 Obsidian。

### MVP 3：AI 辅助整理

目标：开始提炼，但保持人工审核。

- 文档解析。
- 摘要。
- 标签/分类建议。
- 知识卡草案。
- 人工审核。
- 写入 Obsidian。

### MVP 5：轻量 MCP 预览版

目标：为后续 OpenClaw / Hermes 接入打基础。

- `create_source`
- `search_sources`
- `get_source`
- `get_obsidian_note`
- `create_personal_record` 先只存储，不做复杂总结。
- MCP 调用日志。

### V1：在线资料与个人记录

- 已完成首版链接资料收集入口：支持手工输入飞书、腾讯文档、网页、微信、B站、知乎等 URL，保留原始链接和正文/备注。
- 已完成首版个人记录：支持邮件、账单、消费、人际关系、事件和普通笔记入库、筛选、汇总。
- 已完成个人记录写入 Obsidian：先沉淀到 `00_Inbox_收集箱/Personal_个人记录`，后续再进入提炼和 Wiki 编译。
- OpenClaw / Hermes 可继续通过 MCP 写入个人记录，Web UI 和 REST API 使用同一张记录表。
- 真实飞书 / 腾讯文档授权读取、个人记录周期总结进入 V1.x / V2。

### V2：知识运行层

- 向量导出契约：已落地 `POST /api/v1/vector-exports` 和 `GET /api/v1/vector-exports`，可把 Source 正文和个人记录导出为 JSONL chunks。
- 向量库：后续接入 Qdrant / Milvus / pgvector 等私有化向量库。
- hybrid search：等待向量库选型和部署方式确认。
- Lint / 维护 Agent：已落地首版手动维护巡检，能发现空正文、重复正文、未归档个人记录、空向量导出和长期 pending chunk。
- 维护问题处理闭环：R6-3.1 增加人工标记已解决、忽略、重新打开和处理备注，先形成可管理的问题队列。
- 办公室等距视图。
- 周报 / 月报。
- 长期记忆。

## 5. 核心用户

第一版主要服务个人使用者，未来预留私有部署和小团队使用空间。

典型用户特征：

- 有大量分散资料和收藏。
- 经常沉淀项目文档、文章、视频、文件和个人笔记。
- 希望 Obsidian 成为长期知识库。
- 希望 Agent 帮助检查、整理、归档，而不是只做聊天问答。
- 接受本地运行和半自动审核流程。

## 6. 产品形态

系统采用混合形态：

- Obsidian Vault：知识阅读、编辑、双链、长期沉淀。
- 独立 UI 看板：资料投喂、状态查看、审核确认、日志追踪、Obsidian 文件预览。
- 办公室视图：以可视化工位展示 Agent 状态，并支持查看任务、日志和基础操作。
- Raw Sources 原始资料库：统一保存被归集整理后的源文件。
- Personal Records 个人记录库：持续记录消费、账单、邮件、人际关系、个人事件等非文档型资料。
- MySQL 数据库：控制平面、结构化索引、来源、状态、标签、任务、Agent 输出和流程账本。
- Agent 流水线：资料处理和知识库维护。
- 本地优先部署：MVP 本地运行，后续可迁移到 NAS 或私有服务器。

MySQL 采用分级正文保存策略：

- 普通文本、网页、Markdown、小型 Word 可保存解析后的纯文本，便于检索、去重和 Agent 处理。
- PDF、大文件、图片、敏感资料默认只保存路径、摘要、hash 和元数据。
- 是否保存全文由资料类型、文件大小、敏感级别和用户规则决定。

## 6.1 源文件整理模式

源文件整理是第一版核心能力。系统需要先解决“文件太乱、散落在各处”的问题，再决定是否进一步提炼成 Wiki。

目标：

- 从多个指定路径扫描源文件。
- 识别文件类型、文件大小、hash、来源路径。
- 根据规则和 Agent 建议进行初步分类。
- 将文件集中整理到统一 Raw Sources 目录。
- 建立 MySQL 索引，记录原路径、新路径、分类、状态和处理日志。
- 整理完成后，用户可以选择是否进入 Agent 提炼加工流程。

建议 Raw Sources 目录：

```text
WikiForge_RawSources/
  00_Inbox_待整理/
  01_Documents_文档/
  02_Images_图片/
  03_PDFs_PDF/
  04_WebClips_网页收藏/
  05_ProjectFiles_项目文件/
  06_Exports_平台导出/
  90_Unknown_待确认/
```

整理策略：

- 第一版默认采用复制模式：将源文件复制到 Raw Sources 规范目录，保留原文件不删除。
- 支持记录原始路径，方便追溯文件来自哪里。
- 发现重复文件时，不重复复制，记录重复关系。
- 不确定分类的文件进入 `90_Unknown_待确认`。
- 用户确认后，资料才进入后续提炼加工、Source Note 生成和 Wiki 整合流程。

第一版不建议默认移动或删除原文件，避免误操作。

## 6.2 在线文档链接采集

在线文档是收集层的重要来源。用户可以在 Web UI 中输入飞书文档、腾讯文档或其他在线文档地址，系统后台通过 OpenClaw、Hermes 或服务自身连接器读取文档内容，再进入 Agent 处理流程。

目标：

- 支持用户粘贴在线文档链接。
- 自动识别链接来源平台，如飞书文档、腾讯文档、普通网页文档。
- 通过可插拔连接器读取标题、正文、作者、更新时间、权限状态和附件信息。
- 将读取结果生成 Source 记录和 Source Note 草案。
- 经过多 Agent 清洗、分类、提炼后输出知识卡。
- 知识卡可进入审核队列，并最终归档到 Obsidian 项目页、主题页或 Source Note。

读取方式：

- OpenClaw Connector：通过 OpenClaw 能力读取在线文档内容。
- Hermes Connector：通过 Hermes 服务读取在线文档内容。
- Native Connector：服务自身实现的平台连接器。

V1 建议：

- 先支持手动输入链接。
- 连接器采用统一接口，避免绑定单一服务。
- 如果文档无权限或读取失败，进入待处理队列并提示失败原因。
- 在线文档的原链接必须保留，归档后的知识卡必须可追溯到原始文档。

## 6.3 产品目标层级

知识熔炉 WikiForge 的目标分为四层：

1. 收集：把多源资料统一纳入系统。
2. 整理：把散落文件归集到 Raw Sources，并建立索引和初步分类。
3. 归档：把有价值资料沉淀到 Obsidian Source Note、项目页、主题页和实体页。
4. 复用：通过搜索、看板、HTML 报告、后续向量库和 MCP，让资料进入项目、决策、问答和 Agent 工作流。

第一版重点解决前两层，并打通归档闭环；提炼与复用作为持续增强方向，但数据结构和流程从一开始预留。

## 6.4 知识系统路线：LLM Wiki + GBrain

WikiForge 的长期路线不是单纯 RAG，也不是只做 Obsidian 笔记整理，而是融合 LLM Wiki 和 GBrain 两层能力。

### LLM Wiki：知识表达层

LLM Wiki 解决“知识如何稳定表达”的问题。

核心职责：

- 把 Raw Sources 原始资料编译成稳定的 Markdown / Obsidian Wiki 页面。
- 形成 Source Note、Topic、Project、Entity、Record 等可读知识对象。
- 支持人工编辑、双链、索引、日志和长期维护。
- 通过 Schema 约束目录、命名、frontmatter、页面类型和 Agent 行为。

价值：

- 降低传统 RAG 答案波动。
- 避免资料只进入索引却不形成知识体系。
- 让知识可以被人阅读、修改和复盘。

### GBrain：知识运行层

GBrain 思路解决“知识如何持续工作”的问题。

核心职责：

- 接入多源信息，如文件、在线文档、邮件、账单、消费、人际关系和个人事件。
- 通过定时任务、维护 Agent 和批处理持续更新知识。
- 将整理结果导出到向量库，供语义检索和 Agent 调用。
- 通过 MCP 暴露查询、写入、任务触发和 Obsidian 打开能力。
- 让 OpenClaw / Hermes 等机器人可以持续写入记录和调用知识。

价值：

- 知识不只是存起来，而是能被系统和 Agent 反复调用。
- 支持长期记忆、自动归纳、周期总结和行动项生成。
- 让个人数据进入持续运行的知识系统。

### WikiForge 的融合策略

第一阶段优先建设 LLM Wiki 表达层：

- Raw Sources 归集。
- Obsidian Vault 目录与模板。
- Source Note / Wiki 页面生成。
- Schema、index、log、frontmatter 规范。

第二阶段逐步增强 GBrain 运行层：

- 个人记录服务。
- MCP Server / Client。
- Vector Export Service。
- 定时巡检和 Lint Agent。
- OpenClaw / Hermes 机器人写入。
- 自动摘要、周报、月报、关系回顾和消费总结。

设计原则：

- Obsidian Wiki 是人可读、可编辑的稳定知识表达。
- MySQL 是系统运行账本和结构化状态。
- 向量库是后续语义检索和 Agent 调用层。
- MCP 是外部机器人和工具调用 WikiForge 的标准接口。
- Agent 运行结果应反馈到 Wiki 层，形成持续演化闭环。

## 6.5 个人数字生活记录

WikiForge 需要支持持续记录用户自身，而不只是管理文档。系统需要把个人生活、财务、沟通和关系类数据纳入统一记录体系，并通过 Agent 进行整理和总结。

记录范围：

- 消费记录：日常消费、订阅、购物、服务支出。
- 账单记录：信用卡账单、平台账单、报销、周期性费用。
- 邮件记录：重要邮件、待处理邮件、合同/发票/通知类邮件。
- 人际关系：联系人、互动记录、关系备注、后续跟进事项。
- 个人事件：想法、决策、日程回顾、生活记录、阶段总结。

写入方式：

- 用户在 Web UI 手动录入。
- OpenClaw / Hermes 机器人调用 WikiForge MCP 写入。
- 后续通过邮箱、账单、聊天、日历等连接器自动导入。

处理方式：

- 机器人或连接器写入原始记录。
- 系统建立结构化索引。
- Agent 对记录进行分类、归纳、总结和行动项提取。
- 重要记录可以生成 Obsidian 日记、关系页、账单总结、邮件摘要或项目记录。

原则：

- 个人记录和文档资料共用统一收集、整理、归档、复用闭环。
- 个人记录默认需要保留来源、时间和写入方式。
- 涉及隐私、财务、人际关系的记录需要支持敏感标记和人工审核。


## 7. Obsidian Vault 目录结构

采用混合目录结构：PARA 作为顶层组织方式，主题库和实体库作为横向知识网络。

建议目录：

```text
WikiForge_Vault/
  00_Inbox_收集箱/
    Sources_来源/
    Review_审核/
  01_Projects_项目/
  02_Areas_领域/
  03_Resources_资源/
    Topics_主题/
    Entities_实体/
    References_参考/
  04_Archives_归档/
  05_Actions_行动/
  06_Secrets_敏感资产/
  07_Records_个人记录/
  90_System_系统/
    AgentLogs_Agent日志/
    Templates_模板/
    Indexes_索引/
    Schemas_规范/
```

目录职责：

- `00_Inbox_收集箱`：所有待处理资料、导入资料和待审核草案。
- `01_Projects_项目`：项目维度知识沉淀。
- `02_Areas_领域`：长期责任领域，如产品、管理、技术、投资。
- `03_Resources_资源`：主题、实体、参考资料和长期知识资源。
- `04_Archives_归档`：归档项目、过期资料、低频资料。
- `05_Actions_行动`：待读、待实践、待复盘、待确认任务。
- `06_Secrets_敏感资产`：敏感资产索引。第一版不做加密。
- `07_Records_个人记录`：消费、账单、邮件、人际关系、个人事件等持续记录。
- `90_System_系统`：模板、Agent 日志、规则、索引、系统文档。

## 8. 核心知识对象

第一版包含以下对象：

- Source：网页、视频、文档、图片、聊天记录、本地文件。
- Topic：AI、产品管理、系统架构、个人管理等长期主题。
- Project：具体项目、客户、产品、研究任务。
- Entity：人、公司、产品、工具、技术、概念。
- Action：待读、待整理、待确认、待实践、待复盘。
- Secret：密码、账号、密钥、隐私文档。
- Record：消费、账单、邮件、人际关系、个人事件等持续记录。

第一版优先级：

- 核心：Source、Topic、Project、Action。
- 轻量支持：Entity 自动抽取和关联。
- 暂缓深化：Secret 只做敏感标记、索引和人工确认，不做加密。
- 新增：Record 作为个人持续记录对象，MVP 仅预留数据模型；V1 再支持 MCP 写入、手动录入、分类和总结。

## 9. Source Note Frontmatter

参考 LLM Wiki 的可追溯思想，Source Note 需要清楚记录来源、状态、处理过程和被整合到哪里。

建议字段：

```yaml
---
id: source_YYYYMMDD_000001
type: source
title: ""
source_type: webpage
source_url: ""
source_platform: ""
author: ""
created_at: YYYY-MM-DD
collected_at: YYYY-MM-DD
imported_from: manual
local_path: ""
content_hash: ""
language: zh
status: pending_review
risk_level: low
priority: normal
projects: []
topics: []
entities: []
tags: []
actions: []
summary: ""
agent_pipeline_version: v0.2
agent_status: classified
review_required: true
review_reason: ""
integrated_pages: []
related_sources: []
duplicate_of: ""
confidence: 0.0
---
```

字段说明：

- `id`：全局唯一 Source ID。
- `type`：笔记类型，Source Note 固定为 `source`。
- `source_type`：资料类型，如 webpage、video、pdf、word、markdown、image、chat、file。
- `source_platform`：知乎、B站、飞书、腾讯文档、印象笔记、微信、本地文件等。
- `content_hash`：用于去重。
- `status`：处理状态，如 pending、classified、pending_review、archived、rejected。
- `risk_level`：low、medium、high。
- `projects/topics/entities/tags`：多维归类。
- `actions`：Agent 建议产生的行动项。
- `integrated_pages`：该资料已整合进哪些 Wiki 页面。
- `review_required` 和 `review_reason`：是否需要人工审核及原因。
- `confidence`：Agent 对分类/整合建议的置信度。

## 10. Agent 流水线

第一版采用流水线模式：

1. Ingest 收集 Agent：接收链接、文本、文件、本地目录和第三方导出内容。
2. Normalize 清洗 Agent：提取标题、正文、来源、时间、作者、链接和附件信息。
3. Classify 分类 Agent：判断资料类型、项目、主题、标签、优先级和敏感级别。
4. Integrate 整合 Agent：生成 Source Note 和 Wiki 更新草案。
5. Review 审核 Agent：检查重复、过期、冲突、可信度和缺失上下文。
6. Archive 归档 Agent：写入 Obsidian Vault，并更新 MySQL 状态。

第一版底层可以是单 LLM + 多步骤 Agent 角色，但数据库、日志和 UI 必须按 Agent 节点记录，方便未来替换成真实多 Agent。

## 11. Agent 输出

每条资料处理后需要输出：

- 一句话摘要。
- 结构化摘要。
- 关键观点。
- 分类和标签。
- 项目/主题/实体关联。
- 质量检查结果。
- 重复/冲突/过期判断。
- Wiki 整合建议。
- Obsidian Markdown 草案。
- HTML 预览内容。
- HTML 报告导出产物。
- 是否需要人工审核。
- 处理日志。

Markdown 是主知识源，HTML 只用于 Web UI 预览和报告导出，不作为长期知识库替代格式。

## 12. UI 看板

第一版需要独立 Web UI，不需要登录。

核心页面：

- 收集入口页：粘贴链接、文本，上传文件，选择项目/主题。
- 路径导入页：配置指定路径，扫描 Word、Markdown、JPG、PDF 等常见文件。
- 待审核队列：查看摘要、分类、Markdown 草案、质量检查结果，确认/驳回/修改。
- 资料库列表：查看全部 Source，按来源、状态、标签、项目筛选。
- 项目看板：按项目查看资料、任务、知识页和待补充内容。
- Agent 运行日志：查看每条资料经过哪些步骤，每步输出什么。
- 办公室视图：展示 6 个 Agent 工位及状态，并可点击查看任务、日志、失败原因和输出。
- Obsidian 文件预览页：在 Web UI 内渲染 Obsidian Markdown，并支持一键通过 `obsidian://open` 打开本地 Obsidian。
- 系统设置页：配置 Obsidian Vault 路径、MySQL、模型供应商、CC Switch、目录规则。

功能优先级：

1. 收集入口页
2. 路径导入页
3. 待审核队列
4. 资料库列表
5. Agent 日志
6. 办公室视图（V2）

办公室视图作为 V2 拓展能力，采用可操作型设计：

- 展示收集、清洗、分类、整合、审核、归档 6 个 Agent。
- Agent 状态包括空闲、工作中、等待审核、阻塞、失败、完成。
- 点击 Agent 可查看当前任务、历史任务、日志、失败原因和输出。
- 支持对任务进行重跑、跳过、进入审核、查看输出等基础操作。
- 不做复杂拖拽调度、手动分配任务或并行编排编辑器。

## 13. 输入来源与格式

MVP 预计支持：

- 手动输入文本。
- 粘贴链接。
- 指定路径扫描导入。
- Word 文档。
- Markdown 文件。
- PDF 文件。
- JPG 图片。
- 常见图片格式。
- 本地文件夹。

后续扩展：

- 飞书文档。
- 腾讯文档。
- 印象笔记。
- 微信收藏。
- B站收藏。
- 知乎收藏。
- 浏览器插件收藏入口。

## 14. 模型与调用

第一版支持国内模型优先：

- DeepSeek
- MiniMax
- 其他兼容 OpenAI API 格式的模型供应商

模型切换通过 CC Switch 管理。系统侧需要把模型调用抽象成 Provider Adapter，不把 Agent 流程绑定到单个模型。

## 15. 非目标

第一版暂不做：

- 登录和多用户权限。
- 密码/密钥加密管理。
- 企业级权限审计。
- 全自动第三方平台同步。
- 复杂并行 Agent swarm。
- Obsidian 插件开发。
- 移动端 App。
- 不把向量库作为第一版强依赖。

## 15.1 向量库后续规划

知识熔炉 WikiForge 的长期目标不是只做资料归档，而是把分散知识提炼成可被检索、问答和 Agent 调用的知识资产。向量库是后续规划中的重要组成部分。

第一版需要为向量库预留并逐步落地：

- Source Note、Topic、Project、Entity 等内容的可分块结构。
- 每个分块的来源、路径、hash、更新时间和关联对象。
- embedding 生成状态，如未生成、待更新、已生成、失败。
- 批量导出接口，将整理后的 Markdown/Wiki 内容输出给向量化任务。
- 未来接入向量库时，仍以 Obsidian Wiki 和 Source Note 作为可读事实来源。

R6-1 已先完成可落地的导出契约：从 `source_contents.raw_text` 和 `personal_records.raw_content` 生成 JSONL 文件，并在 MySQL 中记录 `vector_export_jobs` 与 `content_chunks`。本轮暂不读取 Obsidian Markdown、不生成 embedding、不接真实向量库。

R6-3 已补充首版知识维护巡检：通过 `POST /api/v1/maintenance-runs` 手动触发，结果写入 `knowledge_maintenance_runs` 与 `knowledge_maintenance_items`，Dashboard 可查看运行记录和问题列表。首版只做发现和展示，不自动修改用户资料。

R6-3.1 补充维护问题处理闭环：对 `knowledge_maintenance_items` 增加处理状态、处理备注、处理人和处理时间。用户可以在 Dashboard 将问题标记为已解决、忽略或重新打开。重新打开本质上把状态恢复为 `open`，本轮不做完整历史事件表、不做自动修复、不删除资料、不改写 Obsidian。

第一版不要求完成向量检索，但文档结构和数据库模型不能阻碍后续批量向量化。

## 15.2 MCP 能力规划

知识熔炉 WikiForge 需要支持 MCP，作为系统与外部 Agent、工具和资料源交互的标准扩展层。

MCP 在系统中的定位：

- 对外暴露 WikiForge 能力，让外部 Agent 可以查询资料、创建 Source、读取项目/主题、触发导入或归档任务。
- 对内接入外部工具和资料源，例如文件系统工具、浏览器工具、第三方文档工具、未来向量库工具。
- 作为 Agent 工具调用的标准接口，降低后续扩展成本。

MVP5 实现轻量 MCP HTTP Preview，详细契约以 `docs/current/MCP接口契约-mcp-api-contract.md` 为准：

- 提供 WikiForge MCP 能力的最小工具集，暴露查询 Source、创建 Source、读取 Source、读取 Obsidian Note 和写入个人记录等基础能力。
- 预留 MCP Client 能力，用于后续连接外部 MCP Server。
- 记录 MCP 工具调用日志，便于排查 Agent 行为。
- 工具不返回本地绝对路径，不读取未登记文件，不做文件删除、移动或任意路径扫描。

第一版不要求完成复杂 MCP Marketplace、权限系统或多租户 MCP 管理。

## 16. 成功标准

第一版成功标准：

- 能通过 UI 输入链接、文本或文件路径。
- 能扫描指定路径并识别常见文件。
- 能生成 Source Note。
- 能进行摘要、分类、标签、项目和主题建议。
- 能生成 Obsidian Wiki 更新草案。
- 能在 UI 中审核并确认归档。
- 能将结果写入 MySQL 和 Obsidian Vault。
- 能在 UI 中查看 Agent 处理日志。
- 能在办公室视图中看到 Agent 状态并执行基础操作。
- 能在 Web UI 中预览 Obsidian Markdown，并一键打开 Obsidian。
- 能生成 HTML 预览或报告导出产物。
- 能为后续向量化保留来源、分块和状态信息。
- 能通过 MCP 暴露基础查询和任务触发能力。
- 能区分自动归档和人工审核资料。
