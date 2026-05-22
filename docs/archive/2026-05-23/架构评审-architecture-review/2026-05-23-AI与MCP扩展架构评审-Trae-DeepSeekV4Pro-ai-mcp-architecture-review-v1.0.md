# WikiForge 架构评审意见 — AI / MCP 扩展架构专项

## 结论
- 是否建议进入 MVP 编码：是（有条件）

**核心理由**：当前架构在 MCP、Agent 编排、向量库、外部机器人接入四个方向的预留设计整体合理，MVP 范围收敛清晰。但有三处关键的"预留接口"实际上停留在口头层面，没有落实到具体的数据模型字段或服务接口约定上——这些接口一旦 MVP 写死，后续 MCP 和 Agent 扩展将面临较大改造成本。

---

## P0 阻塞问题

### P0-1：ModelProviderAdapter 接口未定义，MVP 3 可能写死单一供应商

**现状**：技术架构多次提到"Model Provider Adapter"和"Provider Adapter"抽象，但所有文档中都没有定义这个 Adapter 的接口契约。MVP 3（AI 辅助整理）是该接口在代码中的首次落地，如果 MVP 3 直接写一个 `DeepSeekService` 而不定义 `ModelProvider` 接口，后续接入 MiniMax、OpenAI-compatible、CC Switch 将需要改造 AgentOrchestrator。

**为什么是 P0**：AgentOrchestrator 是后续 MCP、多 Agent 编排的唯一模型调用入口。如果这个入口没有统一的 Provider 抽象，MCP 工具（如 `search_sources` 的语义增强、`generate_summary`）在 V1 切换模型时将无法复用同一套调用逻辑。

**建议（编码前明确）**：

```java
// 必须在 MVP 0 common 包或 domain 包下定义，MVP 3 实现
public interface ModelProvider {
    String getProviderName();
    boolean isEnabled();
    ChatResponse chat(ChatRequest request);         // 非结构化对话
    <T> T generateJson(String prompt, Class<T> schema); // 结构化 JSON 输出
    String generateText(String prompt);              // 自由文本
}

// ChatRequest / ChatResponse 需要包含：
// - model name（支持 per-step 切换）
// - temperature, max_tokens
// - timeout, retry policy
// - trace_id（贯穿 agent_run -> agent_step -> mcp_tool_call）
```

**不入代码仓库的最低要求**：在 DECISIONS.md 中写明 `ModelProvider` 接口契约，并约定 MVP 3 的实现类必须命名为 `DeepSeekProvider implements ModelProvider` 而非直接硬编码在 Orchestrator 中。

---

### P0-2：Agent 流水线当前是线性 Pipe，但 agent_steps 表预留了 step_name 自由枚举——这可能导致多 Agent DAG 扩展时 step_name 语义分裂

**现状**：PRD 定义的流水线是 Ingest → Normalize → Classify → Integrate → Review → Archive（线性）。但数据模型 `agent_steps.step_name` 是 VARCHAR(64)，没有外键约束，没有 step 注册表。MVP 3 在代码中硬编码这 6 个步骤名后，V1/V2 引入"并行 Classify + Summarize"或"子 Agent"时，step_name 将变成自由文本，失去语义一致性。

**为什么是 P0**：MCP 的 `get_agent_status` 工具需要返回当前处理步骤。如果 step_name 没有统一注册，外部 Agent（OpenClaw/Hermes）调用 MCP 时无法理解当前步骤含义，也无法做步骤级别的重试/跳过。

**建议**：在 `agent_steps` 表设计或 Flyway migration 中增加一层轻量约束。两种可选方案：

**方案 A（推荐 MVP，轻量）**：在 `DECISIONS.md` 中约定 `step_name` 的有界值集合，并在 Service 层用枚举校验：

```java
public enum AgentStepName {
    INGEST, NORMALIZE, CLASSIFY, SUMMARIZE, INTEGRATE, REVIEW, ARCHIVE,
    // V1 预留扩展但不实现
    ENTITY_EXTRACT, CONFLICT_CHECK, QUALITY_AUDIT, PARALLEL_CLASSIFY
}
```

**方案 B（V1 再做）**：增加 `agent_step_definitions` 注册表（MVP 不需要建表，但需在设计文档中预留）。

