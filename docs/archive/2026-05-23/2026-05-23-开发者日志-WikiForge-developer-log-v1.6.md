# 2026-05-22 知识熔炉 WikiForge 开发者日志

## 版本索引 Version Index

- 最新版本：v1.6
- 最新小节：`2026-05-23 版本 0.03 发布说明与 main 合并准备`
- 推荐阅读：新 AI 开始工作时，先读本索引和最新小节，再按任务需要阅读历史小节。
- 历史范围：v0.1-v0.9 记录需求发掘、架构评审、MVP0 骨架、微服务拆分和同日滚动归档规则；仅在追溯需求来源或架构决策时阅读。

## 会话主题

围绕个人知识库管理系统进行需求发掘、产品定义、技术架构和数据模型设计。项目最终命名为 **知识熔炉 WikiForge**。

## 核心痛点

用户当前数据源很多，包括：

- 个人看板
- 密码文档
- 飞书在线文档
- 腾讯文档
- 印象笔记
- 本地文档
- 微信收藏
- B站收藏
- 知乎文章
- 项目文档
- 各类杂乱文件

核心问题不是单纯“数据源很多”，而是：

- 没有统一收集。
- 没有整理。
- 没有归档。
- 没有提炼。
- 没有后续复用。

用户希望先把所有知识和源文件收集汇总起来，整理归集到统一位置，再逐步进行提炼、归档和复用。

## 产品定位演进

最初方向是通过本地数据库 + Obsidian 进行规范化管理。

后续逐步明确为：

- 本地优先的 LLM Wiki / Agentic Knowledge Base。
- 通过多 Agent 对输入文档和资料进行检查、整理、归档。
- 类似 Karpathy LLM Wiki 的 Raw Sources / Wiki / Schema 思路。
- 支持独立 Web UI 看板。
- 支持 Obsidian 作为主阅读、编辑、沉淀界面。
- 支持 MySQL 作为控制平面、索引库和流程账本。
- 支持后续批量导入向量库。
- 支持 MCP 能力。
- 进一步明确为 LLM Wiki + GBrain 融合路线：LLM Wiki 负责知识表达层，GBrain 思路负责知识运行层。

最终产品定位：

> 知识熔炉 WikiForge 是一个本地优先的个人知识资产管理系统。它先把分散、杂乱的源文件和在线资料收集整理到统一位置，再通过 Agent 流水线进行分类、提炼、知识卡生成和 Obsidian 归档，最终支持搜索、报告、向量库和 MCP 复用。

## 技术路线判断：LLM Wiki + GBrain

用户补充了一套重要分析：传统 RAG 解决的是“找得到”，但没有解决“沉淀成体系”。传统 RAG 存在三个痛点：

- 答案不稳定：同一问题多次询问，召回材料和上下文不同，输出容易波动。
- 知识难以进化：新增内容往往只补进索引，不重组已有认知结构。
- 缺乏长期记忆：每次查询像临时拼装，缺少可持续运行的记忆机制。

由此形成两条路线：

- LLM Wiki：构建稳定的知识表达层。
- GBrain：构建持续运行的知识系统。

WikiForge 的选择不是二选一，而是融合：

- 第一阶段偏 LLM Wiki：先完成 Raw Sources、Obsidian Wiki、Source Note、Schema、index、log、知识卡和人工可维护页面。
- 第二阶段增强 GBrain：接入个人记录、邮件、账单、人际关系、MCP、向量库、定时巡检、OpenClaw / Hermes 机器人写入和 Agent 长期记忆。

关键架构判断：

- LLM Wiki 是知识的编译器。
- GBrain 是知识的操作系统。
- WikiForge 需要先把知识编译成稳定表达，再让知识在系统里持续运行。
- Obsidian Wiki 负责人可读、可编辑、可维护。
- MySQL 负责运行账本和结构化状态。
- 向量库负责语义检索和 Agent 调用。
- MCP 负责外部 Agent / 机器人 / 工具接入。
- 定时任务和 Lint Agent 负责持续维护。

## 项目命名

确定正式名称：

- 中文名：知识熔炉
- 英文名：WikiForge
- 完整名：知识熔炉 WikiForge

命名含义：

- 原始资料像矿石。
- Agent 像工匠。
- Obsidian Wiki 是被锻造出来的知识资产。

办公室视图名称：

- 确认为“办公室”
- 不使用“奥恩工坊”

## 参考资料

用户提供：

- Karpathy LLM Wiki: https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f#llm-wiki
- Superset / OpenClaw 相关多 Agent 编排参考: https://github.com/superset-sh/superset
- OpenClaw Office / Marvis 风格：等距办公室 + Agent 状态展示
- iBlinkQ / llm-wiki-obsidian-blink: https://github.com/iBlinkQ/llm-wiki-obsidian-blink
- garrytan / gbrain: https://github.com/garrytan/gbrain
- Cognitive Forge: https://github.com/ewanqian/cognitive-forge
- Knowledge_forge: https://github.com/nisaral/Knowledge_forge
- sage-wiki: https://github.com/xoai/sage-wiki
- MaxKB: https://github.com/1Panel-dev/MaxKB
- Langchain-Chatchat: https://github.com/chatchat-space/Langchain-Chatchat
- Khoj: https://github.com/khoj-ai/khoj
- thirdspace-pub: https://github.com/zzyong24/thirdspace-pub

额外参考方向：

- LangGraph：状态流转、人类审核、可恢复工作流。
- LlamaIndex Workflows：知识 ingestion 和工作流。
- OpenAI Agents SDK：Agent 分工、handoff、tracing。
- Khoj：个人 AI 知识库和 Obsidian 生态。
- Onyx：企业知识入口、搜索、连接器体验。

新增参考判断：

- `iBlinkQ/llm-wiki-obsidian-blink` 是 Karpathy LLM Wiki 思路的 Obsidian 实践版本。
- 它对 WikiForge 的 Obsidian 知识层、Schema 规范、index/log 系统文件、Ingest/Query/Lint 工作流非常有参考价值。
- 它不适合作为完整架构照搬，因为 WikiForge 还需要 Java 后端、MySQL、Web UI、办公室视图、MCP、源文件整理、个人记录和向量库导出。

新增 GBrain 项目参考判断：

