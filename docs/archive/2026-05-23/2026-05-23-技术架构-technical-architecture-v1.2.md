# 知识熔炉 WikiForge 技术架构 v1.2

## 1. 架构目标

本系统需要同时满足：

- 本地优先运行。
- 支持从多源分散资料到整理、归档、提炼、复用的完整闭环。
- 第一版优先解决源文件归集整理和基础归档。
- 前后端分离架构，便于后续维护和扩展。
- 后端采用少服务微服务模式，优先拆分 Core Service、Worker Service，并新增独立 Orchestration 辅助服务。
- 后端主要采用 Java 技术栈。
- 技术选型优先采用国内开发主流、成熟、维护活跃的框架和 jar 包，避免生僻冷门技术。
- Obsidian Vault 可直接读写。
- Raw Sources 原始资料库可统一归集源文件。
- MySQL 作为控制平面、索引库和 Agent 流程账本。
- 独立 Web UI 看板。
- 办公室视图展示并操作 Agent 状态。
- Web UI 可预览 Obsidian 文件，并通过 Obsidian URI 打开本地文件。
- Markdown 作为主知识源，HTML 作为预览和报告导出产物。
- 为后续批量导入向量库预留分块、导出和 embedding 状态管理。
- 支持 MCP Server / MCP Client 扩展能力。
- 支持 OpenClaw / Hermes 机器人通过 MCP 写入个人记录。
- Agent 流水线可追踪、可恢复、可扩展。
- 支持独立开发编排辅助工程，用于长期维护任务、Agent 状态、分支、验证和 Handoff。
- 支持 DeepSeek、MiniMax 等国内模型，并通过 CC Switch 切换。
- 后续可从本地应用迁移到私有服务器部署。
- 支持 CI/CD 自动构建、测试、镜像打包和 Docker 发布，避免后续因无法自动迭代而反改架构。

实施优先级：

- MVP 优先实现本地源文件整理、MySQL 索引、Obsidian Source Note 和基础 Web UI。
- 在线文档连接器、办公室等距视图、完整 MCP、个人记录、向量库和维护 Agent 作为后续拓展。
- 架构需要预留接口和数据模型，但不能让 MVP 被长期能力拖重。

## 2. 总体架构

```text
User
  |
  v
WikiForge UI
  |
  v
Core Service
  |
  +--> Settings
  +--> Source / SourceFile / ImportJob
  +--> Review Queue
  +--> Obsidian Note Mapping
  +--> Search API
  +--> Worker Task API
  |
  +--> Worker Service
        |
        +--> Import Worker
        +--> Raw Source Organizer
        +--> File Hash Service
        +--> File Type Detector
        +--> Parser Worker
  |
  +--> MySQL
  +--> Obsidian Vault
  +--> Raw Sources
  +--> Local File System

WikiForge Orchestration UI
  |
  v
Orchestration Service
  |
  +--> Task Control Plane
  +--> Agent State Board
  +--> Workspace / Branch Registry
  +--> Verification / Handoff Log
  +--> GitHub Issue Sync (later)

Later Services
  |
  +--> Link Ingestion Service
  +--> Connector Gateway
  +--> Agent Orchestrator
  +--> Agent Office Service
  +--> Artifact Service
  +--> Vector Export Service
  +--> MCP Service
  +--> Personal Record Service
  +--> Model Provider Adapter
```

架构按两层知识能力组织：

- LLM Wiki 表达层：Raw Sources、Obsidian Vault、Source Note、Wiki 页面、Schema、index、log。
- GBrain 运行层：MySQL 状态账本、Agent Orchestrator、MCP Service、Vector Export Service、Personal Record Service、定时任务和外部机器人接入。

表达层保证知识稳定、可读、可编辑；运行层保证知识可以被持续调用、更新、总结和复用。

### 2.1 少服务微服务落地策略

用户已选择 B 方案：MVP 采用少服务微服务模式，而不是完整大微服务体系。

MVP 0/1 目标运行服务：

```text
wikiforge-ui
wikiforge-core-service
wikiforge-worker-service
wikiforge-orchestration-service
wikiforge-orchestration-ui
mysql
```

后续逐步拆分：

```text
wikiforge-gateway
wikiforge-agent-service
wikiforge-connector-service
wikiforge-mcp-service
wikiforge-vector-service
wikiforge-record-service
```

约束：

- MVP 0/1 不引入 Nacos、Kafka、Redis、XXL-JOB、Service Mesh。
- 服务拆分先服务于领域边界和多人协作，不为了形式上微服务而制造分布式复杂度。
- MySQL MVP 阶段可以共享实例，但数据表必须明确服务归属。
- 跨服务先使用 REST 和清晰 DTO；消息队列等异步基础设施后续再评估。

### 2.2 AI 开发 Skill 分层

WikiForge 吸收 CDP AI 开发范式中的分层思想，建立项目内 Skill：

```text
docs/ai-skills/wikiforge-development/SKILL.md
```

该 Skill 分为：

- 全局入口：`SKILL.md`，约束所有 AI 和开发者开始工作前的读取顺序。
- 架构样式：`architecture-style.md`。
- 服务边界：`service-boundaries.md`。
- 后端 DDD 标准：`backend-ddd-standard.md`。
- 前端标准：`frontend-standard.md`。
- CI/Docker 标准：`ci-docker-standard.md`。
- 多人和多 AI 协作：`multi-agent-collaboration.md`。