**最低要求**：MVP 3 的 `agent_steps` 写入必须通过枚举校验，禁止裸字符串。MCP `get_agent_status` 的返回格式中 step 字段使用该枚举值。

---

### P0-3：MCP tool schema 只提了"预留"，但未约定 tools 的注册/发现机制，MVP 4 可能建成"硬编码工具列表"

**现状**：PRD 和架构文档都提到"MVP 仅预留 MCP tool schema 和调用日志模型"，数据模型中 `mcp_servers.tools_json` 是 JSON 字段存储工具定义快照。但没有任何文档约定这个 JSON 的 schema 格式。

MCP 协议本身定义了 tool 的 JSON Schema 格式（name、description、inputSchema）。如果 MVP 4 实现时没有遵循 MCP 标准格式，而是自定义一套工具描述格式，后续正式接入 MCP SDK 时需要全部重写。

**为什么是 P0**：MCP 是 WikiForge 最核心的外部扩展接口。OpenClaw/Hermes 机器人、向量库工具、外部 Agent 都通过 MCP 接入。如果 tool schema 格式一开始就走偏，MVP 4 的 `create_source`、`search_sources` 等工具将无法被标准 MCP Client 调用。

**建议**：

在 DECISIONS.md 或 MCP 相关设计文档中明确：