- `garrytan/gbrain` 是 Agent 长期记忆和知识运行系统参考，比普通 RAG 更接近 WikiForge 的后续运行层。
- 它强调 Agent-first：每条消息都可以触发 signal capture、search、write、auto-link、sync。
- 它内置 MCP Server，支持 stdio / HTTP MCP，可以被 Claude、Cursor、Hermes、OpenClaw 等客户端调用。
- 它的混合检索、typed links、自连接知识图谱、cron/autopilot、job queue、skillpack 对 WikiForge 很有启发。
- WikiForge 可借鉴其运行闭环，但不照搬技术栈。gbrain 偏 TypeScript/Bun/Postgres/PGLite；WikiForge 已确定 Java + Spring Boot + MySQL。
- 对 WikiForge 最有价值的是：
  - Personal Record Service 可参考 signal capture。
  - MCP Server 要作为 OpenClaw / Hermes 写入记录和查询知识的一等入口。
  - 向量库后续不应只做 vector-only RAG，应考虑 BM25 + vector + rerank + graph boost。
  - MySQL 运行账本后续可增加 typed links、facts、claims、timeline 等结构化能力。
  - 维护 Agent 可参考 autopilot / doctor / background jobs。

新增项目分类参考判断：

- 知识熔炉/AI 自动整理型：
  - `sage-wiki` 最值得深入参考，尤其是 LLM 编译式 Wiki、tiered compilation、provenance、output trust、watch folder、hybrid search。
  - `Cognitive Forge` 适合作为认知增强、红蓝对抗、思维轨迹和 Obsidian 方法论参考。
  - `Knowledge_forge` 适合作为多模态学习资料处理、摘要、问答、mind map、flowchart、测试题生成参考。
- 开箱即用型：
  - `MaxKB` 适合作为低门槛知识库创建、模型配置、应用后台和国内私有化部署体验参考。
- 轻量化 RAG 型：
  - `Langchain-Chatchat` 适合作为中文本地 RAG、本地模型、向量库和离线部署参考。
  - 它解决“问答/找得到”更强，不解决“沉淀成体系”，不能作为 WikiForge 的核心路线。
- Obsidian 生态型：
  - `Khoj` 适合作为 AI second brain、Obsidian/本地笔记接入、自托管个人助手体验参考。

对 WikiForge 的总体启发：

- 第一阶段应参考 `sage-wiki` 和 `llm-wiki-obsidian-blink`，把 LLM Wiki 表达层做好。
- Web UI 开箱体验可参考 `MaxKB`。
- RAG / 模型 / 向量库接入可参考 `Langchain-Chatchat`。
- 个人 AI 助手体验可参考 `Khoj`。
- 认知增强、红蓝对抗和思维记录可参考 `Cognitive Forge`。
- 多模态学习报告和测验类输出可参考 `Knowledge_forge`。

新增 thirdspace-pub 参考判断：

- `zzyong24/thirdspace-pub` 是 MCP + Obsidian + 个人知识/行动/反思系统参考。
- 它提出“一切皆 Item”，将笔记、知识卡片、Prompt、日志、计划、事件、反思、项目文档统一管理。
- 它的 `crafted / found / flux` 分层对 WikiForge 很有启发，可类比 Obsidian Wiki / Source Note / Raw Sources。
- 它的 Tool -> AI -> Tool 模式适合 WikiForge：工具负责 IO，AI 负责总结、分类、提炼。
- 它的 Ship-Learn-Next、Mirror/Deepen/Bridge、行动追踪和 LifeOS 对 WikiForge 后续个人记录、复盘、人际关系管理有参考价值。
- 它的 Topic 自动创建、recount、suggest_merge 可参考用于 WikiForge Topic 管理。
- 不适合照搬的地方：项目是 Python MCP 工具型项目，WikiForge 是 Java + Spring Boot + MySQL；thirdspace 强调 Topic 单轴分类，而 WikiForge 需要 Source、Project、Topic、Entity、Record 多维组织。

## 第一版总体闭环

最终确认的闭环：

```text
资料进入系统
  -> 源文件归集整理
  -> 建立索引和分类
  -> 决定是否进入 Agent 提炼加工
  -> 生成建议、知识卡和草案
  -> 自动归档或人工审核
  -> 写入 MySQL 和 Obsidian
  -> UI 看板追踪状态
  -> 后续导入向量库 / MCP 复用
```

## 产品目标层级

目标拆成四层：

1. 收集：把多源资料统一纳入系统。
2. 整理：把散落文件归集到 Raw Sources，并建立索引和初步分类。
3. 归档：把有价值资料沉淀到 Obsidian Source Note、项目页、主题页和实体页。
4. 复用：通过搜索、看板、HTML 报告、后续向量库和 MCP，让资料进入项目、决策、问答和 Agent 工作流。

第一版重点解决前两层，并打通基础归档。

## 源文件整理模式

用户明确表示真正需要的是源文件整理模式，因为东西太乱，希望先整理放到一个地方，后续再决定是否提炼加工。

确定：

- 源文件整理是第一版核心能力。
- 默认采用复制模式，保留原文件不删除。
- 支持多个指定路径扫描。
- 支持按类型、项目、来源、时间或规则整理。
- 重复文件不重复复制，只记录重复关系。
- 不确定分类进入待确认目录。
- 用户可以选择只整理，不提炼。

Raw Sources 目录建议：

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

## Obsidian Vault 目录结构

确定采用混合目录结构：

- PARA 作为顶层组织方式。
- 主题库和实体库作为横向知识网络。
- 目录采用 `数字_英文_中文` 格式。

目录建议：

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
  90_System_系统/
    AgentLogs_Agent日志/
    Templates_模板/
    Indexes_索引/
    Schemas_规范/
```

## Obsidian 角色

Obsidian 同时承担：

- 主界面之一：查看、搜索、编辑、维护知识。
- 长期沉淀层：保存规范 Markdown、双链、标签、图谱和人工编辑内容。

Web UI 需要支持：

- 内部预览 Obsidian Markdown。
- 通过 `obsidian://open` 一键打开本地 Obsidian 文件。

## MySQL 定位

最初 MySQL 作用不清晰，后续明确为：

> MySQL = 控制平面 + 结构化索引库 + Agent 流程账本 + 轻量内容缓存。

MySQL 不作为最终知识正文主库。

MySQL 保存：

- Source 元数据。
- 文件路径、hash、来源、状态。
- Agent 执行过程和日志。
- 审核队列。
- 标签、项目、主题、实体关联。
- Obsidian 文件映射。
- HTML artifact 记录。
- 办公室 Agent 状态。
- 后续向量化分块和 embedding 状态。
- MCP Server / Client 配置和工具调用日志。

正文保存策略：

- 普通文本、网页、Markdown、小型 Word 可保存解析文本。
- PDF、大文件、图片、敏感资料默认只保存路径、摘要、hash 和元数据。
- 是否保存全文由资料类型、文件大小、敏感级别和用户规则决定。

