# 2026-05-23 知识熔炉 WikiForge 参考项目清单 Reference Projects

## 用途

本文件单独记录本次需求讨论中提到的 Git / Gist / 开源项目参考。后续开发过程中，如需要实现 LLM Wiki、Agent 办公室视图、在线文档采集、多 Agent 编排、MCP 或知识库能力，可回到本文件查找对应参考。

## 用户明确提供的参考

### Karpathy LLM Wiki

- 链接：https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f#llm-wiki
- 类型：Gist / 设计思想参考
- 参考方向：
  - Raw Sources / Wiki / Schema 分层思想。
  - 原始资料只读。
  - Wiki 由 LLM / Agent 持续维护。
  - 规则和约束显式化。
  - 更像持续编译的 Markdown Wiki，而不是单纯 RAG。
- 对 WikiForge 的启发：
  - Raw Sources 原始资料库。
  - Obsidian Wiki 作为可读、可编辑、可演化知识层。
  - Source Note 和 Wiki 页面双层结构。
  - Agent 修改需要保留来源、日志和可追溯关系。

### Superset / OpenClaw 多 Agent 编排参考

- 链接：https://github.com/superset-sh/superset
- 类型：GitHub 项目
- 参考方向：
  - 多 Agent 编排。
  - 状态跟踪。
  - 任务隔离。
  - 人类审核。
  - Agent 执行过程监控。
- 对 WikiForge 的启发：
  - Agent 流水线状态。
  - 办公室视图的 Agent 工位状态。
  - 任务日志、失败原因、重跑、跳过、进入审核等操作。

### OpenClaw Office / ClawLibrary / Marvis 风格参考

- 用户描述：`ClawLibrary GitHub：WW-AI-Lab/openclaw-office`
- 类型：GitHub 项目 / UI 体验参考
- 关键词：OpenClaw Office、Marvis、等距办公室、Agent 状态
- 参考方向：
  - 等距办公室视觉。
  - Agent 工位。
  - Agent 状态展示。
  - WebSocket / 实时状态。
  - 多 Agent 可视化监控。
- 对 WikiForge 的启发：
  - Web UI 中的“办公室”视图。
  - 6 个 Agent 工位：收集、清洗、分类、整合、审核、归档。
  - 状态：空闲、工作中、等待审核、阻塞、失败、完成。
  - 点击 Agent 查看任务、日志、失败原因和输出。

### iBlinkQ / llm-wiki-obsidian-blink

- 链接：https://github.com/iBlinkQ/llm-wiki-obsidian-blink
- 类型：GitHub 项目 / Obsidian LLM Wiki 实践参考
- 定位：一个基于 Andrej Karpathy LLM Wiki 模式实现的 Obsidian 知识库。
- 参考方向：
  - `raw/` 作为原始资料层。
  - `wiki/` 作为 LLM 维护的知识层。
  - `TheSchema.md` 定义系统规则和工作流。
  - Ingest / Query / Lint 三个日常操作。
  - `wiki/index.md` 帮助 LLM 快速定位内容。
  - `wiki/log.md` 记录知识演化和操作日志。
  - 页面类型包括 sources、entities、concepts、comparisons、overview。
- 对 WikiForge 的启发：
  - 可借鉴 `TheSchema.md`，为 WikiForge 设计 `90_System_系统/Schemas_规范/WikiForgeSchema.md`。
  - 可把 WikiForge Agent 操作扩展为 Ingest、Query、Lint、Organize、Record、Export。
  - 可强化 Lint Agent：定期检查矛盾、过时内容、孤立页面、缺失交叉引用。
  - 可把 `index.md` 和 `log.md` 作为 Obsidian 层必备系统文件。
  - 可参考其页面类型，但 WikiForge 需要扩展 Source、Project、Topic、Entity、Action、Record、Secret 等更复杂对象。
  - 它适合作为 Obsidian 知识层模板参考，不适合作为完整系统架构照搬，因为 WikiForge 还需要 MySQL、Web UI、办公室视图、MCP、Raw Source Organizer、Personal Record Service 和向量库导出。

### garrytan / gbrain