后续多人开发时，开发者或 AI 必须先声明目标服务、预计修改文件、依赖契约和验证命令。

## 3. 核心模块

### 3.1 Web UI Dashboard

职责：

- 提供资料投喂入口。
- 提供指定路径扫描入口。
- 展示待审核队列。
- 展示资料库和处理状态。
- 展示 Agent 日志。
- 展示办公室视图。
- 预览 Obsidian Markdown。
- 通过 `obsidian://open` 打开 Obsidian 文件。
- 展示 HTML 预览和报告。
- 配置 Obsidian Vault、模型供应商、MySQL 和目录规则。

第一版不需要登录。

Web UI 长期采用双视图；MVP 先实现 Console 视图，办公室视图放到 V2：

- 办公室视图：以 6 个 Agent 工位展示状态和任务，可点击查看日志、输出、失败原因，并执行重跑、跳过、进入审核等基础操作。
- Console 视图：传统列表、审核、资料库、日志和系统设置。

### 3.2 Core Service

职责：

- 为 UI 提供 REST API。
- 管理 Source、Project、Topic、Action 等核心对象。
- 触发导入任务，并调用 Worker Service 执行耗时文件任务。
- 查询处理结果、审核状态和日志。
- 管理系统配置、模型供应商配置、Obsidian Vault 映射和审核队列。

Core Service 是 MVP 对外业务 API 入口。UI 不直接调用 Worker 内部接口。

### 3.2.1 Worker Service

职责：

- 执行本地路径扫描。
- 复制和归集 Raw Sources 文件。
- 计算 SHA-256 hash。
- 识别文件类型、大小和基础元数据。
- 后续执行文档解析、OCR 前置处理、Source Note 草案生成前置处理。
- 将任务结果回写 Core Service 管理的 ImportJob / Source / SourceFile 状态。

Worker Service 不承载 UI 查询职责，不直接对外暴露用户操作入口。

### 3.3 Import Service

职责：

- 接收手动文本、上传文件。
- 扫描指定本地路径。
- 识别文件类型。
- 提取基础元数据。
- 生成待处理 Source 记录。
- 将扫描结果交给 Raw Source Organizer 进行归集整理。

第一版支持格式：

- Word
- Markdown
- PDF
- JPG
- 常见图片
- 普通文本
- 本地文件夹

解析策略：

- Markdown：直接读取正文和 frontmatter。
- Word：提取正文、标题、元数据。
- PDF：提取文本，必要时标记为 OCR 待处理。
- 图片/JPG：第一版可先保存文件引用和元数据，后续接 OCR。
- 链接：交给 Link Ingestion Service 识别和读取。

### 3.3.1 Link Ingestion Service

职责：

- 接收用户输入的飞书文档、腾讯文档、普通网页文档等在线文档链接。
- 识别链接平台和文档类型。
- 调用 Connector Gateway 读取在线文档内容。
- 保存原始链接、标题、正文、作者、更新时间、权限状态和附件信息。
- 创建 Source 记录，并将内容交给 Agent 流水线生成知识卡。

在线文档处理流程：

```text
用户输入在线文档链接
  -> Link Ingestion Service 识别平台
  -> Connector Gateway 选择 OpenClaw / Hermes / Native Connector
  -> 读取文档内容和元数据
  -> 创建 Source
  -> Normalize / Classify / Integrate Agent 处理
  -> 生成知识卡和 Obsidian 草案
  -> Review 队列确认
  -> Archive 归档
```

异常处理：

- 无访问权限：记录为 permission_denied，进入待处理队列。
- 链接无效：记录为 invalid_url。
- 连接器失败：记录 connector_error，并允许重试。
- 内容为空：记录 empty_content，等待人工确认。

### 3.3.2 Connector Gateway

职责：

- 为不同在线文档读取能力提供统一接口。
- 支持 OpenClaw Connector、Hermes Connector 和 Native Connector。
- 屏蔽不同服务的认证、读取、错误格式和返回结构差异。
- 输出标准化文档对象，供后续 Agent 使用。

标准化输出：

```text
title
source_url
source_platform
document_type
author
updated_at
content_text
attachments
permission_status
connector_name
connector_trace_id
```

### 3.3.3 Raw Source Organizer

职责：

- 将散落在多个路径的源文件归集到统一 Raw Sources 目录。
- 按文件类型、项目、来源、时间或用户规则进行初步整理。
- 计算文件 hash，识别重复文件。
- 记录原始路径、新路径、复制状态、分类结果和错误信息。
- 将整理后的资料标记为待提炼、待确认或仅归档。

第一版整理策略：

- 默认采用复制模式，保留原文件不删除。
- 不确定分类的文件放入待确认目录。
- 重复文件不重复复制，只建立重复关系。
- 源文件整理和知识提炼分成两个阶段，用户可以先只整理源文件。

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

### 3.4 Orchestration Service

职责：

- 管理 WikiForge 开发过程中的任务控制平面。
- 展示和记录任务、Agent、分支 / worktree、验证命令、Handoff Packet 和下一步动作。
- 为后续 Codex、OpenClaw、Hermes 或其他 Agent 提供统一接入入口。
- 长期支持 GitHub Issue 同步、任务重试、runner 注册和执行日志。

边界：

- Orchestration Service 管开发编排，不承载 Source / Obsidian / AI 审核等知识库业务能力。
- 第一版只做只读任务清单、任务详情和状态统计。
- 第一版不自动执行本机命令，不自动创建或关闭 GitHub Issue。
- 端口默认 `8090`。