## 多 Agent 流水线

初版采用流水线模式，便于落地：

```text
Ingest 收集
  -> Normalize 清洗
  -> Classify 分类
  -> Integrate 整合
  -> Review 审核
  -> Archive 归档
```

第一版底层可以是单 LLM + 多步骤 Agent 角色，但 UI、日志、数据库中按 Agent 节点建模。

后续可插拔升级成真实多 Agent 实例。

## Agent 输出

Agent 输出全选，并采用建议 + 草案 + 分级写入机制：

- 一句话摘要。
- 结构化摘要。
- 关键观点。
- 标签/分类。
- 项目/主题/实体关联。
- 任务建议。
- 质量检查。
- 重复、过期、冲突判断。
- Wiki 整合建议。
- Obsidian Markdown 草案。
- HTML 预览内容。
- HTML 报告导出产物。
- 是否需要人工审核。
- 处理日志。

## HTML 输出定位

确定支持：

- Web UI 预览。
- 报告导出。

不支持：

- HTML 作为知识库主存储。

原则：

- Markdown / Obsidian Wiki 是主知识源。
- HTML 是展示层和导出产物。

## 办公室视图

Web UI 需要展示 Agent，参考 OpenClaw Office / Marvis 风格。

确认：

- 视图名称叫“办公室”。
- 第一版采用可操作型，而不是纯展示型。
- 不做复杂拖拽调度或编排编辑器。

办公室视图展示 6 个 Agent 工位：

1. 收集 Agent
2. 清洗 Agent
3. 分类 Agent
4. 整合 Agent
5. 审核 Agent
6. 归档 Agent

Agent 状态：

- 空闲
- 工作中
- 等待审核
- 阻塞
- 失败
- 完成

支持操作：

- 查看当前任务。
- 查看历史任务。
- 查看日志。
- 查看失败原因。
- 查看输出。
- 重跑任务。
- 跳过步骤。
- 进入审核。

## 在线文档链接采集

用户补充：收集层存在飞书文档、在线文档等，希望输入飞书文档地址后，后台通过 OpenClaw / Hermes / 服务自身能力读取链接文档，再通过多 Agent 处理输出知识卡并归档。

确定为正式能力：在线文档链接采集。

流程：

```text
用户输入飞书/腾讯文档等在线文档地址
  -> Link Ingestion Service 识别平台
  -> Connector Gateway 调用 OpenClaw / Hermes / Native Connector
  -> 读取文档内容和元数据
  -> 创建 Source 和采集记录
  -> Agent 流水线生成知识卡
  -> 进入审核或自动归档
```

连接器类型：

- OpenClaw Connector
- Hermes Connector
- Native Connector

异常状态：

- permission_denied
- invalid_url
- connector_error
- empty_content

## 个人数字生活记录

用户补充核心诉求：除了整理杂乱知识外，也希望持续记录自己，不只是各种文档，还包括消费记录、账单、邮件、人际关系等个人数据。

新增定位：

> WikiForge 不只是知识文件整理系统，也需要成为个人数字生活记录与归纳系统。

用户希望通过 OpenClaw 或 Hermes 机器人调用本项目 MCP，把个人记录写入 WikiForge，再由系统进行整理、归纳和总结。

记录范围：

- 消费记录：日常消费、订阅、购物、服务支出。
- 账单记录：信用卡账单、平台账单、报销、周期性费用。
- 邮件记录：重要邮件、待处理邮件、合同/发票/通知类邮件。
- 人际关系：联系人、互动记录、关系备注、后续跟进事项。
- 个人事件：想法、决策、日程回顾、生活记录、阶段总结。

写入方式：

- Web UI 手动录入。
- OpenClaw / Hermes 机器人调用 WikiForge MCP 写入。
- 后续通过邮箱、账单、聊天、日历等连接器自动导入。

新增 MCP tools：

- create_personal_record
- search_personal_records

新增架构模块：

- Personal Record Service

新增数据模型：

- personal_records

原则：

- 个人记录和文档资料共用统一收集、整理、归档、复用闭环。
- 个人记录必须保留来源、时间、写入方式和原始内容。
- 涉及隐私、财务、人际关系的记录需要支持敏感标记和人工审核。
- Agent 可以做分类、归纳、总结和行动项提取，但不能只保存摘要而丢失原始记录。

## 向量库规划

用户明确：后续规划会将整理结果批量导入向量库。

确定：

- 第一版不强依赖向量库。
- 但第一版需要预留分块、hash、embedding 状态和批量导出能力。
- 后续用于语义检索、知识问答和 Agent 调用。

架构新增：

- Vector Export Service

数据模型新增：

- content_chunks
- embedding_jobs

后续可接：

- Milvus
- Qdrant
- Chroma
- pgvector
- 其他本地或私有部署向量库

## MCP 能力

用户要求项目支持 MCP。

确定：

- MCP 是系统扩展层。
- 第一版优先实现 WikiForge MCP Server。
- MCP Client 先预留，用于后续连接外部 MCP Server。
- MCP 调用必须落日志。

MCP tools 规划：

- search_sources
- get_source
- create_import_job
- get_agent_status
- get_review_items
- get_obsidian_note
- open_obsidian_note

数据模型新增：

- mcp_servers
- mcp_tool_calls

## 技术架构约束

用户明确：

- 项目采用前后端分离架构，便于后续维护。
- 主要开发语言是 Java 技术栈。
- 技术建议采用国内开发主流技术。
- 不使用生僻冷门框架和 jar 包。

确认技术建议：

后端：

- Java 17 或 Java 21 LTS。
- Spring Boot 3.x。
- Spring Web / Spring MVC。
- Spring Validation。
- MyBatis-Plus 或 MyBatis。
- Spring Scheduler 或 XXL-JOB。
- Maven 优先。

前端：

- Vue 3 + Vite + TypeScript 优先。
- Element Plus 或 Ant Design Vue。
- Pinia。

文件解析：

- Markdown：commonmark-java 或 flexmark-java。
- Word：Apache POI。
- PDF：Apache PDFBox。
- 图片元数据：metadata-extractor。
- JSON：Jackson。

模型调用：

- 通过标准 HTTP Client 封装 Provider Adapter。
- 不在第一版绑定冷门 AI 框架。

## 模型与调用

用户希望支持国内模型：

- DeepSeek
- MiniMax
- 其他兼容 OpenAI API 格式的模型供应商

模型切换：

- 通过 CC Switch 管理。
- 系统侧通过 Provider Adapter 抽象模型调用。