- 链接：https://github.com/garrytan/gbrain
- 类型：GitHub 项目 / Agent 长期记忆与知识运行系统参考
- 定位：面向 OpenClaw / Hermes 等 Agent 的“brain”，重点不是只沉淀知识，而是让 Agent 拥有可持续运行的长期记忆。
- 参考方向：
  - Agent-first 的知识系统：每条消息都可能触发 signal capture、search、write、auto-link、sync。
  - MCP Server：支持 stdio / HTTP MCP，供 Claude、Cursor、Hermes、OpenClaw 等 MCP 客户端调用。
  - 混合检索：向量检索 + BM25 + reranker + ranking boost。
  - 自连接知识图谱：页面写入时自动抽取 wikilink / typed links，形成 typed edges。
  - typed links：如 attended、works_at、invested_in、founded、advises、mentions。
  - cron / autopilot：夜间自动 enrichment、dedup、citation fixing、contradiction detection、task prep。
  - job queue：后台任务可恢复、可审计、可限流。
  - skillpack：将 ingest、enrich、query、brain ops、daily task、reports 等技能交给 Agent 使用。
  - BrainEngine contract：抽象不同存储引擎，对 CLI 和 MCP 暴露统一操作。
- 对 WikiForge 的启发：
  - WikiForge 的 GBrain 运行层可以参考它的运行闭环：signal -> search -> respond -> write -> auto-link -> sync。
  - Personal Record Service 可以参考 signal capture 思路，把消费、账单、邮件、人际关系、想法等都作为可写入信号。
  - MCP Server 应作为一等入口，让 OpenClaw / Hermes 机器人可以写入记录、查询知识、触发任务。
  - MySQL 可设计为运行账本，但要增加 typed links / timeline / facts / claims 等结构化能力。
  - 办公室视图中的任务状态、失败重试、批处理和维护 Agent 可借鉴 gbrain doctor / autopilot / background jobs 思路。
  - 后续向量库能力不应只是 vector-only RAG，应考虑 BM25 + vector + rerank + graph boost 的混合检索。
  - Obsidian Wiki 页面可作为人可读事实源，MySQL / 向量库 / 图谱作为运行态索引。
- 不适合照搬的地方：
  - gbrain 使用 TypeScript / Bun / PGLite / Postgres 生态，WikiForge 已确定后端主技术栈为 Java + Spring Boot + MySQL。
  - gbrain 是 Agent brain 产品，WikiForge 还需要更强的源文件整理、Obsidian Vault、个人记录、办公室 UI 和中文知识管理体验。
  - 第一版不要一次性实现完整 autopilot、复杂 job queue、OAuth MCP、多引擎存储和全量 eval 框架。

### TheSyart / claude-agent-examples

- 链接：https://github.com/TheSyart/claude-agent-examples
- 类型：Agent 教学与工程模式参考
- 定位：从简单到复杂的 Claude Agent 示例，包含多轮对话、工具调用、Skills、Todolist、Subagent、Agent Team、三层记忆和自动压缩。
- 参考方向：
  - 三层记忆：工作记忆、情景记忆、长期记忆。
  - 自动压缩：上下文过长后压缩为情景记忆和长期记忆。
  - Skills 系统：通过 `SKILL.md` 动态加载能力包。
  - Todolist：用结构化任务列表驱动 Agent 持续执行。
  - Subagent：子代理拥有独立 history，只把结果摘要回填主上下文。
  - Agent Team：固定队友通过 inbox 协作，适合长期项目。
- 对 WikiForge 的启发：
  - WikiForge 的 Agent 流水线可以借鉴 Skills + Todolist + Subagent 的结构化执行模型。
  - 办公室视图可参考 Agent Team 的固定角色、状态和 inbox 概念。
  - 维护 Agent、Lint Agent、Connector Agent 可以设计为不同工具权限和职责边界。
  - 长期记忆压缩思想可用于 Personal Record 总结、日报/周报/月报和 Agent 运行日志压缩。
  - 适合作为 Agent 工程教学和模式参考，不适合作为知识库产品架构照搬。

### TheSyart / emperor-agent

- 链接：https://github.com/TheSyart/emperor-agent
- 类型：本地个人 Agent 产品化参考
- 定位：本地运行的个人 AI Agent，带 Vue WebUI、多模型提供商、流式聊天、工具、Skills、记忆系统、MCP、Scheduler、Watchlist、Token telemetry 和子代理派遣。
- 参考方向：
  - Vue WebUI + WebSocket 流式事件展示。
  - 多模型 Provider 配置和双模型路由。
  - 三层记忆、自动压缩、记忆版本回滚。
  - MCP 外部工具接入。
  - 本地 Scheduler 和 Watchlist Heartbeat。
  - 子代理派遣、Agent Team、技能系统。
  - Token 统计和运行事件日志。
  - 权限模式、Ask / Plan 控制流。