第一版 API：

```text
GET /api/health
GET /api/v1/orchestration/overview
GET /api/v1/orchestration/tasks
GET /api/v1/orchestration/tasks/{taskId}
```

### 3.4.1 Orchestration UI

职责：

- 独立展示任务看板、当前阶段、任务状态、验证命令和 Handoff 要求。
- 作为用户和后续 Agent 的“开发控制台”。
- 后续可扩展为 Agent 办公室视图、runner 状态、日志流和失败重试入口。

边界：

- 独立于主业务 `wikiforge-ui`。
- 第一版默认端口 `3001`。
- 第一版只调用 Orchestration Service，不直接调用 Core / Worker。

### 3.5 Agent Orchestrator

职责：

- 负责编排 Agent 流水线。
- 记录每个 Agent 节点输入、输出、状态、错误。
- 根据风险级别决定自动归档或进入人工审核。

第一版流程：

```text
Ingest -> Normalize -> Classify -> Integrate -> Review -> Archive
```

每个节点必须生成结构化输出，并写入 `agent_runs` 和 `agent_steps`。

Agent Orchestrator 同时承担两类任务：

- 知识编译任务：将原始资料编译为 Source Note、Topic、Project、Entity、Record 等 Wiki 页面。
- 知识运行任务：定时巡检、批处理、MCP 写入处理、个人记录总结、向量化导出准备。

### 3.5.1 Agent Office Service

职责：

- 聚合每个 Agent 的当前状态。
- 为办公室视图提供 Agent 工位、任务、日志和错误信息。
- 支持基础操作：重跑任务、跳过失败步骤、进入审核、查看输出。

V2 办公室视图预留 Agent：

- Ingest 收集 Agent
- Normalize 清洗 Agent
- Classify 分类 Agent
- Integrate 整合 Agent
- Review 审核 Agent
- Archive 归档 Agent

Agent 状态：

- idle
- running
- waiting_review
- blocked
- failed
- completed

### 3.6 Model Provider Adapter

职责：

- 屏蔽不同模型供应商差异。
- 支持通过 CC Switch 切换 DeepSeek、MiniMax 等模型。
- 提供统一调用接口给 Agent。

建议抽象：

```text
generate_text(prompt, schema, model_config)
generate_json(prompt, json_schema, model_config)
```

第一版重点是结构化 JSON 输出稳定性。

MVP4 首轮落地：

- Core Service 直接封装 OpenAI-compatible HTTP 调用。
- Provider 不写死在业务逻辑中，通过 `providerName`、`providerType`、`baseUrl`、`modelName` 和环境变量共同决定。
- 通用环境变量约定：`WIKIFORGE_MODEL_<PROVIDER>_API_KEY`、`WIKIFORGE_MODEL_<PROVIDER>_BASE_URL`、`WIKIFORGE_MODEL_<PROVIDER>_MODEL`、`WIKIFORGE_MODEL_<PROVIDER>_TYPE`。
- Spring 配置约定：`wikiforge.model.providers.<provider>.api-key`、`base-url`、`model`、`type`。
- MiniMax 默认 Base URL：`https://api.minimax.io/v1`。
- 调用路径：`/chat/completions`。
- 配置变量：`WIKIFORGE_MINIMAX_API_KEY`、`WIKIFORGE_MINIMAX_BASE_URL`、`WIKIFORGE_MINIMAX_MODEL`。
- 密钥只从环境变量或本机 `.env` 读取，不写入仓库、日志和文档。
- 未配置密钥、未配置模型或调用失败时，使用本地规则生成待审核草案，避免 MVP4 主链路不可用。

### 3.7 Review Service

职责：

- 管理待审核队列。
- 展示 Agent 建议、分类、摘要、质量检查和 Markdown 草案。
- 支持确认、驳回、修改后确认。
- 保存审核意见和最终决策。

审核触发条件：

- `risk_level` 为 medium 或 high。
- Agent 置信度低。
- 检测到重复或冲突。
- 资料属于敏感资产。
- 资料关联重要项目。

### 3.8 Obsidian Writer

职责：

- 根据模板生成 Source Note。
- 更新 Topic / Project / Entity 页面。
- 维护索引页和 Agent 日志页。
- 保证文件路径、文件名和 frontmatter 规范。
- 生成 Obsidian URI，供 Web UI 一键打开。

写入策略：

- Source Note 新建为主，避免覆盖原始来源记录。
- Wiki 页面更新需要先生成草案。
- 自动写入只允许低风险资料。
- 高风险或不确定内容必须通过审核后写入。

### 3.8.1 Obsidian Preview

职责：

- 从 Vault 读取 Markdown 文件。
- 渲染为 Web UI 可展示的 HTML。
- 提供文件元数据、frontmatter、关联 Source 和打开 Obsidian 的 URI。

打开方式：

```text
obsidian://open?vault=<vault_name>&file=<vault_relative_path>
```

### 3.8.2 Artifact Service

职责：

- 管理 Agent 生成的非主知识源产物。
- 生成 HTML 预览。
- 生成 HTML 报告，如项目报告、资料分析报告、阶段总结。
- 记录 artifact 路径、类型、来源和生成时间。

规则：

- Markdown / Obsidian Wiki 是主知识源。
- HTML 用于 Web UI 预览和报告导出。
- HTML 不作为长期知识库替代格式。

### 3.9 Search Service

第一版以数据库筛选和 Obsidian 搜索为主。

支持：