## UI 看板页面

第一版需要独立 Web UI，不需要登录。

核心页面：

- 收集入口页。
- 路径导入页。
- 在线文档链接采集入口。
- 待审核队列。
- 资料库列表。
- 项目看板。
- Agent 运行日志。
- 办公室视图。
- Obsidian 文件预览页。
- 系统设置页。

## 敏感资产

第一版不做加密。

但需要：

- 支持 Secret 类型。
- 支持敏感标记。
- 敏感资料进入人工确认。
- 不建议 Agent 默认读取明文密码或密钥。

## 维护 / 定时处理

讨论过手工修改后是否需要定时检查。

建议加入维护 Agent：

- 定时扫描 Obsidian Vault 中被手工修改的 Markdown。
- 对比 content_hash / last_written_at。
- 刷新数据库索引。
- 判断是否需要重新生成摘要、标签、分块、embedding 状态。
- 检查链接断裂、重复、过期、冲突。
- 不确定变更进入审核队列。

调度建议：

- 第一版使用 Spring Scheduler。
- 后续任务复杂后接入 XXL-JOB。

原则：

- 用户手工修改优先。
- Agent 不应静默覆盖人工内容。

## 已生成文档

当前已生成三份核心文档：

- `docs/需求文档-knowledge-base-prd.md`
- `docs/技术架构-technical-architecture.md`
- `docs/数据模型-data-model.md`
- `docs/2026-05-22-参考项目清单-WikiForge-reference-projects.md`
- `docs/2026-05-22-需求完整度自检-WikiForge-requirements-completeness-review.md`
- `docs/2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md`

本开发者日志：

- `docs/2026-05-22-开发者日志-WikiForge-developer-log.md`

## 下一步建议

后续继续迭代时，可以从以下方向推进：

1. MVP 里程碑拆分。
2. 页面原型和 UI 信息架构。
3. Spring Boot 项目模块划分。
4. MySQL DDL 初稿。
5. Agent prompt / output schema。
6. Raw Source Organizer 详细规则。
7. Link Ingestion Connector 接口定义。
8. Obsidian Source Note 模板和 Wiki 页面模板。
9. MCP Server tool schema。
10. 向量库导出格式。

## 复盘后的 MVP 收敛结论

用户确认长期目标正确，但实施阶段需要调整：优先保证 MVP 可行，再逐步拓展。

确认事项：

1. MVP 第一目标是先把本地杂乱源文件归集整理起来。
2. 飞书 / 腾讯文档自动读取可以暂缓，MVP 先预留连接器接口。
3. 办公室等距视图是拓展能力，MVP 先做普通后台 UI。
4. MCP 在 MVP 只做最小工具集，先满足后续 OpenClaw / Hermes 接入基础。

调整后的 MVP 核心闭环：

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

阶段划分：

- MVP 0：项目骨架。
- MVP 1：源文件归集。
- MVP 2：Obsidian Source Note。
- MVP 3：AI 辅助整理。
- MVP 4：轻量 MCP 预览版。
- V1：在线资料与个人记录。
- V2：知识运行层。

当前优先级：

先实现 LLM Wiki 表达层的最小闭环，再逐步增强 GBrain 运行层。

## 2026-05-23 文档归档规则

用户要求将沟通形成的需求文档、开发者日志等 Markdown 文件复制一份到项目目录下，并按“日期 + 文件名 + 版本号”的方式记录留档，用于保留完整迭代过程。

后续补充命名规则：文档文件名采用“中文名 + EnglishName”的方式，方便中文阅读和开发语义识别；`README.md` 保持通用入口命名。

已建立归档目录：

```text
docs/archive/2026-05-23/
```

本次归档内容：

- PRD v0.2
- 技术架构 v0.2
- 数据模型 v0.2
- MVP 实施计划 v0.1
- 需求完整度自检 v0.1
- 参考项目清单 v0.1
- 开发者日志 v0.1
- 架构决策记录 v0.1
- README v0.1

归档索引：

```text
docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-v0.1.md
```

## 2026-05-23 AI 开发前置规则

用户补充要求：后续参与开发的 AI 必须先查看最新日期快照，避免读取旧需求后跑偏。

已新增根目录规则文件：

```text
AGENTS.md
```

规则要点：

1. 开发、改文档、做方案前，先查找 `docs/archive/YYYY-MM-DD/` 中日期最新的目录。
2. 先阅读最新目录下的归档索引。
3. 再按任务需要阅读该日期目录中的需求文档、技术架构、数据模型、实施计划和开发者日志快照。
4. 最后阅读 `docs/` 下的当前主文档。
5. 如最新快照、主文档和用户当前指令冲突，先指出冲突并请用户确认。

## 2026-05-23 架构评审准备

用户准备在正式写 MVP 代码前发起一次架构评审。

已新增评审材料：

```text
docs/2026-05-23-架构评审材料-WikiForge-architecture-review.md
```

同时归档快照：

```text
docs/archive/2026-05-23/2026-05-23-架构评审材料-WikiForge-architecture-review-v0.1.md
```

本次评审目标是确认 WikiForge MVP 是否可以进入编码阶段，重点审查 MVP 边界、技术栈、MySQL / Obsidian / Raw Sources 职责、文件系统安全、数据模型收敛度和后续 MCP / 向量库 / 在线文档 / 个人记录扩展预留。

## 2026-05-23 架构评审补充：CI/CD 与 Docker 发布

用户补充要求：架构评审必须考虑打包和部署方式，计划采用 CI/CD 打包流程和 Docker 发布模式，避免后续出现无法自动迭代、Docker 打包异常而反向修改架构的问题。

已更新：

- `docs/技术架构-technical-architecture.md` 升级为 v0.3，补充 CI/CD、Docker Compose、镜像拆分、配置注入、volume 挂载、Flyway migration、健康检查和部署评审约束。
- `docs/2026-05-23-架构评审材料-WikiForge-architecture-review.md` 升级为 v0.2，补充 CI/CD 与 Docker 专项评审问题。

新增归档快照：

```text
docs/archive/2026-05-23/2026-05-23-技术架构-technical-architecture-v0.3.md
docs/archive/2026-05-23/2026-05-23-架构评审材料-WikiForge-architecture-review-v0.2.md
```

当前倾向：

- MVP 采用前后端分离镜像：`wikiforge-backend`、`wikiforge-frontend`、`mysql:8`。
- 使用 Docker Compose 管理应用、前端和 MySQL。
- Raw Sources、Obsidian Vault、MySQL 数据、日志和配置必须外部挂载。
- CI/CD 最小流水线覆盖后端测试、前端构建、Docker build 和 Compose smoke test。