1. `mcp_servers.tools_json` 存储格式遵循 [Model Context Protocol 标准 Tool 定义](https://spec.modelcontextprotocol.io/specification/2024-11-05/server/tools/)：

```json
[
  {
    "name": "search_sources",
    "description": "按关键词、项目、主题、标签、状态查询 Source",
    "inputSchema": {
      "type": "object",
      "properties": {
        "query": { "type": "string", "description": "搜索关键词" },
        "project": { "type": "string" },
        "topic": { "type": "string" },
        "status": { "type": "string", "enum": ["pending", "archived", "rejected"] }
      },
      "required": ["query"]
    }
  }
]
```

2. MVP 4 的 MCP Server 实现直接使用 `io.modelcontextprotocol:java-sdk` 官方 SDK（已在技术架构中提及），不做自研 MCP 协议层。

3. MVP 预留的 MCP tool 列表应在 DECISIONS.md 或 MCP 设计文档中明确（非代码实现）：

| Tool Name | MVP 状态 | 说明 |
|---|---|---|
| `search_sources` | MVP 4 实现 | 查询 Source |
| `get_source` | MVP 4 实现 | 读取 Source 详情 |
| `create_import_job` | MVP 4 实现 | 创建导入任务 |
| `get_agent_status` | MVP 4 实现 | 查询 Agent 状态 |
| `get_review_items` | MVP 4 实现 | 查询审核队列 |
| `get_obsidian_note` | MVP 4 实现 | 读取 Obsidian Note |
| `open_obsidian_note` | MVP 4 实现 | 返回 obsidian://open URI |
| `create_personal_record` | MVP 4 实现 | 写入个人记录（简化存储） |
| `search_personal_records` | V1 实现 | 查询个人记录 |
| `get_project` | V1 预留 | 读取项目详情 |
| `get_topic` | V1 预留 | 读取主题详情 |
| `trigger_vector_export` | V2 预留 | 触发向量导出 |

---

## P1 高风险问题

### P1-1：Agent Orchestrator 与 MCP Server 共享同一个 Source 模型但缺少统一的知识对象抽象层

**现状**：AgentOrchestrator（MVP 3）处理 Source 生成草案，MCP Server（MVP 4）暴露 `search_sources` / `get_source`。两者操作同一个 `sources` 表。当前设计没有问题——但缺少一个"知识对象"的统一抽象层。

长期来看，MCP Server 不仅需要暴露 Source，还需要暴露 Project、Topic、Entity、ObsidianNote。如果每个对象类型各写一套查询逻辑，MCP tools 的实现将高度冗余。

**建议（设计层面，不要求 MVP 实现）**：

在技术架构 MCP Service 章节中补充：

```text
MCP Service 内部通过 KnowledgeObjectResolver 统一解析不同类型的知识对象：

interface KnowledgeObjectResolver<T> {
    String getObjectType();  // "source", "project", "topic", "entity"
    T resolve(Long id);
    List<T> search(String query, Map<String, String> filters);
    Map<String, Object> toMcpResult(T object);  // 转为 MCP tool 返回格式
}
```

MVP 只需要一个 `SourceResolver implements KnowledgeObjectResolver`。后续增加 `ProjectResolver`、`TopicResolver` 时只需注册新实现。

**不阻塞 MVP**：MVP 可以直接在 MCP Service 中写死 Source 查询逻辑。但建议在代码注释中标记 `// TODO: extract to KnowledgeObjectResolver pattern for V1`。

---

### P1-2：Vector Export Service 的数据模型预留了 `content_chunks` 表，但 chunk 的"所有权"模型不清晰——Source vs ObsidianNote vs Agent 合成内容

**现状**：`content_chunks` 表通过 `related_type` + `related_id` 关联到 Source 或 ObsidianNote。V2 的场景中，向量化应该基于"知识对象"而不仅仅是"原始 Source"。例如：
- 用户编辑过的 Obsidian Note（可能融合了多个 Source）需要重新分块
- Agent 生成的 Topic 总结页（非 Source 也非 Note）需要向量化
- 个人记录（PersonalRecord）需要纳入语义检索

当前设计中 `related_type` 的可选值（source、note、project、topic、entity）已经覆盖了这些场景。但 `source_id` 和 `note_id` 两个独立外键造成了"必须知道原始 Source 或 Note 才能检索"的隐含约束。

**建议**：在数据模型文档中明确 `content_chunks` 的使用规则：

```text
规则：
- 当 related_type = 'source' 时，source_id 必填，note_id 为 NULL
- 当 related_type = 'note' 时，note_id 必填，source_id 为 NULL
- 当 related_type = 'project'/'topic'/'entity' 时，source_id 和 note_id 均为 NULL
- 一个知识对象可以有零到多个 chunk
- chunk 的 chunk_hash 变更（内容更新）时，embedding_status 自动变为 'stale'
```

**不阻塞 MVP**：content_chunks 表是预留表，MVP 不建表不写数据。但上述规则应在 DECISIONS.md 中记录，避免 V2 实现时理解偏差。

---

### P1-3：Personal Record 数据模型已为 MCP 接入做了良好预留，但缺少"批量写入"的语义定义

**现状**：OpenClaw/Hermes 机器人通过 MCP 调用 `create_personal_record` 写入记录。MVP 4 的简化实现只做单条存储。但实际使用中，机器人可能在短时间内写入大量记录（如一次消费会话产生多条记录、邮件批处理导入）。

如果 MVP 4 的 `create_personal_record` 只支持单条写入，V1 扩展时 API 契约变更会影响已有 MCP Client。

**建议**：在 MCP tool 定义阶段（DESIGN，非实现）就约定 `create_personal_record` 支持批量：

```json
{
  "name": "create_personal_record",
  "inputSchema": {
    "type": "object",
    "properties": {
      "records": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "record_type": { "type": "string", "enum": ["expense", "bill", "email", "relationship", "event"] },
            "title": { "type": "string" },
            "occurred_at": { "type": "string", "format": "date-time" },
            "source_channel": { "type": "string" },
            "source_ref": { "type": "string" },
            "raw_content": { "type": "string" },
            "structured_json": { "type": "object" }
          },
          "required": ["record_type", "title", "occurred_at"]
        }
      }
    },
    "required": ["records"]
  }
}
```

MVP 4 实现时 `records` 数组可以先只处理第一条（或少量），但 API 契约从一开始就支持数组，V1 扩展时无需变更。

---

### P1-4：Agent 流水线的"人类介入"机制当前通过 review_items 实现，但缺少 OpenClaw/Hermes 等外部 Agent 触发审核回调的约定

**现状**：MVP 3 的审核流程是用户通过 Web UI 手动审核。V2 场景中，外部 Agent（如 OpenClaw）可能希望在审核完成后收到回调通知，或者外部 Agent 本身成为审核者（自动审核低风险内容）。

当前 review_items 表没有预留"外部审核者"字段，也没有审核完成后的 webhook/callback 机制。

**建议**：在 review_items 表的预留字段中增加（MVP 不实现，但 Flyway migration 预留）：

```sql
-- V1 预留字段，MVP 不写入
reviewer_type VARCHAR(64) DEFAULT 'human',  -- 'human', 'external_agent', 'auto'
reviewer_id VARCHAR(128),                    -- 外部 Agent ID / 用户标识
callback_url TEXT,                           -- 审核完成后通知地址
```

**不阻塞 MVP**：MVP 3 的 review_items 不需要这些字段，也不需要在 V1 migration 之前建表。但在 DECISIONS.md 中记录这个扩展方向。

---

### P1-5：MCP Client（连接外部 MCP Server）方向预留不足——当前所有文档聚焦 WikiForge 作为 MCP Server，但 WikiForge 作为 MCP Client 接入外部工具的场景没有预留

**现状**：PRD 第 15.2 节提到"MCP Client 先预留，V1/V2 再用于连接外部工具和资料源"。技术架构 3.10 节也提到"预留 MCP Client，用于连接外部 MCP Server"。但数据模型中没有 MCP Client 相关的配置表（只有 `mcp_servers` 作为 Server 配置），也没有外部工具调用的日志表。

WikiForge 作为 MCP Client 的典型场景：
- 连接文件系统 MCP Server 做高级文件操作
- 连接浏览器 MCP Server 做网页抓取
- 连接飞书 MCP Server 读取飞书文档
- AgentOrchestrator 调用外部 MCP 工具作为流水线步骤

**建议**：在 DECISIONS.md 中记录：

```text
MCP Client 方向（V2）：
- AgentOrchestrator 的 step 可以调用外部 MCP 工具
- agent_steps 表通过 step_type 区分：'internal_llm' | 'mcp_tool_call' | 'human_review'
- mcp_tool_calls 表的 caller_type 已有 'agent' 枚举，V2 时 Agent step 可写入该表
- MCP Client 的连接配置复用 mcp_servers 表（server_type = 'external'）
```

当前数据模型实际上已经可以支撑这个场景：`mcp_servers` 的 `server_type` 有 `internal` / `external` 区分，`mcp_tool_calls` 的 `caller_type` 有 `agent` 值。架构预留是充分的，只是文档中没有显式描述 Client 方向。

**不阻塞 MVP**：在 DECISIONS.md 补充一节"MCP Client 扩展方向"说明即可。

---

## P2 优化建议

### P2-1：建议在 AgentOrchestrator 设计文档中引入"Pipeline 策略模式"预留

当前 Agent 流水线是线性的 6 步。后续可能引入：
- 条件分支（高风险内容走加强审核）
- 并行步骤（同时 Classify + Summarize）
- 子流水线（Project 级别的批量处理）

建议在技术架构 Agent Orchestrator 章节补充：

```text
后续演进方向：
- PipelineStrategy 接口：定义步骤编排逻辑（线性/条件/并行）
- MVP 实现 LinearPipelineStrategy
- V2 可扩展为 ConditionalPipelineStrategy、ParallelPipelineStrategy
```

不需要 MVP 实现，但代码中 AgentOrchestrator 的方法签名应避免硬编码 6 步流程。

---

### P2-2：content_chunks 的 embedding_status 枚举建议增加 `skipped` 状态

当前 `embedding_status` 枚举：`not_ready` → `pending` → `embedded` → `stale` → `failed`。

建议增加 `skipped`：用户显式标记某个 chunk 不参与向量化（如包含个人信息、低质量内容、纯元数据页面）。这对于后续向量检索质量有重要意义——避免低质量内容污染语义搜索。

---

### P2-3：MCP tool 日志 `mcp_tool_calls` 建议增加 `duration_ms` 字段

当前表有 `started_at` 和 `finished_at`，可以计算耗时。但作为日志表，直接冗余一个 `duration_ms INT` 字段可以：
- 方便 SQL 直接做性能分析（`SELECT AVG(duration_ms) FROM mcp_tool_calls WHERE tool_name = 'search_sources'`）
- MCP 监控面板直接展示慢调用

建议在 V1 实现时增加，MVP 4 可预留列但不写入。

---

### P2-4：建议模型供应商的 prompt 管理独立于代码

当前架构中 `agent_steps` 有 `prompt_version` 字段（好的设计），但 prompt 内容本身没有管理位置。随着 Agent 步骤增多和模型切换，prompt 的版本管理和 A/B 测试会成为需求。

建议方向（不要求 MVP 实现）：
- `90_System_系统/Prompts_提示词/` 目录存放 prompt 模板（Markdown + frontmatter）
- 或者数据库增加 `prompt_templates` 表

MVP 可以硬编码 prompt，但在技术架构中标记这个扩展点。

---

### P2-5：OpenClaw/Hermes 写入方向的身份溯源

PRD 提到 OpenClaw/Hermes 通过 MCP 写入个人记录。`personal_records.created_by` 字段已预留了 `openclaw`、`hermes` 值。但 `source_channel` 和 `source_ref` 的语义需要进一步明确，以便后续做"这条记录是哪个机器人、哪次会话写入的"溯源。

建议在 PRD 或数据模型文档中补充：

```text
source_channel: 写入渠道。取值包括 web_ui, openclaw, hermes, mcp, email_connector
source_ref: 来源引用。格式取决于渠道：
  - openclaw: "openclaw:{session_id}:{message_id}"
  - hermes: "hermes:{task_id}:{step_id}"
  - mcp: "mcp:{server_id}:{call_uid}"
  - web_ui: "web_ui:{timestamp}"
```

---

### P2-6：MCP Server 的 Obsidian URI 生成安全

P1 已由后端评审覆盖了 `obsidian://open` URI 路径编码问题（Trae 后端评审 P1-5）。作为 MCP 扩展视角补充：如果 `open_obsidian_note` MCP tool 被外部 Agent 调用，URI 中的 `vault` 参数应该从 `system_settings` 读取，**不允许 MCP 调用方自行指定 vault 名称**，防止恶意调用者构造指向其他 Vault 的 URI。

---

## MVP 范围建议

### 建议保留：
- MVP 0：项目骨架（Spring Boot + Vue + MySQL + Flyway）
- MVP 1：源文件归集（路径扫描、复制、hash 去重、文件类型识别）
- MVP 2：Obsidian Source Note（Vault 初始化、模板、frontmatter、Markdown 预览、obsidian://open）
- MVP 3：AI 辅助整理（单 LLM 多步骤：Normalize → Classify → Summarize → 生成草案 → 人工审核）
- MVP 4：轻量 MCP 预览版（search_sources、get_source、create_import_job、create_personal_record）

### 建议移出：
- **无**。当前 MVP 范围已经足够收敛，不建议进一步缩小。如果必须压缩，优先把 MVP 4（MCP 预览版）移到 V1 的早期迭代，让 MVP 0-3 构成完整的"文件归集 + Obsidian 沉淀 + AI 辅助"闭环。

### 建议新增：
- **ModelProvider 接口契约**（P0-1，编码前写入 DECISIONS.md）
- **AgentStepName 枚举定义**（P0-2，编码前写入 DECISIONS.md）
- **MCP Tool Schema 标准格式约定**（P0-3，编码前写入 DECISIONS.md）

## 技术栈建议

### 后端：
- Java 21 + Spring Boot 3.x + Maven：**合理，无需调整**
- MyBatis-Plus：**合理**，JSON 列的 TypeHandler 需要在 MVP 0 统一注册（复用后端评审 P1-3 建议）
- ModelProviderAdapter：**需要在 MVP 0 定义接口**（P0-1），MVP 3 首版实现 DeepSeekProvider
- MCP SDK：**MVP 4 使用 `io.modelcontextprotocol:java-sdk`**，MVP 0-3 不引入该依赖

### 前端：
- Vue 3 + Vite + TypeScript + Element Plus：**合理，无需调整**

### 数据库：
- MySQL 8.x + Flyway：**合理**。Flyway migration 分段策略建议参考后端评审 P0-3（按 MVP 阶段分批建表）

### 文件解析：
- commonmark-java（Markdown）、Apache POI（Word）、Apache PDFBox（PDF）：**合理**
- **不建议 MVP 引入 Apache Tika**（依赖过大，对 MVP 场景过度）

### AI/LLM 调用：
- 模型调用通过统一的 `ModelProvider` 接口（P0-1），首个实现为 DeepSeek
- 结构化输出优先要求 JSON Schema
- API Key 从环境变量读取，不入代码仓库
- ModelProviderAdapter 需要提供 `trace_id` 贯穿全链路

### MCP：
- MVP 0-3：**不引入 MCP SDK**，仅在设计文档中约定 tool schema
- MVP 4：引入官方 `io.modelcontextprotocol:java-sdk`，实现 WikiForge MCP Server
- MCP Client 方向不阻塞 MVP，V2 再实现

## 数据模型建议

### 需要保留：
- `sources`：核心表，MVP 1 必建。建议采纳后端评审 P0-1（raw_text 拆到 source_contents）
- `source_files`：核心表，索引设计参考 DBA 评审建议
- `import_jobs`：核心表，建议采纳后端评审 P1-1（增加 cancel 状态）
- `obsidian_notes`：核心表，MVP 2 必建
- `agent_runs` / `agent_steps`：Agent 流程账本，MVP 3 必建
- `review_items`：审核队列，MVP 3 必建
- `model_providers`：模型配置，MVP 0 必建（即使只有一条 DeepSeek 记录）
- `system_settings`：系统配置，MVP 0 必建

### 需要收敛：
- `sources` 表中与在线文档、向量库直接相关的字段（`connector_name`、`connector_status`、`connector_trace_id`）建议在 MVP migration 中建列但不建索引，待 V1 启用
- `agent_steps.step_name` 必须在 Service 层通过枚举约束（P0-2），不允许自由文本
- `mcp_servers.tools_json` 必须遵循 MCP 标准 tool 定义格式（P0-3）

### 需要新增：
- `source_contents` 表（拆离 sources.raw_text，采纳后端评审建议）
- `agent_steps` 表 V2 预留字段：`step_type VARCHAR(64) DEFAULT 'internal_llm'`（区分 LLM 调用和 MCP 工具调用）

### MVP 阶段 Flyway migration 分批建议：

```text
V1.0.x（MVP 0）：
  V1.0.001__create_system_settings.sql
  V1.0.002__create_model_providers.sql

V1.1.x（MVP 1）：
  V1.1.001__create_sources.sql
  V1.1.002__create_source_contents.sql    -- raw_text 拆表
  V1.1.003__create_source_files.sql
  V1.1.004__create_import_jobs.sql

V1.2.x（MVP 2）：
  V1.2.001__create_obsidian_notes.sql

V1.3.x（MVP 3）：
  V1.3.001__create_agent_runs.sql
  V1.3.002__create_agent_steps.sql
  V1.3.003__create_review_items.sql

V1.9.x（预留表，V1/V2 启用）：
  V1.9.001__create_content_chunks.sql
  V1.9.002__create_embedding_jobs.sql
  V1.9.003__create_mcp_servers.sql
  V1.9.004__create_mcp_tool_calls.sql
  V1.9.005__create_personal_records.sql
  V1.9.006__create_agent_office_status.sql
```

## AI / MCP 扩展架构专项评价

### MCP 扩展就绪度：良好 ⭐⭐⭐⭐

**优点**：
- PRD 第 15.2 节对 MCP 定位清晰：对外暴露 WikiForge 能力，对内接入外部工具
- 数据模型 `mcp_servers` 区分 internal/external，`mcp_tool_calls` 全覆盖日志
- MVP 4 的 8 个 tool 规划合理，覆盖了查询、导入、审核、Obsidian 打开的核心闭环
- `create_personal_record` 的"先存储不做复杂总结"策略正确，不拖累 MVP

**需要补强**：
- Tool schema 标准格式（P0-3）
- MCP Client 方向文档补全（P1-5）
- MCP tool 批量写入语义（P1-3）

### Agent 编排扩展就绪度：良好 ⭐⭐⭐⭐

**优点**：
- `agent_runs` / `agent_steps` 表设计允许单 LLM 多步骤无损升级为多 Agent 并行
- `agent_steps` 的 input/output JSON 设计使步骤可重放、可审计
- Agent Office Service（V2）的状态模型已预留
- `prompt_version` 和 `model_provider` 字段为后续 prompt 管理和模型切换提供了数据基础

**需要补强**：
- AgentOrchestrator 内部接口抽象（P2-1，Design-only）
- step_name 枚举约束（P0-2）

### 向量库扩展就绪度：良好 ⭐⭐⭐⭐

**优点**：
- `content_chunks` 表设计完整：分块、hash、token_count、元数据、embedding 状态、向量库映射
- `embedding_jobs` 支持批量任务追踪
- `related_type` 的多态设计（source/note/project/topic/entity）允许向量化对象从 Source 扩展到所有知识对象
- "以 Obsidian Wiki 为事实来源"的原则避免向量库成为数据孤岛

**需要补强**：
- chunk 的所有权模型明确（P1-2）
- skipped 状态建议（P2-2）

### OpenClaw / Hermes 接入就绪度：良好 ⭐⭐⭐

**优点**：
- `personal_records` 表设计完整，`created_by` 和 `source_channel` 支持多来源标识
- MCP `create_personal_record` tool 为机器人写入提供了标准通道
- PRD 明确规划了机器人调用 MCP 写入个人记录的流程

**需要补强**：
- `source_ref` 的渠道特定格式约定（P2-5）
- 批量写入语义（P1-3）
- 外部审核回调机制（P1-4，V2 场景）

---

## 最终建议

- **下一步是否可以开始 MVP 0 项目骨架：是（满足以下三个条件后）**

**条件清单**：

1. **DECISIONS.md 补充** — 写入以下四项约定（预计 1 小时，纯文档工作）：
   - ModelProvider 接口契约（Java 接口定义 + 职责边界）
   - AgentStepName 枚举定义（有界值集合 + 扩展规则）
   - MCP Tool Schema 标准格式（遵循 MCP 协议规范 + MVP 4 tool 列表）
   - MCP Client 扩展方向（架构预留说明）

2. **数据模型文档补充** — content_chunks 的所有权规则（P1-2），约 15 分钟

3. **Flyway migration 版本号段规划** — 确认 MVP 0-3 和预留表的分批建表策略（采纳后端评审 P0-3 建议），约 30 分钟

**以上三项均不涉及代码编写，是纯设计文档补全。完成后即可进入 MVP 0 编码。**

**MVP 0 编码时需要注意的三个"不要做"**：

1. **不要** 在 MVP 0 的 Flyway V1 migration 中创建全部 27 张表——按版本号段分批建表
2. **不要** 在 MVP 0-2 中引入 `io.modelcontextprotocol:java-sdk` 依赖——等 MVP 4 再引入
3. **不要** 在 AgentOrchestrator 中直接 hardcode DeepSeek API 调用——先定义 `ModelProvider` 接口

---

## 与其他评审的交叉引用

本评审与以下已有评审存在交叉关注点，已在分析中引用并确认一致性：

| 交叉点 | 后端评审 (Qoder) | DBA 评审 | 本评审立场 |
|--------|-----------------|----------|-----------|
| raw_text 拆表 | P0-1，建议拆为 source_contents | P1-3，不建议存大文本 | **同意后端评审**，采纳拆表方案 |
| content_hash 索引 | P2-2，建议 CHAR(64) | 已包含 | **同意** |
| Flyway 分段 | P0-3，建议按版本号段 | 建议分阶段开发 | **同意后端评审**，本评审细化到具体 migration 文件 |
| import_jobs 状态机 | P1-1，建议增加 cancel | 未涉及 | **同意** |
| obsidian://open 编码 | P1-5，URL 编码细节 | 未涉及 | **同意**，本评审补充 MCP tool 层面的安全约束 |
| MyBatis JSON TypeHandler | P1-3 | 未涉及 | **同意** |
| agent_steps step_name | - | - | **本评审 P0-2 为新增发现** |
| ModelProvider 接口 | - | - | **本评审 P0-1 为新增发现** |
| MCP tool schema | - | - | **本评审 P0-3 为新增发现** |

---

*评审人：Trae (DeepSeek v4 Pro) — AI / MCP 扩展架构专项*
*评审日期：2026-05-23*
*文档版本：v1.0*