- 对 WikiForge 的启发：
  - WikiForge Web UI 的 Agent 日志、工具调用事件、任务进度可以参考其流式 WebUI。
  - 模型配置页可以参考多 Provider + 主/次模型路由。
  - MCP 接入、Scheduler、Watchlist 对 WikiForge 的 GBrain 运行层很有参考价值。
  - 个人记录和维护任务可以借鉴 Scheduler + Watchlist，定期触发总结、巡检、提醒。
  - Token telemetry 可用于 WikiForge 后续成本统计和模型调用审计。
  - Ask / Plan 控制流适合 WikiForge 的人工审核、危险操作确认和 Agent 写入 Obsidian 前确认。
- 不适合照搬的地方：
  - emperor-agent 是 Python 后端，WikiForge 已确定 Java + Spring Boot + MySQL。
  - 它是通用个人 Agent，不是知识熔炉；WikiForge 的核心仍然是源文件整理、Obsidian Wiki 编译、Personal Record 和 GBrain 运行层。
  - MVP 不应引入完整桌宠、复杂权限模式、完整 Agent Team 和大量 UI 特效。

### zzyong24 / thirdspace-pub

- 链接：https://github.com/zzyong24/thirdspace-pub
- 类型：MCP + Obsidian + 个人知识/行动/反思系统参考
- 定位：基于 MCP 协议的个人知识管理系统，让用户和 AI 共同构建可持续运转的知识基础设施。
- 参考方向：
  - 一切皆 Item：笔记、知识卡片、Prompt、日志、计划、事件、反思、项目文档等统一为 Item。
  - Topic 是唯一分类轴：新内容写入时自动创建 Topic，并通过 `topics.json` 管理。
  - Tool -> AI -> Tool 协作模式：MCP 工具只负责 IO，智能摘要、分析、计划由 AI 完成。
  - 内容身份分离：`space/crafted/` 存原创内容，`space/found/` 存收集内容，`flux/` 存原始抓取数据。
  - Obsidian 原生兼容：Markdown、frontmatter、层级标签、双链、Graph View。
  - ItemManager 统一数据层：Topic 管理、Item 写入、Item 检索、统计。
  - Ship-Learn-Next：从知识到实践的行动闭环。
  - 深度反思系统：Mirror / Deepen / Bridge。
  - 行动追踪：行动项写入统一 JSON，并自动渲染 Obsidian 看板。
  - Skills 扩展：worklog、reflect、lifeos、actions、review、article、video-analyzer、feishu 等。
  - MCP 工具数量丰富，覆盖知识卡片、计划、日志、Prompt、反思、LifeOS、周/月复盘、飞书等。
- 对 WikiForge 的启发：
  - 可以借鉴“一切皆 Item”的统一抽象，但 WikiForge 需要保留 Source、Record、Project、Topic、Entity 等更清晰的对象边界。
  - `crafted / found / flux` 对 WikiForge 的 `Obsidian Wiki / Source Note / Raw Sources` 分层很有启发。
  - Tool -> AI -> Tool 很适合 WikiForge 的 MCP 设计：工具负责文件、数据库、Obsidian IO；LLM 负责总结、分类、提炼。
  - Topic 自动创建和 recount/suggest_merge 可用于 WikiForge 的 Topic 管理。
  - Ship-Learn-Next、Mirror/Deepen/Bridge、行动追踪适合 V1/V2 的个人记录、复盘、成长系统。
  - LifeOS 对人际关系、事件分析、关系复盘很有参考价值。
  - 飞书 skill 可作为 WikiForge V1 在线文档连接器参考。
  - 安装脚本自动初始化 Vault、生成 MCP config 的体验值得参考，可用于 WikiForge 的开箱即用部署。
- 不适合照搬的地方：
  - thirdspace-pub 是 Python MCP 工具型项目，WikiForge 后端主栈是 Java + Spring Boot + MySQL。
  - 它强调 Topic 是唯一分类轴；WikiForge 需要 Topic + Project + Entity + Source + Record 多维组织，不宜收敛成单轴。
  - MVP 不应一次性实现 80+ MCP tools、反思系统、LifeOS、视频生成、飞书全能力等。
  - WikiForge 第一阶段仍优先源文件整理和 Obsidian 最小归档闭环。