## 2026-05-23 外部 AI 架构评审归档与最终结论

用户提供了多份外部 AI / 模型的架构评审结果，原始目录为：

```text
docs/archive/2026-05-23/架构评审/
```

已规范为：

```text
docs/archive/2026-05-23/架构评审-architecture-review/
```

目录内文件已按“日期 + 中文名 + EnglishName + 版本号”的规则重命名，并新增：

```text
docs/archive/2026-05-23/架构评审-architecture-review/2026-05-23-架构评审索引-architecture-review-index-v0.1.md
docs/archive/2026-05-23/架构评审-architecture-review/2026-05-23-架构评审结论-WikiForge-architecture-review-conclusion-v1.0.md
```

同时新增当前主结论文档：

```text
docs/2026-05-23-架构评审结论-WikiForge-architecture-review-conclusion.md
```

评审综合结论：

```text
可以进入 MVP 0 项目骨架阶段。
```

采纳的关键调整：

- 数据访问最终采用 MyBatis-Plus 3.5.x。
- Flyway 分阶段建表，MVP 0 不创建全部长期规划表。
- MVP 0 纳入 CI/CD、Docker Compose、`.env.example` 和健康检查。
- `sources` 不承载大文本正文，后续拆 `source_contents`。
- 路径安全、符号链接、SHA-256、原子复制、Obsidian URI 安全作为 MVP 1 前置约束。
- AI / MCP 延后到 MVP 3 / MVP 4，不阻塞 MVP 0。

同步更新并归档：

```text
docs/架构决策-DECISIONS.md
docs/数据模型-data-model.md
docs/2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md
docs/archive/2026-05-23/2026-05-23-架构决策-DECISIONS-v0.2.md
docs/archive/2026-05-23/2026-05-23-数据模型-data-model-v0.3.md
docs/archive/2026-05-23/2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.2.md
```

## 2026-05-23 开发实施参考：aruis/codex-cookbook

用户建议在正式开发前查看：

```text
https://github.com/aruis/codex-cookbook
```

评估结论：

- 该仓库对 WikiForge 产品架构本身帮助有限，不作为功能架构或技术栈参考。
- 该仓库对 Codex / 外部 AI 参与开发实施很有帮助，适合作为 MVP 0 开发协作方法参考。

可吸收的实践：

- 每个阶段任务采用“目标、模式、约束、输出、验证”结构。
- 复杂任务先收敛计划，再进入执行。
- 多轮协作必须持续落文档，不依赖会话上下文。
- 主线明确后直接推进，非关键问题不频繁打断。
- 发现技术债时先判断是否阻塞当前交付；不阻塞则挂账，不顺手扩大范围。

对下一阶段的约束：

- MVP 0 只做工程骨架、CI/CD、Docker、Flyway、健康检查和基础配置。
- 文件扫描、AI、MCP 等后续能力不得提前打断 MVP 0 主线。
- 每个开发任务完成后必须给出最小验证步骤。

## 2026-05-23 MVP 0 工程骨架启动

本次进入 MVP 0 编码阶段，创建独立开发分支：

```text
codex/mvp0-project-skeleton
```

本阶段目标保持收敛：先建立可测试、可打包、可 CI、可 Docker 化的前后端分离工程骨架，不提前实现文件扫描、AI Agent、MCP、向量库等后续能力。

新增工程骨架：

- 后端：`backend/`，Spring Boot 3.3.6 + Java 21 + MyBatis-Plus 3.5.9 + Flyway + MySQL + Actuator + springdoc。
- 前端：`frontend/`，Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router + Axios。
- 部署：`deploy/docker-compose.yml`、`deploy/docker-compose.dev.yml`、后端/前端 Dockerfile、`.dockerignore`、`.env.example`。
- CI：`.github/workflows/ci.yml`，包含后端测试与打包、前端构建、Docker 镜像构建、Compose 配置校验。
- 数据库迁移：`V20260523_001__create_mvp0_tables.sql`，先建立 `system_settings` 与 `model_providers` 两张 MVP 0 基础表。

本地验证结果：

```text
mvn -s %TEMP%\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\repository" test
结果：通过

mvn -s %TEMP%\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\repository" -B -DskipTests package
结果：通过

npm ci
结果：通过，无漏洞

npm run build
结果：通过，有 Vite 大包体积提示，MVP 0 暂不处理

docker compose -f deploy/docker-compose.yml config
结果：通过

docker compose -f deploy/docker-compose.dev.yml config
结果：通过

docker compose -f deploy/docker-compose.yml build
结果：未完成，本机 Docker Desktop Linux engine 未启动，报 dockerDesktopLinuxEngine 管道不存在
```

环境记录：

- 用户本地 Maven 仓库地址为 `E:\repository`。
- PowerShell 下 Maven 本地仓库参数需要加引号：`"-Dmaven.repo.local=E:\repository"`。
- 当前默认 `java -version` 为 Java 8，不适合本项目；本次使用 Trae 内置 JDK 25 执行 Maven。项目配置仍以 Java 21 为准，CI 也使用 Temurin 21。
- 本地全局 Maven mirror 指向不可用地址，本次用临时 settings 文件绕过；后续应优先在开发机 Maven 配置层修复，不把本机绝对路径写进项目配置。

已发现但不阻塞 MVP 0 的事项：

- 前端 Element Plus 初始整体引入会产生 Vite chunk size warning，后续看板复杂后再做按需加载或分包。
- 当前只有健康检查和占位看板，业务能力仍从 MVP 1 的“源文件归集整理”开始实现。

## 2026-05-23 微服务架构与 AI 开发 Skill 约定

用户补充确认：WikiForge 应采用微服务模式，并根据领域进行服务拆分，以支撑长期扩展和多人维护。

最终选择：

```text
B：少服务微服务
```

MVP 0/1 目标服务调整为：

```text
wikiforge-core-service
wikiforge-worker-service
wikiforge-ui
mysql
```

`wikiforge-gateway` 可预留，但 MVP 0/1 不强制运行。后续再拆 `agent-service`、`connector-service`、`mcp-service`、`vector-service`、`record-service`。

本次学习了用户提供的民生 CDP AI 开发范式目录：

```text
E:\个人知识体系\01_工作项目\2026_民生CDP项目\20260513 V3.0版本\20260513 V3.0版本\
```

只吸收架构样式和设计理念：