- 按标题、标签、项目、主题、来源、状态筛选。
- 按处理状态查找资料。
- 按文件路径查找导入结果。

向量检索是明确的后续规划，但不作为第一版核心依赖。

### 3.10 Vector Export Service

职责：

- 将整理后的 Source Note、Topic、Project、Entity 页面切分为可向量化的文本块。
- 记录每个文本块的来源、Obsidian 路径、hash、更新时间和关联对象。
- 批量导出待向量化内容。
- 管理 embedding 生成状态，方便后续接入向量库。

第一版可以只完成分块和导出，不要求接入具体向量数据库。

Vector Export Service 属于 GBrain 运行层。它不替代 Obsidian Wiki，而是把稳定 Wiki 内容和结构化记录转换成可被 Agent 检索和调用的运行态知识。

后续可接入：

- Milvus
- Qdrant
- Chroma
- pgvector
- 其他本地或私有部署向量库

### 3.11 MCP Service

职责：

- 提供 WikiForge MCP Server，将系统能力暴露给外部 Agent 和工具。
- 预留 MCP Client，用于连接外部 MCP Server。
- 管理 MCP 工具定义、调用日志和错误信息。
- 将 MCP 调用映射到 Core Service、Worker Service、Search Service、Agent Orchestrator 等内部服务。

MVP 仅预留 MCP 相关配置和日志。MVP 4 可暴露的轻量 MCP tools：

- `search_sources`：按关键词、项目、主题、标签、状态查询 Source。
- `get_source`：读取 Source 元数据、摘要和关联 Obsidian Note。
- `create_import_job`：创建文本、链接或路径导入任务。
- `get_agent_status`：查询办公室视图中的 Agent 状态。
- `get_review_items`：查询待审核队列。
- `get_obsidian_note`：读取 Obsidian Note 的元数据和 Markdown 内容。
- `open_obsidian_note`：返回可打开的 `obsidian://open` URI。
- `create_personal_record`：写入消费、账单、邮件、人际关系或个人事件。
- `search_personal_records`：查询个人记录。

MVP / V1 MCP 范围控制：

- 优先实现本系统 MCP Server。
- MCP Client 先做配置预留和少量验证，不作为核心闭环依赖。
- 不做 MCP Marketplace。
- 不做多用户 MCP 权限系统。
- MCP 工具调用必须写入日志。

### 3.12 Personal Record Service

职责：

- 管理非文档型个人记录。
- 接收 Web UI 手动录入。
- 接收 OpenClaw / Hermes 机器人通过 MCP 写入的记录。
- 将消费、账单、邮件、人际关系和个人事件标准化为统一 Record。
- 触发 Agent 对记录进行分类、归纳、总结和行动项提取。
- 将重要记录输出为 Obsidian 日记、关系页、账单总结、邮件摘要或项目记录。

支持记录类型：

- expense：消费记录。
- bill：账单记录。
- email：邮件记录。
- relationship：人际关系记录。
- event：个人事件。

处理原则：

- 所有记录必须保留来源、时间、写入方和原始内容。
- 财务、人际关系、邮件类记录默认支持敏感标记。
- Agent 生成总结前需要保留原始记录，避免只存摘要。

## 4. 数据流

### 4.1 手动输入流程

```text
用户输入文本/链接
  -> Core Service 创建 Source
  -> Agent Orchestrator 执行流水线
  -> 生成摘要、分类、草案
  -> 自动归档或进入 Review
  -> Obsidian Writer 写入 Vault
  -> MySQL 更新状态
  -> UI 可预览 Markdown 或 HTML artifact
```

### 4.2 指定路径导入流程

```text
用户配置本地路径
  -> Import Service 扫描文件
  -> 计算 content_hash
  -> Raw Source Organizer 复制到 Raw Sources 规范目录
  -> 跳过重复文件或标记重复
  -> 提取元数据和正文
  -> 创建 Source
  -> 用户决定是否进入 Agent 提炼加工流水线
```

### 4.3 在线文档链接采集流程

```text
用户输入飞书/腾讯文档等在线文档地址
  -> Link Ingestion Service 识别平台
  -> Connector Gateway 调用 OpenClaw / Hermes / Native Connector
  -> 读取文档内容和元数据
  -> 创建 Source 和采集记录
  -> Agent 流水线生成知识卡
  -> 进入审核或自动归档
```

### 4.4 审核归档流程

```text
Review 队列
  -> 用户查看 Agent 建议
  -> 用户确认/修改/驳回
  -> Archive Agent 写入 Obsidian
  -> 更新 Source 状态和日志
```

### 4.5 知识复用流程

```text
已整理 Source / Obsidian Note
  -> Search Service 检索
  -> UI 看板按项目、主题、标签展示
  -> Artifact Service 生成 HTML 报告
  -> Vector Export Service 批量生成分块
  -> 后续导入向量库
  -> MCP Service 暴露给外部 Agent 调用
```

### 4.5.1 LLM Wiki 到 GBrain 的运行反馈

```text
Raw Sources / Personal Records
  -> Agent 编译成 Obsidian Wiki / Source Note
  -> MySQL 记录结构化状态和关系
  -> Vector Export Service 生成分块
  -> 向量库提供语义检索
  -> MCP Service 提供工具调用
  -> OpenClaw / Hermes / Agent 使用知识
  -> 运行结果反馈为新记录、新任务或 Wiki 更新建议
```

该流程保证 WikiForge 不是一次性整理工具，而是持续运行的个人知识系统。