### colbymchenry / codegraph

- 链接：https://github.com/colbymchenry/codegraph
- 类型：本地代码知识图谱 / MCP Agent 查询参考
- 定位：使用 tree-sitter 解析本地代码仓库，生成文件、类、函数、调用、导入、继承等代码关系图，并通过 MCP 暴露给 AI 编程助手查询。
- 参考方向：
  - 本地优先的代码仓库索引。
  - tree-sitter AST 解析。
  - `nodes` + `relationships` 的图谱建模。
  - FTS / vector / graph 组合查询。
  - MCP 工具设计，如 search、get_node、get_relationships、get_callers、get_callees、get_project_structure。
  - Agent 按需查询上下文，而不是一次性塞入整个仓库。
- 对 WikiForge 的启发：
  - 可作为后续“代码仓库 Source Parser”的技术参考，把代码仓库也纳入 WikiForge 的资料源。
  - 可参考其 MCP 工具形态，设计 WikiForge 的 `search_sources`、`get_source`、`get_related_sources`、`get_project_structure` 等工具。
  - 可参考 `nodes` + `relationships` 思路，为后续 Entity、Topic、Project、Source、KnowledgeCard 建立关系层。
  - 对 AI 开发协作有辅助价值：WikiForge 代码规模变大后，可独立运行代码图谱工具帮助 Codex / Claude / Cursor / Hermes 理解项目结构。
  - 适合进入 V2+ 的代码类资料治理和 MCP Agent 记忆能力，不进入 MVP1/MVP2 核心范围。
- 不适合照搬的地方：
  - codegraph 面向代码结构理解，不解决 PDF、Word、图片、飞书、腾讯文档、微信收藏、B 站收藏等 WikiForge 核心数据源整理问题。
  - WikiForge 后端主栈是 Java + Spring Boot + MySQL，不应直接把它作为核心运行时依赖。
  - 它使用 SQLite 作为本地索引实现，WikiForge 已确定 MySQL 作为控制面，后续向量库作为检索层。
  - 后续若需要直接集成或复用代码，需先确认 license、维护状态和安全边界。

## 讨论中补充的参考方向

### Cognitive Forge / 认知锻造炉

- 链接：https://github.com/ewanqian/cognitive-forge
- 类型：Obsidian + Agent 认知增强方法论参考
- 定位：基于 Trae Solo Agent + Obsidian 的智能知识管理与认知增强系统。
- 参考方向：
  - 知识节点关联管理。
  - 可视化知识图谱。
  - 红蓝对抗训练。
  - 思维轨迹记录。
  - 本地安全存储。
  - MCP 作为 Agent 扩展接口。
- 对 WikiForge 的启发：
  - 可以把“红蓝对抗”作为后续高级 Agent 能力，用于方案审查、决策复盘、项目风险评估。
  - `07_Records_个人记录/` 可以增加思维轨迹、每日反思、周/月度总结等模板。
  - 办公室视图可以增加“审查/挑战 Agent”，用于对已有知识和决策进行反向质询。
  - 适合参考认知训练和个人成长玩法，不适合作为底层架构参考。

### Knowledge_forge

- 链接：https://github.com/nisaral/Knowledge_forge
- 类型：多模态 RAG / 学习工具参考
- 定位：基于 Flask、FAISS、Gemini API 的 AI 学习工具，支持网页、YouTube、文本等内容分析，并提供 Q&A、摘要、测试和可视化。
- 参考方向：
  - 多模态资料摄入。
  - 视频 / 网页 / 文本内容摘要。
  - Mind map / flowchart 等学习型可视化。
  - Q&A、mock tests 等交互。
- 对 WikiForge 的启发：
  - 后续处理 B 站、网课、音视频资料时，可以参考其“学习资料 -> 摘要/问答/测试/可视化”的输出形态。
  - HTML Report 可以支持 mind map、flowchart、知识测验等学习报告。
  - 适合做多模态学习体验参考，不适合直接作为生产级架构参考。

### sage-wiki