- 全局强约束、脚手架规范、条线型服务规范分层。
- 服务职责、包结构、API、数据表归属、异常码和跨服务调用先文档化。
- 后端采用 DDD 四层依赖方向。
- 前端采用分层目录。
- 多人或多 AI 开发前先声明目标服务、文件边界、依赖契约和验证命令。

不复制 CDP 业务和重型基础设施：

- 不采用 CDP 业务模型。
- 不采用 Spring Boot 4.1.x。
- 不在 MVP 0/1 引入 Nacos、Kafka、Redis、XXL-JOB、TDSQL、StarRocks。

新增 WikiForge 项目内 AI 开发 Skill：

```text
docs/ai-skills/wikiforge-development/SKILL.md
docs/ai-skills/wikiforge-development/references/agent-role-prompts.md
docs/ai-skills/wikiforge-development/references/architecture-style.md
docs/ai-skills/wikiforge-development/references/service-boundaries.md
docs/ai-skills/wikiforge-development/references/backend-ddd-standard.md
docs/ai-skills/wikiforge-development/references/frontend-standard.md
docs/ai-skills/wikiforge-development/references/ci-docker-standard.md
docs/ai-skills/wikiforge-development/references/multi-agent-collaboration.md
docs/ai-skills/wikiforge-development/references/development-workflow.md
```

补充多人协作角色 Prompt：

- Architect Agent：架构评审。
- Core Service Agent：核心业务 API。
- Worker Service Agent：文件扫描和归集任务。
- UI Agent：前端页面。
- DevOps Agent：CI/CD 和 Docker。
- Docs Agent：文档与归档。
- Review Agent：代码评审。

补充 Work Order 模板，要求每个开发切片开始前明确任务名称、目标服务、目标文件、依赖契约、数据表归属、验证命令和是否需要归档。

## 2026-05-23 Git 提交规则补充

用户补充：前端 `node_modules` 属于依赖/编译相关内容，不需要也不允许提交到 Git 仓库。

本次同步规则：

- `node_modules/`、`dist/`、`.vite/` 不允许提交。
- 后端 `target/`、`build/` 不允许提交。
- `.env`、日志、运行数据、Raw Sources、Obsidian Vault 数据不允许提交。
- `package-lock.json` 可以提交，用于保证前端依赖可重复安装。
- 提交前必须检查 `git status --short` 和 `git diff --check`。

同步更新：

```text
AGENTS.md
docs/ai-skills/wikiforge-development/references/development-workflow.md
```

同步更新：

```text
AGENTS.md
docs/技术架构-technical-architecture.md
docs/数据模型-data-model.md
docs/2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md
docs/架构决策-DECISIONS.md
docs/2026-05-23-微服务架构与AI开发Skill设计-WikiForge-microservice-ai-skill-design.md
```

下一步建议：

```text
将当前单后端 MVP0 骨架重构为 backend Maven monorepo：
wikiforge-common + wikiforge-core-service + wikiforge-worker-service
```

## 2026-05-23 MVP0 服务拆分执行

本次开始执行 MVP0 微服务拆分开发计划，将旧的单 `backend` Spring Boot 骨架调整为 Maven monorepo：

```text
backend/
  pom.xml
  wikiforge-common/
  wikiforge-core-service/
  wikiforge-worker-service/
```

本次完成：

- `wikiforge-common`：沉淀统一 `ApiResponse`。
- `wikiforge-core-service`：作为 MVP 对外 API 入口，保留 MyBatis-Plus、Flyway、MySQL、springdoc 和 `/api/health`。
- `wikiforge-worker-service`：作为文件任务执行服务骨架，提供 `/api/v1/worker/health`。
- Docker 镜像拆分为 `core-service.Dockerfile` 和 `worker-service.Dockerfile`。
- Docker Compose 服务调整为 `mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui`。
- Nginx `/api` 和 `/actuator` 反向代理到 Core Service。
- GitHub Actions 调整为后端多模块测试/打包，并分别构建 Core、Worker、UI 镜像。
- `.dockerignore` 补充 `backend/**/target`、`backend/**/build`、`frontend/.vite`。

本地验证结果：

```text
mvn -s %TEMP%\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\repository" -B test
结果：通过

mvn -s %TEMP%\wikiforge-maven-settings.xml "-Dmaven.repo.local=E:\repository" -B -DskipTests package
结果：通过

npm run build
结果：通过，有 Vite / Rollup 非阻塞 warning

docker compose -f deploy/docker-compose.yml config
结果：通过

docker compose -f deploy/docker-compose.dev.yml config
结果：通过

docker info --format '{{.ServerVersion}}'
结果：未通过，本机 Docker Desktop Linux engine 未启动
```

说明：

- 本次没有进入 MVP1 文件扫描/归集业务，仍然只做 MVP0 工程骨架。
- Core 持有 MVP0 Flyway migration，`system_settings` 和 `model_providers` 表归属 Core。
- Worker 暂不直连数据库，后续通过 Core API 或明确的数据归属契约推进任务状态回写。

## 2026-05-23 开发日志与归档快照规则调整

用户补充：同一天内如果多次更新开发日志或归档快照，不希望每次都新增一批文件，避免归档目录快速膨胀。

本次规则调整为：

- 同一日期、同一文档类型，优先维护当天最新版本文件。
- 文件内部追加“版本记录 / Version History”或对应日期小节，记录多次更新内容。
- 文件名版本号随最新内容递增：`v0.1 -> v0.2 -> ... -> v0.9 -> v1.0 -> v1.1`。
- 小版本累计到 10 次时提升大版本，不使用 `v0.10`。
- 归档索引只指向同一天同一类文档的最新版本文件，并在说明中概括多次更新。
- 已经存在的旧归档文件作为历史保留，不批量删除或重写。

同步更新：

```text
AGENTS.md
docs/archive/README.md
docs/ai-skills/wikiforge-development/references/development-workflow.md
```

## 2026-05-23 开发日志长文件索引规则补充

用户补充：如果同一文件持续迭代，文件内容会越来越长，AI 每次读取完整文件会占用大量上下文。

本次规则追加：

- 长归档文件顶部必须维护“版本索引 / Version Index”。
- 版本索引记录最新版本、最新小节、推荐阅读范围和历史版本读取条件。
- AI 默认只读版本索引和最新版本小节。
- 只有需要追溯历史决策、需求演进、架构争议，或用户明确要求时，才读取旧版本内容。

本次同步更新：

```text
AGENTS.md
docs/archive/README.md
docs/ai-skills/wikiforge-development/references/development-workflow.md
```

## 2026-05-23 并行开发 init 检查与专家编排规则

用户要求在进入并行开发前做 init 检查，并学习 `awesome-codex-subagents` 的专家池式任务拆解思路。