### 4.6 个人记录写入流程

```text
OpenClaw / Hermes 机器人
  -> 调用 WikiForge MCP create_personal_record
  -> Personal Record Service 标准化记录
  -> MySQL 保存结构化 Record
  -> Agent 分类、归纳、提取行动项
  -> 必要时进入审核队列
  -> 写入 Obsidian 个人记录页 / 日记 / 关系页 / 总结页
```

## 5. Obsidian 写入规范

目录采用混合结构：

```text
00_Inbox_收集箱/
01_Projects_项目/
02_Areas_领域/
03_Resources_资源/
04_Archives_归档/
05_Actions_行动/
06_Secrets_敏感资产/
07_Records_个人记录/
90_System_系统/
```

文件命名建议：

- Source Note：`YYYY-MM-DD - 标题.md`
- Project Page：`项目名称.md`
- Topic Page：`主题名称.md`
- Entity Page：`实体名称.md`
- Agent Log：`YYYY-MM-DD - source_id.md`

写入要求：

- 所有 Source Note 必须包含 frontmatter。
- 所有 Agent 生成内容必须记录来源。
- Wiki 页面引用 Source Note，而不是直接丢失来源。
- Agent 更新页面时需要记录变更摘要。

## 6. 技术选型建议

### 6.0 技术栈总览

MVP 技术选型原则：

- 以 Java + Spring Boot + MySQL + Vue 为主线。
- 优先国内开发团队熟悉、资料充足、维护活跃的框架。
- MVP 避免引入冷门 Agent 框架、复杂工作流引擎和过重基础设施。
- 对 V1/V2 的 MCP、向量库、办公室视图、个人记录等能力预留接口和数据模型。

推荐技术栈：

| 层级 | MVP 推荐 | 后续可选 | 说明 |
| --- | --- | --- | --- |
| 后端语言 | Java 21 LTS，Java 17 可兼容 | - | 优先 Java 21，新项目更适合长期维护 |
| 后端框架 | Spring Boot 3.x | Spring AI 可在 V1/V2 评估 | MVP 不依赖复杂 AI 框架 |
| Web API | Spring MVC / Spring Web | WebFlux 仅在高并发流式场景评估 | MVP 用同步 REST API 更简单 |
| 数据访问 | MyBatis-Plus 或 MyBatis | JPA 不作为首选 | 贴合国内 Java 团队习惯 |
| 数据库 | MySQL 8.x | PostgreSQL 后续可评估 | 用户已确定 MySQL |
| 数据库连接池 | HikariCP | - | Spring Boot 默认成熟方案 |
| 数据库迁移 | Flyway | Liquibase 可选 | 用版本化 SQL 管理 DDL |
| 前端框架 | Vue 3 + Vite + TypeScript | React 可选但不推荐作为主线 | 国内后台系统 Vue 生态更顺手 |
| UI 组件 | Element Plus | Ant Design Vue | 优先 Element Plus |
| 前端状态 | Pinia | - | Vue 3 主流状态管理 |
| HTTP 客户端 | Axios | fetch | 便于统一拦截器和错误处理 |
| Markdown 预览 | markdown-it 或 marked + DOMPurify | - | UI 内预览 Obsidian Markdown |
| 文档解析 | Apache POI 5.5.1、Apache PDFBox 3.0.7、Java UTF-8 文本读取 | Apache Tika 可作为聚合解析层 | MVP3 先用明确依赖，Tika 后续评估 |
| 图片元数据 | metadata-extractor | OCR 后续接入 | MVP 图片先做元数据和文件引用 |
| JSON/YAML | Jackson、Jackson YAML 或 SnakeYAML | - | frontmatter 和配置解析 |
| 模型调用 | Java HTTP Client / Spring RestClient 封装 Provider Adapter | Spring AI 后续评估 | MVP 直接对接 OpenAI-compatible API |
| 任务调度 | Spring Scheduler | XXL-JOB、Spring Batch | MVP 用内置调度，批处理复杂后升级 |
| MCP | 预留 schema 和日志；MVP4 使用官方 Java SDK | Spring AI MCP 后续评估 | 不让 MCP 阻塞 MVP |
| 日志 | SLF4J + Logback | JSON 日志后续增强 | 必须记录导入、Agent、MCP 调用 |
| 监控 | Spring Boot Actuator | Prometheus/Grafana 后续 | MVP 本地运行先做健康检查 |
| 后端测试 | JUnit 5、Mockito | Testcontainers | Testcontainers 可用于 MySQL 集成测试 |
| 前端测试 | Vitest | Playwright | MVP 可先做关键组件/工具函数测试 |
| 构建 | Maven | Gradle 可选 | 国内 Java 项目 Maven 更普遍 |
| 部署 | 本地启动 + Docker Compose + CI/CD 镜像打包 | NAS / 私有服务器 | Docker Compose 管理 MySQL 和应用，CI/CD 保证可自动迭代 |

### 6.1 后端

主要技术栈采用 Java。

建议：

- Java 21 LTS 优先，Java 17 可兼容。
- Spring Boot 3.x。
- Spring Web / Spring MVC 提供 REST API。
- Spring Validation 做参数校验。
- MyBatis-Plus 或 MyBatis 做数据库访问。
- Flyway 管理数据库 DDL 迁移。
- HikariCP 作为数据库连接池。
- Spring Boot Actuator 提供健康检查。
- Spring Scheduler 或 XXL-JOB 处理定时扫描和批处理任务。
- Maven 作为构建工具。