- 链接：https://github.com/xoai/sage-wiki
- 类型：LLM 编译式个人知识库 / Obsidian Wiki / MCP 参考
- 定位：实现 Karpathy LLM Wiki 思路，把 papers、articles、notes 编译成结构化、互联、可搜索的 Wiki。
- 参考方向：
  - `raw/` 输入，`wiki/` 输出。
  - LLM 自动总结、抽取概念、发现交叉引用、生成互联 Wiki。
  - 支持 Obsidian。
  - 支持 MCP，Agent 可触发 topic compile。
  - 支持 100K+ 文档级别的 tiered compilation。
  - 支持 hybrid search、query expansion、re-ranking、graph expansion。
  - 支持多格式解析：Markdown、PDF、Word、Excel、PowerPoint、CSV、EPUB、Email、字幕、图片、代码等。
  - 支持 watch folder、lint、doctor、diff、provenance、trust/output verification。
- 对 WikiForge 的启发：
  - 这是当前最值得深入学习的 LLM Wiki 类参考。
  - WikiForge 的 Raw Source Organizer + Obsidian Wiki 编译流程可以重点参考它。
  - 可以借鉴 tiered compilation：先索引、再摘要、再完整编译，避免一上来处理所有文件。
  - 可以借鉴 provenance：明确 Source 与 Wiki 页面之间的来源映射。
  - 可以借鉴 output trust：Agent 生成内容先进入审核区，验证后再进入正式知识库。
  - 可以借鉴 hybrid search：BM25 + vector + rerank + graph expansion。
  - 可以借鉴 watch folder：源文件变化后自动触发增量处理。
  - 不照搬技术栈，sage-wiki 是 Go 项目，WikiForge 主栈为 Java + Spring Boot + MySQL。

### MaxKB

- 链接：https://github.com/1Panel-dev/MaxKB
- 类型：开箱即用知识库 / 企业级智能体平台参考
- 定位：开源企业级智能体平台，强调强大易用、文档知识库、智能体和私有化部署。
- 参考方向：
  - 开箱即用体验。
  - 文档上传、知识库管理、应用/智能体配置。
  - 企业级后台和可视化管理台。
  - 国内部署和模型接入体验。
- 对 WikiForge 的启发：
  - Web UI 可以参考其低门槛知识库创建流程。
  - 系统设置、模型配置、知识库管理、应用发布体验值得参考。
  - 适合做产品交互和后台管理参考。
  - 但 WikiForge 不应变成普通“上传文档问答”平台，核心仍是 Raw Sources 整理 + Obsidian Wiki 编译 + GBrain 运行层。

### Langchain-Chatchat

- 链接：https://github.com/chatchat-space/Langchain-Chatchat
- 类型：本地 RAG / Agent 应用参考
- 定位：基于 Langchain、ChatGLM、Qwen、Llama 等模型的本地知识库问答和 Agent 应用，中文生态成熟，可离线部署。
- 参考方向：
  - 本地知识库问答流程。
  - 文档加载、文本分割、向量化、召回、LLM 生成。
  - 本地模型框架接入，如 Xinference、Ollama、LocalAI、FastChat。
  - 中文 RAG 场景和本地部署经验。
  - BM25 + KNN 等检索组合。
- 对 WikiForge 的启发：
  - 可以参考中文本地模型和向量库接入经验。
  - 可以参考知识库配置、模型配置、离线部署说明。
  - 适合做轻量化 RAG 层参考。
  - 但它主要解决“找得到/问答”，不解决“沉淀成体系”，所以不能作为 WikiForge 的核心路线。

### Khoj

- 链接：https://github.com/khoj-ai/khoj
- 类型：AI second brain / 自托管个人 AI 助手参考
- 定位：自托管 AI 第二大脑，可从本地文档或 Web 获取答案，支持自定义 agents、自动化、深度研究和多模型。
- 参考方向：
  - 个人知识库 + AI 助手体验。
  - 自托管。
  - 本地文档和在线信息统一查询。
  - 自定义 Agent 和自动化任务。
  - Obsidian / 本地笔记生态。
- 对 WikiForge 的启发：
  - 适合参考个人 AI 助手、自动化、跨来源问答体验。
  - 可参考其“second brain”的用户体验和个人工作流。
  - WikiForge 需要比 Khoj 更强调源文件整理、Obsidian Wiki 编译、个人记录和 GBrain 运行账本。

### LangGraph

- 链接：https://langchain-ai.github.io/langgraph/
- 类型：Agent / Workflow 框架
- 参考方向：
  - 状态机。
  - Human-in-the-loop。
  - 可恢复工作流。
  - Agent 流程编排。
- 对 WikiForge 的启发：
  - Agent 流水线可以先自研轻量实现，后续参考 LangGraph 的状态管理思想。
  - 高价值、不确定、敏感资料进入人工审核。