本次并行预检结论：

- 当前后端、前端、部署骨架可以作为并行开发基础。
- 但不建议立刻多 Agent 同时开写 MVP1。
- 前置条件一：先提交或冻结当前 MVP0 骨架，避免子 Agent / worktree 拿不到未跟踪文件。
- 前置条件二：先串行冻结 MVP1 契约，包括 REST API 前缀、Core -> Worker DTO、`sources` / `source_files` / `import_jobs` DDL、状态枚举、路径安全策略。

本次学习 `awesome-codex-subagents` 后吸收的原则：

- 并行不是让 Agent 自由发挥，而是主编排 Agent 显式拆任务、选专家、分配文件边界并最终集成。
- 子 Agent 使用独立上下文，主编排 Agent 提供最小上下文包。
- 专家角色按任务选择，不追求一次启用大量专家。
- 高冲突文件必须串行修改。
- 子 Agent 统一输出 Handoff Packet，由主编排 Agent 汇总验证。

本次同步更新：

```text
AGENTS.md
docs/ai-skills/wikiforge-development/SKILL.md
docs/ai-skills/wikiforge-development/references/multi-agent-collaboration.md
docs/ai-skills/wikiforge-development/references/development-workflow.md
docs/ai-skills/wikiforge-development/references/agent-role-prompts.md
```

## 2026-05-23 版本标签 0.02 与更新内容补充

用户要求在合并到 `main` 后生成对应版本标签，并补充本次更新内容。

本次处理：

- 将 `codex/mvp0-project-skeleton` 快进合并到 `main`。
- 提交 MVP0 工程骨架与 Agent 协作基线，提交号为 `f76893a`。
- 计划创建版本标签 `0.02`，用于标记 MVP0 工程骨架可开发基线。
- 新增版本更新记录：

```text
docs/2026-05-23-版本更新记录-WikiForge-release-notes.md
docs/archive/2026-05-23/2026-05-23-版本更新记录-WikiForge-release-notes-v0.1.md
```

版本 `0.02` 的核心范围：

- 少服务微服务骨架。
- Core / Worker / UI / MySQL Compose 拓扑。
- GitHub Actions CI。
- Dockerfile 与 Compose 发布配置。
- WikiForge AI 开发 Skill。
- 并行开发和 Git 提交规则。

边界说明：

- `0.02` 不包含 MVP1 文件扫描、归集、解析和 Obsidian 写入业务。
- 下一阶段应先冻结 MVP1 API、DTO、DDL、状态枚举和路径安全策略，再开始并行开发。

## 2026-05-23 MVP1 契约冻结 Parallel Work Order

用户要求接着此前规划，先创建 MVP1 契约冻结 Parallel Work Order，完成后再正式派 Core、Worker、UI、DevOps、Test/Review 多路并行开工。

本次处理：

- 已推送 `main` 和标签 `0.02` 到远程仓库。
- 已从 `main` 创建开发分支 `codex/mvp1-source-ingestion`。
- 已暂停提前实现接口的动作，避免契约未冻结前进入编码。
- 已创建 MVP1 契约冻结并行工作单：

```text
docs/superpowers/plans/2026-05-23-MVP1契约冻结并行工作单-WikiForge-mvp1-contract-freeze-parallel-work-order.md
docs/archive/2026-05-23/2026-05-23-MVP1契约冻结并行工作单-WikiForge-mvp1-contract-freeze-parallel-work-order-v0.1.md
```

本次冻结内容：

- Core / Worker / UI / DevOps 服务边界。
- UI -> Core、Core -> Worker、Worker -> Core API。
- DTO 名称。
- `ImportJobStatus`、`ImportType`、`OrganizeMode`、`SourceStatus`、`ParseStatus`、`RawOrganizeStatus`。
- 错误码。
- 路径安全规则。
- Flyway migration 编号：`V20260523_002__create_source_import_tables.sql`。
- 并行任务拆分和合并顺序。

下一步：

- 先完成 `PWO-MVP1-CF-001` 和 `PWO-MVP1-COMMON-001`。
- 然后按 PWO 派发 Core、Worker、UI、DevOps、QA/Review 并行实现。

## 2026-05-23 MVP1 并行开工与 QA 契约收口

用户确认 PWO 已经提交，可以正式派 Core、Worker、UI、DevOps、Test/Review 多路并行开工。

本次执行：

- 已提交 PWO 基线：`74cb8e1 docs: freeze mvp1 parallel work order`。
- 已完成 Common 串行区：`7b9f807 feat: add common error response and path safety`。
- 已派发 Core、Worker、UI、DevOps、Test/Review 五路 Agent。
- Test/Review Agent 提前发现 P1 契约缺口，并已由主编排收口更新 PWO。

QA 触发的契约补充：

- 去重归属：Core 是持久化去重关系的唯一权威，Worker 负责 hash 和本次 run 内复制去重。
- DDL：补充 `import_jobs`、`sources`、`source_files` 完整字段、索引和外键约束。
- 路径安全：补充 allowed roots、rawSourcesRoot 配置一致性、no-follow symlink / junction 策略。
- 内部 API：Worker -> Core 内部回调统一使用 `X-WikiForge-Internal-Token`。
- 环境变量：新增 `WIKIFORGE_INTERNAL_API_TOKEN`，统一 `WIKIFORGE_RAW_SOURCES_ROOT` 和 `WIKIFORGE_WORKER_BASE_URL`。
- 大文件策略：`maxFileSizeMb` 收敛为 `maxCopyFileSizeMb`，超限文件不复制，标记 `need_confirm` 或计入 skipped。

同步通知：

- Core Agent 已收到字段、DDL、内部 token、去重归属和 runtime config 更新。
- Worker Agent 已收到字段、去重归属、内部 token、大文件策略和 symlink 策略更新。
- UI Agent 已收到 `maxCopyFileSizeMb` 和 `ApiResponse.code` 更新。
- DevOps Agent 已收到 `WIKIFORGE_INTERNAL_API_TOKEN` 更新。

## 2026-05-23 MVP1 并行集成与 Core-Worker 闭环

在并行 Agent 返回后，主编排进行集成自检，发现 MVP1 不能只停留在创建 pending job：Core 必须在持久化任务后派发 Worker，才能完成“先把杂乱源文件整理到 Raw Sources”的 MVP 核心闭环。

本次补充：