选型原则：

- 优先国内团队熟悉、资料充足、社区活跃的技术。
- 优先选择维护稳定、长期可用的 jar 包。
- 避免为了炫技引入冷门 Agent 框架、冷门工作流引擎或难维护依赖。
- LLM 和 Agent 编排第一版可以用自研轻量编排服务实现，后续再评估是否接入 LangGraph、OpenAI Agents SDK 等外部框架。

### 6.2 前端

建议：

- 前后端分离。
- Vue 3 + Vite + TypeScript。
- UI 组件库优先 Element Plus。
- 状态管理使用 Pinia。
- 路由使用 Vue Router。
- HTTP 请求使用 Axios。
- Markdown 预览使用 markdown-it 或 marked，并配合 DOMPurify 做 HTML 清理。
- UI 以本地看板、办公室视图、审核队列和资料库为主，不做复杂营销页面。

### 6.2.1 前后端边界

前端职责：

- 展示办公室视图、资料库、审核队列、Agent 日志。
- 发起导入、审核、重跑、跳过、打开 Obsidian 等操作。
- 渲染 Markdown 预览和 HTML 报告。

后端职责：

- 管理业务状态和数据库。
- 调度 Agent 流水线。
- 读写 Obsidian Vault。
- 解析文件和生成 artifact。
- 对接模型供应商和 CC Switch。

前后端通过 REST API 通信，办公室视图的 Agent 实时状态后续可通过 WebSocket 或 Server-Sent Events 增强。

### 6.3 数据库

第一版使用 MySQL。

定位：

- 控制平面。
- 结构化索引库。
- Agent 流程账本。
- 轻量内容缓存。

用途：

- Source 状态。
- Agent 运行日志。
- 分类、标签、项目、主题。
- 审核队列。
- 文件导入记录。
- Obsidian 文件映射和 Obsidian URI。
- HTML artifact 记录。
- 办公室视图 Agent 状态。

正文保存策略：

- 普通文本、网页、Markdown、小型 Word 可保存解析后的纯文本。
- PDF、大文件、图片、敏感资料默认只保存路径、摘要、hash 和元数据。
- 是否保存全文由资料类型、文件大小、敏感级别和用户规则决定。
- MySQL 不作为最终知识正文主库。
- MySQL 需要记录后续向量化所需的分块、hash 和 embedding 状态。

### 6.4 文件解析

建议能力：

- Markdown 解析。
- Word 文本提取。
- PDF 文本提取。
- 图片元数据读取。
- 后续 OCR。

Java 侧建议优先考虑成熟依赖：

- Markdown：MVP3 先使用 UTF-8 文本读取并去除 YAML frontmatter；复杂 Markdown AST 后续再评估 commonmark-java 或 flexmark-java。
- Word：Apache POI 5.5.1，MVP3 支持 `.docx` 段落文本抽取。
- PDF：Apache PDFBox 3.0.7，MVP3 支持基础文本抽取。
- 图片元数据：metadata-extractor。
- 文件类型识别：Java NIO + MIME 探测；Apache Tika 后续作为聚合解析层评估。
- 文件复制和 hash：Java NIO、MessageDigest，必要时使用 Apache Commons IO。
- JSON：Jackson。

OCR 可作为后续能力，优先选择本地可部署且维护活跃的方案。

### 6.5 模型调用

第一版通过模型适配层接入：

- DeepSeek
- MiniMax
- 兼容 OpenAI API 协议的供应商
- CC Switch

Java 侧模型调用建议先使用标准 HTTP Client 封装 Provider Adapter，不在第一版绑定冷门 AI 框架。

模型调用实现要求：

- 所有供应商通过统一 `ModelProviderAdapter` 调用。
- 请求必须设置超时、重试上限和失败日志。
- API Key 不写入代码仓库，优先从环境变量或本地配置读取。
- Agent 输出优先要求 JSON schema 结构化，失败时进入人工审核或重试队列。

MVP4 已落地的最小实现：

- Core Service 使用 Spring `RestClient` 调用 MiniMax OpenAI-compatible `/chat/completions`。
- MiniMax / DeepSeek / CC Switch 等配置来自环境变量，不在数据库和仓库中保存明文密钥。
- 本地规则 provider 作为兜底 provider，用于无密钥、无模型或外部调用失败时继续生成审核草案。

### 6.6 MCP 技术栈

MVP 阶段：

- 仅预留 MCP tool schema、配置项和调用日志表。
- 不阻塞源文件整理和 Obsidian 归档主流程。

MVP 5：

- 先完成 WikiForge Orchestration 辅助工程，保证后续 Agent 开发过程可视化。
- MCP 第一轮提供 HTTP Preview API，工具 schema 对齐 MCP 形态。
- 官方 MCP Java SDK 作为后续 transport 替换方向，当前不让 SDK 版本兼容风险阻塞 MVP5。
- 优先暴露 `create_source`、`search_sources`、`get_source`、`get_obsidian_note`、`create_personal_record`。
- `create_personal_record` 可以先只做结构化存储，不做复杂总结。

后续：

- 可评估 Spring AI MCP 集成，但不作为 MVP 依赖。
- MCP Client 先预留，V1/V2 再用于连接外部工具和资料源。

### 6.7 工程化与质量保障

后端工程化：