### LlamaIndex Workflows

- 链接：https://docs.llamaindex.ai/en/stable/workflows/
- 类型：知识处理 / Agent Workflow 框架
- 参考方向：
  - 文档 ingestion。
  - 知识抽取。
  - 索引和检索。
  - 工作流。
- 对 WikiForge 的启发：
  - 文件解析、分块、索引、后续向量库导入。

### OpenAI Agents SDK

- 链接：https://platform.openai.com/docs/guides/agents-sdk/
- 类型：Agent SDK
- 参考方向：
  - Agent 分工。
  - Handoff。
  - Guardrails。
  - Tracing。
- 对 WikiForge 的启发：
  - Agent 执行日志。
  - Agent 输出结构化。
  - 模型调用与工具调用追踪。

### Onyx

- 链接：https://docs.onyx.app/
- 类型：企业知识库 / 搜索 / Agent 平台
- 曾用名：Danswer
- 参考方向：
  - 多知识源连接。
  - 统一搜索。
  - Chat / Agents。
  - 企业知识入口。
- 对 WikiForge 的启发：
  - 后续团队化和私有部署。
  - 多来源连接器。
  - 知识检索体验。

### aruis/codex-cookbook

- 链接：https://github.com/aruis/codex-cookbook
- 类型：Codex / AI 编程协作方法参考
- 定位：面向个人和团队开发者的 Codex 实战手册，聚焦高频场景、可复制模板和稳定复用的协作方法。
- 参考方向：
  - 用边界换聚焦，先明确不做什么、哪些可妥协、哪些不能妥协。
  - 用文档和验证换稳定，避免多轮协作只依赖会话记忆。
  - 单次任务采用“目标、模式、约束、输出、验证”的结构化闭环。
  - 中型项目按阶段推进，文档和边界先定，再进入实现。
  - 主线推进中发现技术债时先判断是否阻塞，不阻塞则挂账，不轻易打断主线。
- 对 WikiForge 的启发：
  - 对产品架构本身帮助有限，不是 WikiForge 的功能或技术栈参考。
  - 对开发实施流程帮助很大，适合用于约束 Codex / 外部 AI 参与 MVP 开发。
  - WikiForge 后续每个 MVP 阶段都应采用“目标、约束、输出、验证”的任务格式。
  - 进入 MVP 0 后，应优先保持主线：工程骨架、CI/CD、Docker、Flyway、健康检查，不被文件扫描、AI、MCP 等后续能力提前打断。
  - 发现技术债时，如果不阻塞当前 MVP 0，应先记录，不顺手扩大改动。

## 后续开发时的参考优先级

1. LLM Wiki / Obsidian 知识层：优先看 Karpathy LLM Wiki 和 iBlinkQ/llm-wiki-obsidian-blink。
2. Agent 办公室 UI：优先看 OpenClaw Office / WW-AI-Lab/openclaw-office。
3. Agent 长期记忆与运行层：优先参考 garrytan/gbrain。
4. MCP + Obsidian + 个人记录/反思/行动闭环：重点参考 thirdspace-pub。
5. Agent 工程模式、Skills、Subagent、Agent Team：参考 claude-agent-examples 和 emperor-agent。
6. Agent 流程状态和审核：参考 Superset / OpenClaw、多 Agent 编排思路。
7. 状态机和人类审核：参考 LangGraph。
8. LLM 编译式 Wiki、增量编译、provenance、output trust：重点参考 sage-wiki。
9. 开箱即用后台和知识库管理体验：参考 MaxKB。
10. 中文本地 RAG、模型框架和离线部署：参考 Langchain-Chatchat。
11. 文档 ingestion / 分块 / 向量化：参考 LlamaIndex Workflows。
12. 个人知识库体验：参考 Khoj。
13. 多来源连接和团队化：参考 Onyx。
14. 代码仓库知识图谱、代码类 Source Parser 和 MCP Agent 代码上下文查询：参考 colbymchenry/codegraph。
15. Codex / 外部 AI 开发实施方法：参考 aruis/codex-cookbook。

## 注意事项

- 第一版 WikiForge 后端主技术栈为 Java，不直接照搬 Python/JS 项目的技术栈。
- 外部项目优先参考产品设计、流程、数据结构和交互模式。
- 避免为了复刻参考项目而引入冷门或难维护依赖。
- 后续真正调用或集成某个项目时，需要重新确认 license、API、维护状态和安全边界。