- Core 新增 `WorkerImportJobClient`，通过 `WIKIFORGE_WORKER_BASE_URL` 调用 Worker `/api/v1/worker/import-jobs/local/run`。
- Core 创建本地导入任务后，在事务提交后派发 Worker，避免 Worker 回调时读不到尚未提交的 `import_jobs`。
- Worker Compose 环境补齐 `WIKIFORGE_CORE_SERVICE_BASE_URL`，避免容器内默认回调 `localhost:8080`。
- Core / Worker `application.yml` 补齐 Raw Sources、Worker base URL、Core base URL 和内部 token 配置。
- Compose 将 `WIKIFORGE_INTERNAL_API_TOKEN` 本地默认值调整为 `change-me`，避免空 token 导致 Worker 回调被 Core 拒绝；正式部署必须覆盖为真实 token。
- MVP1 PWO 追加说明：UI 只调用 Core，Core 负责派发 Worker。

本次归档版本：

```text
docs/archive/2026-05-23/2026-05-23-开发者日志-WikiForge-developer-log-v1.5.md
docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-v1.5.md
docs/archive/2026-05-23/2026-05-23-MVP1契约冻结并行工作单-WikiForge-mvp1-contract-freeze-parallel-work-order-v0.3.md
```

## 2026-05-23 MVP1 本地端到端烟测与稳定性修复

用户确认本地 Docker 已启动后，开始 MVP1 第一轮本地验收。完整 Docker Compose 镜像构建在拉取基础镜像阶段受 Docker Hub `auth.docker.io:443` 网络连接影响未完成，因此先采用本地 MySQL 容器 + 本地 Core / Worker Jar 的方式验证 MVP1 业务闭环。

本次验收发现并修复：

- MySQL 8 中 `recursive` 属于保留关键字，Flyway 执行 `V20260523_002__create_source_import_tables.sql` 失败；已改为数据库字段 `recursive_scan`，同时补充迁移 SQL 兼容性测试。
- MyBatis Plus 自动映射曾生成 `recursive_scan AS recursive`，仍会触发 MySQL 保留字问题；已将持久化实体字段调整为 `recursiveScan`，避免生成保留字别名。
- Worker 使用默认 HTTP request factory 时不支持 `PATCH` 回调；已改为 `JdkClientHttpRequestFactory`，并调整测试注入方式。
- Core 的 `jobUid` 使用进程内自增序号，服务重启后会重新从 `job_yyyyMMdd_000001` 开始，和数据库已存在任务撞唯一索引；已改为 `job_yyyyMMdd_<12位uuid>`，降低重启、并发和未来多实例下的碰撞风险。
- MVP1 契约示例中的 `jobUid` 样式已同步为随机后缀格式。

本地端到端验收结果：

```text
Core health: UP
Worker health: UP
Flyway: 20260523.001 / 20260523.002 success
JobUid: job_20260523_006ea8a44e17
Status: completed
TotalCount: 5
SuccessCount: 4
SkippedCount: 0
FailedCount: 0
SourceFileTotal: 5
OrganizedFiles: 4
Frontend dev server: http://localhost:3000
Frontend proxy /api/v1/import-jobs: success
```

用户补充 Docker 已登录成功并可拉取 `hello-world` 后，继续补做容器级验收。

本次 Docker 验收结果：

```text
docker pull maven:3.9.9-eclipse-temurin-21: success
docker pull eclipse-temurin:21-jre-alpine: success
docker pull node:22-alpine: success
docker pull nginx:1.27-alpine: success
docker compose build wikiforge-ui: success
docker compose build wikiforge-worker-service: success
docker compose build wikiforge-core-service: success
docker compose up --no-build: success
mysql: healthy
wikiforge-core-service: healthy
wikiforge-worker-service: healthy
wikiforge-ui: healthy
```

容器级端到端导入验收：

```text
Entry: http://localhost:3000/api/v1/import-jobs/local
InputPath: /data/wikiforge/imports/test-input
RawSourcesRoot: /data/wikiforge/raw-sources
JobUid: job_20260523_5c052ba13a89
Status: completed
TotalCount: 5
SuccessCount: 4
SkippedCount: 0
FailedCount: 0
SourceFileTotal: 5
```

本次额外修复：

- `wikiforge-ui` 容器内 healthcheck 原先使用 `http://localhost/`，在 Nginx 仅监听 IPv4 时会被容器内 `wget` 解析到 IPv6 loopback 并出现 connection refused；已改为 `http://127.0.0.1/`。
- 验证后 UI 容器健康状态恢复为 `healthy`。

## 2026-05-23 版本 0.03 发布说明与 main 合并准备

用户要求将当前 MVP1 分支通过合并到 `main`，并新建版本标签、补充版本说明。

本次版本判断：

- 现有标签：`0.01`、`0.02`。
- `0.02` 是 MVP0 工程骨架与 Agent 协作基线。
- 当前分支已完成 MVP1 本地源文件归集整理闭环，并通过本地 Jar 与 Docker Compose 两轮端到端验收。
- 因此本次发布标签规划为 `0.03`。

本次补充文档：

```text
README.md
docs/2026-05-23-版本更新记录-WikiForge-release-notes.md
docs/archive/2026-05-23/2026-05-23-README-v0.6.md
docs/archive/2026-05-23/2026-05-23-版本更新记录-WikiForge-release-notes-v0.2.md
docs/archive/2026-05-23/2026-05-23-开发者日志-WikiForge-developer-log-v1.6.md
docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-v1.6.md
```

版本 `0.03` 说明：

- 完成 Core / Worker / UI 的本地源文件导入链路。
- 新增 `import_jobs`、`sources`、`source_files`。
- 支持路径安全校验、Raw Sources 归集、基础目录分类和重复文件识别。
- 修复 MySQL 8 保留字、Worker PATCH、jobUid 重启碰撞和 UI healthcheck 问题。
- Docker Compose 下 `mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui` 全部 healthy。

版本边界：

- `0.03` 是 MVP1 源文件收集整理闭环。
- 尚未包含文档正文解析、Source Note 生成、Obsidian 写入、MCP、向量库和多 Agent 知识提炼。

样例目录包含 5 个文件，其中 1 个 Markdown 与另一个文件内容重复。验收结果符合预期：5 条源文件记录入库，4 个文件复制到 Raw Sources，1 个文件标记为 `duplicate` 并复用已复制文件的 managed path。

本次归档版本：

```text
docs/archive/2026-05-23/2026-05-23-开发者日志-WikiForge-developer-log-v1.5.md
docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-v1.5.md
docs/archive/2026-05-23/2026-05-23-MVP1契约冻结并行工作单-WikiForge-mvp1-contract-freeze-parallel-work-order-v0.3.md
```