- 统一异常处理：Spring `@ControllerAdvice`。
- 参数校验：Spring Validation。
- API 文档：springdoc-openapi。
- 日志：SLF4J + Logback，导入任务、文件复制、Agent 调用、MCP 调用必须落日志。
- 数据库迁移：Flyway SQL migration。
- 单元测试：JUnit 5 + Mockito。
- 集成测试：关键数据库流程可用 Testcontainers MySQL。

前端工程化：

- ESLint + Prettier。
- TypeScript 严格模式。
- Vitest 覆盖关键工具函数和页面状态逻辑。
- Playwright 可在 MVP 后用于关键流程端到端测试。

本地部署：

- MVP 支持本地开发启动。
- 提供 Docker Compose 管理 Core Service、Worker Service、UI 和 MySQL。
- 后端、前端、MySQL、Raw Sources、Obsidian Vault 路径需要在配置中明确。

### 6.7.1 CI/CD 与 Docker 发布

CI/CD 和 Docker 发布需要作为 MVP 架构约束提前纳入，而不是后续补丁。

目标：

- 每次提交后可以自动执行后端测试、前端构建和基础质量检查。
- 每个可发布版本可以自动构建 Docker 镜像。
- Docker Compose 可以一键启动应用和 MySQL。
- Raw Sources、Obsidian Vault、日志、配置、MySQL 数据必须通过 volume 或外部路径持久化。
- 运行时配置不能写死在镜像中，必须通过环境变量或外部配置文件注入。

建议仓库结构：

```text
WikiForge/
  backend/
    pom.xml
    wikiforge-common/
    wikiforge-core-service/
    wikiforge-worker-service/
    wikiforge-orchestration-service/
    wikiforge-gateway/        # 后续预留
  frontend/
  orchestration-ui/
  deploy/
    docker/
      core-service.Dockerfile
      worker-service.Dockerfile
      orchestration-service.Dockerfile
      frontend.Dockerfile
      orchestration-ui.Dockerfile
    docker-compose.yml
    docker-compose.dev.yml
  .github/
    workflows/
      ci.yml
```

MVP 阶段建议先采用少服务微服务镜像：

- `wikiforge-core-service`：Spring Boot 核心业务 API 镜像。
- `wikiforge-worker-service`：Spring Boot 文件扫描和归集任务镜像。
- `wikiforge-orchestration-service`：Spring Boot 开发编排状态服务镜像。
- `wikiforge-ui`：Nginx 托管前端静态资源，并反向代理 `/api` 到 Core Service。
- `wikiforge-orchestration-ui`：Nginx 托管开发编排控制台静态资源。
- `mysql:8`：数据库容器，数据目录使用 volume 持久化。

不建议 MVP 阶段把前端静态资源打进 Spring Boot jar，原因：

- 前后端分离边界更清晰。
- Docker 镜像构建更容易定位问题。
- 后续 Web UI 独立迭代更方便。
- Nginx 代理更贴近私有服务器和 NAS 部署方式。

CI/CD 最小流水线：

1. 后端：`mvn test`。
2. 后端：`mvn package`。
3. 前端：`npm ci`。
4. 前端：`npm run build`。
5. Docker：构建 Core Service 镜像。
6. Docker：构建 Worker Service 镜像。
7. Docker：构建 Orchestration Service 镜像。
8. Docker：构建主业务前端镜像。
9. Docker：构建 Orchestration UI 镜像。
10. Docker Compose：做基础启动校验。

Docker 运行时配置：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `CORE_SERVER_PORT`
- `WORKER_SERVER_PORT`
- `WIKIFORGE_HOST_RAW_SOURCES_ROOT`
- `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH`
- `WIKIFORGE_RAW_SOURCES_PATH`
- `WIKIFORGE_OBSIDIAN_VAULT_PATH`
- `WIKIFORGE_OBSIDIAN_VAULT_NAME`
- `WIKIFORGE_ALLOWED_SCAN_ROOTS`
- `WIKIFORGE_MODEL_PROVIDER`
- `WIKIFORGE_MODEL_API_KEY`

容器卷挂载建议：

```text
./data/mysql:/var/lib/mysql
${WIKIFORGE_HOST_RAW_SOURCES_ROOT:-./data/raw-sources}:/data/wikiforge/raw-sources
${WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH:-./data/obsidian-vault}:/data/wikiforge/obsidian-vault
./data/logs:/app/logs
./config:/app/config
```

当前用户确认的本机 Obsidian Vault 宿主机路径为：

```text
E:\WikiForgeVault
```

本路径只作为本机部署和后续 MVP2 验收使用，不写入镜像；Docker 模式通过 `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH` 映射到容器内 `/data/wikiforge/obsidian-vault`。

数据库迁移策略：

- Flyway migration 随后端应用启动执行。
- CI 中必须校验 migration 可以在空库上成功执行。
- 后续如涉及破坏性 DDL，必须单独写迁移说明和回滚策略。

健康检查：

- 后端提供 `/actuator/health`。
- 前端 Nginx 提供静态资源健康检查。
- Docker Compose 配置 `healthcheck`。
- CI 中至少验证后端健康检查和前端首页可访问。

部署评审必须确认：

- Docker 镜像内不写死 Windows 本地路径。
- Obsidian Vault 和 Raw Sources 必须是外部挂载路径。
- 本地开发模式和 Docker 部署模式使用同一套配置语义。
- 前后端 API 地址通过环境变量或 Nginx 代理配置解决，不能写死 localhost。
- CI/CD 失败能暴露构建、测试、镜像和 Compose 启动问题。

安全和文件系统边界：

- 文件扫描必须由用户显式配置根路径。
- 所有扫描、复制、预览都必须校验路径在允许目录内，避免路径穿越。
- 默认复制源文件，不删除、不移动原始文件。
- 对大文件设置大小上限和跳过策略。
- 对敏感资料只做标记和人工审核，不默认进行明文提炼。

### 6.8 暂不引入的技术

MVP 暂不引入：

- Elasticsearch / OpenSearch。
- Redis。
- Kafka / RocketMQ。
- 完整工作流引擎。
- 完整 Spring AI Agent 框架。
- 完整向量数据库。
- OCR 引擎。
- 登录权限系统。
- Nacos / 服务注册中心。
- XXL-JOB / 分布式调度中心。

这些能力在源文件整理和 Obsidian 归档闭环跑通后再评估。

## 7. 部署形态

第一版：

- 本地电脑运行 Core Service、Worker Service 和前端。
- MySQL 本地或局域网部署。
- Obsidian Vault 指向本地目录。
- 文件扫描仅扫描用户配置路径。
- 提供 Docker Compose 一键启动应用、前端和 MySQL。
- 提供 CI/CD 流水线自动完成测试、构建和 Docker 镜像打包。

后续：

- 私有服务器部署。
- NAS 部署。
- 多端访问。
- 用户登录和权限系统。

## 8. 安全边界

第一版不做登录和加密，但需要保留风险标记：

- Secret 类型资料必须标记。
- 敏感资料默认进入人工审核。
- Agent 是否读取敏感明文由用户确认。
- 本地路径扫描必须由用户显式配置。

## 9. 可扩展点

- 将单 LLM 多步骤升级为真实多 Agent。
- 增加并行处理和专家会审。
- 增加向量检索。
- 批量导入向量库。
- 增加 OCR。
- 增加浏览器插件。
- 增加 MCP Client 接入外部工具和资料源。
- 增加飞书、腾讯文档、微信收藏等同步。
- 增加登录、权限和团队空间。

## 10. 实施阶段

### MVP 0：项目骨架

- Spring Boot Maven monorepo 后端工程。
- `wikiforge-common` 公共模块。
- `wikiforge-core-service` 核心业务服务。
- `wikiforge-worker-service` 文件任务服务。
- `wikiforge-orchestration-service` 开发编排辅助服务。
- Vue 3 前端工程。
- `orchestration-ui` 独立开发编排控制台。
- MySQL 初始化。
- 基础配置模块。
- Obsidian Vault 路径配置。
- Raw Sources 路径配置。
- CI/CD、Docker Compose 和各服务健康检查。

### MVP 1：源文件归集

- Core Service 创建和查询 ImportJob / Source / SourceFile。
- Worker Service 执行 Import Worker 和 Raw Source Organizer。
- Worker Service 完成文件扫描、复制、hash 去重。
- Core Service 负责 Source / SourceFile / ImportJob 状态管理。
- Web UI 文件列表和导入任务状态。

### MVP 2：Obsidian Source Note

- 已实现 Obsidian Vault 初始化 API：`POST /api/v1/obsidian/init`。
- 已实现 Source Note 草案 API：`POST /api/v1/source-files/{fileUid}/obsidian-note/draft`。
- 已实现 Source Note 写入 API：`POST /api/v1/source-files/{fileUid}/obsidian-note/write`。
- 已实现 Markdown 预览 API：`GET /api/v1/obsidian/notes/{noteUid}/preview`。
- 已实现 `obsidian_notes` 运行账本，Core Service 负责 Source / SourceFile 与 Vault Markdown 文件映射。
- 已实现服务端路径校验、Vault 内相对路径解析、临时文件写入和原子 rename。
- 已实现 `obsidian://open` 链接生成，Vault 名称和 Vault 内路径均进行 URL encode。
- 已实现 Web UI Source Note 抽屉，支持编辑草案、写入 Vault、读取预览和打开 Obsidian。
- `index / log` 系统文件暂未进入 MVP2 实现，后续随 Wiki 编译和维护 Agent 增补。

### MVP 4：AI 辅助整理

- 已实现 `agent_runs`、`agent_steps`、`review_items` 最小运行账本和审核队列。
- 已实现 `POST /api/v1/source-files/{fileUid}/ai-review-runs`，基于已解析正文生成结构化整理草案。
- 已实现 `GET /api/v1/ai-review-runs/{runUid}` 和 `GET /api/v1/review-items`。
- 已实现 MiniMax OpenAI-compatible 调用入口；未配置时保留本地规则兜底。
- 已实现 Web UI 审核队列和草案查看抽屉。
- 已实现 `POST /api/v1/review-items/{reviewUid}/approve`，人工确认后写入 Obsidian Source Note。
- 已实现 Web UI 审核通过入口，成功后刷新审核队列并打开写入后的 Source Note 预览。

### MVP 5：轻量 MCP 预览版

- MCP Service 基础框架。
- `create_source`。
- `search_sources`。
- `get_source`。
- `create_personal_record` 简化存储。
- MCP 调用日志。

### V1：在线资料与个人记录

- Link Ingestion Service。
- Connector Gateway。
- 飞书 / 腾讯文档读取。
- OpenClaw / Hermes 写入。
- Personal Record Service 完整化。

### V2：知识运行层

- Vector Export Service。
- 向量库接入。
- hybrid search。
- Lint / Maintain Agent。
- 办公室等距视图。
- 定时总结和长期记忆。
