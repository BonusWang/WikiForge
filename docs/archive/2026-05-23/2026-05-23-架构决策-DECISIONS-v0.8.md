# WikiForge 架构决策记录 Architecture Decisions

## 2026-05-23 MVP 范围

MVP 先做本地源文件整理和最小 Obsidian 归档闭环。

暂缓：

- 飞书 / 腾讯文档自动读取
- 完整 MCP Server
- 向量库
- 个人记录完整处理
- 办公室等距视图
- 复杂多 Agent 编排

## 2026-05-23 技术栈

- 后端主栈：Java 21/17 + Spring Boot 3.x
- 前端主栈：Vue 3 + Vite + TypeScript
- 数据库：MySQL 8.x
- 数据访问：MyBatis-Plus 3.5.x，复杂 SQL 可补充 MyBatis XML
- 数据库迁移：Flyway
- UI 组件：Element Plus

选择原因：

- 国内开发主流。
- 文档和生态成熟。
- 便于后续维护。
- 避免 MVP 引入冷门框架和过重基础设施。

## 2026-05-23 知识存储边界

Obsidian 是长期知识正文和人工编辑层。

MySQL 是控制平面、结构化索引库、Agent 流程账本和轻量内容缓存，不作为最终知识正文主库。

Raw Sources 是原始源文件归集目录，MVP 默认复制文件，不移动、不删除原始文件。

## 2026-05-23 产品路线

WikiForge 采用 LLM Wiki + GBrain 融合路线：

- LLM Wiki 表达层：Raw Sources、Obsidian Vault、Source Note、Schema、index、log。
- GBrain 运行层：MySQL、Agent Orchestrator、MCP、向量库、个人记录、定时任务。

MVP 优先表达层最小闭环，V1/V2 再增强运行层。

## 2026-05-23 架构评审最终结论

外部 AI 架构评审整体结论为有条件通过。结合项目目标和 MVP 收敛原则，最终判断：

- 可以进入 MVP 0 项目骨架阶段。
- MVP 0 只做工程骨架、配置、CI/CD、Docker、基础健康检查和数据库迁移基线。
- MVP 1 再做源文件归集。
- MVP 2 再做 Obsidian Source Note。
- MVP 3 再做 AI 辅助整理。
- MVP 4 再做轻量 MCP 预览。

评审中被标为 P0 的 AI/MCP 接口设计问题，不阻塞 MVP 0；这些问题降级为 MVP 3/MVP 4 开工前必须确认的设计约束。

## 2026-05-23 微服务架构选择

用户选择 B 方案：少服务微服务。

决策：

- WikiForge 长期按领域微服务演进。
- MVP 0/1 不采用单体作为目标架构，也不采用完整大微服务体系。
- MVP 0/1 目标服务为 `wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui`、`mysql`。
- `wikiforge-gateway` 可以预留目录和配置，但不强制在 MVP 0/1 运行。
- 后续再拆出 `wikiforge-agent-service`、`wikiforge-connector-service`、`wikiforge-mcp-service`、`wikiforge-vector-service`、`wikiforge-record-service`。

原因：

- Core / Worker 是当前需求中最清晰的领域边界。
- 文件扫描、复制、hash、解析是耗时任务，不应长期和 UI 查询、配置、审核状态挤在同一服务内。
- 少服务模式能支持多人开发维护，同时避免 MVP 过早承担 Nacos、Kafka、Redis、XXL-JOB 等治理成本。

约束：

- MySQL MVP 阶段可以共享实例，但表归属必须明确。
- UI 只调用 Core 或后续 Gateway，不直接调用 Worker 内部接口。
- Worker 只做任务执行，不承载用户查询入口。
- 跨服务先使用 REST + DTO；消息队列、服务注册、调度中心在 V1/V2 再评估。

## 2026-05-23 AI 开发 Skill 决策

参考用户提供的民生 CDP AI 开发范式，WikiForge 建立项目内 AI 开发 Skill：

```text
docs/ai-skills/wikiforge-development/SKILL.md
```

该 Skill 只吸收架构样式和设计理念，不复制 CDP 业务内容。

采用的理念：

- 全局强约束、脚手架规范、条线型服务规范分层。
- 每个服务必须明确职责、包结构、API、数据表归属、异常码和跨服务调用方式。
- 多人或多 AI 并行开发时，必须先声明目标服务、文件边界、依赖契约和验证命令。

不采纳的内容：

- 不采用 CDP 业务模型。
- 不采用 CDP 的 Spring Boot 4.1.x 版本设定。
- 不在 MVP 0/1 引入 Nacos、Kafka、Redis、XXL-JOB、TDSQL、StarRocks 等基础设施。

## 2026-05-23 数据访问决策

MVP 采用 MyBatis-Plus 3.5.x。

原因：

- MVP 前期以单表 CRUD、分页查询、状态更新为主。
- 比纯 MyBatis XML 更快落地。
- 国内 Java 项目使用广泛，维护成本可控。

约束：

- JSON 字段必须统一注册 TypeHandler。
- 分页必须统一使用 MybatisPlusInterceptor + PaginationInnerInterceptor。
- 涉及复杂统计、批处理或性能敏感查询时，可使用 MyBatis XML 或自定义 Mapper。

## 2026-05-23 Flyway 迁移决策

Flyway migration 分阶段创建，禁止 MVP 0 一次性创建全部长期规划表。

路径：

```text
backend/src/main/resources/db/migration/
```

命名：

```text
VYYYYMMDD_NNN__description.sql
```

阶段：

- MVP 0：`system_settings`、`model_providers`。
- MVP 1：`sources`、`source_files`、`import_jobs`。
- MVP 2：`obsidian_notes`，必要时补充轻量审核状态。
- MVP 3：`source_contents`。
- MVP 4：`agent_runs`、`agent_steps`、`review_items`。
- MVP 5：`mcp_servers`、`mcp_tool_calls`。
- V1/V2：`personal_records`、`content_chunks`、`embedding_jobs`、办公室视图相关表。

CI 必须校验 migration 可以在空 MySQL 库上成功执行。

## 2026-05-23 数据模型收敛决策

MySQL 不保存长期知识正文。

决策：

- `sources` 表不直接承载大文本正文。
- `raw_text` 不进入 MVP 1/2 的核心读写路径。
- 解析文本使用独立 `source_contents` 表，在 MVP 3 引入。
- MVP 1/2 读取原文时优先从 `managed_path` 指向的文件读取。
- 在线文档连接器字段、MCP 字段、向量库字段不进入 MVP 0/1 的 Flyway 初始表。

MVP 阶段一个 Source 对应一个 Source File，先用唯一约束保证 1:1；一对多附件关系放到后续阶段。

Source 状态枚举统一为：

```text
pending
organized
processing
pending_review
archived
rejected
failed
```

## 2026-05-23 文件系统安全决策

文件系统能力必须在 MVP 0/1 前明确边界。

规则：

- 扫描根路径必须是绝对路径。
- 不支持通配符扫描路径。
- 扫描路径必须位于 `WIKIFORGE_ALLOWED_SCAN_ROOTS` 允许范围内。
- 后端使用 `Path.toRealPath()` 规范化输入路径和白名单根路径，再用 `Path.startsWith()` 校验。
- 默认不跟随符号链接。
- 如果后续允许跟随符号链接，目标真实路径也必须重新通过白名单校验。
- 默认最大扫描深度为 20。
- 默认大文件解析阈值为 100 MB。
- 默认绝对文件处理上限为 500 MB，超过后跳过并记录原因。
- Hash 算法统一使用 SHA-256，必须流式计算。
- 文件复制采用临时文件 + 原子 rename。
- MVP 不暴露 `move` 模式，只允许 `copy`、`index_only`，可预留 `dry_run`。
- 生成目标文件名时需要清洗非法字符，并处理 Windows 路径长度。
- `obsidian://open` URI 必须使用 Vault 内相对路径，并进行 URL encode。
- Docker 模式下区分容器内路径和宿主机路径，Obsidian URI 使用宿主机可识别路径。

## 2026-05-23 CI/CD 与 Docker 决策

MVP 0 必须同步建设可自动迭代的工程骨架。

仓库结构采用：

```text
WikiForge/
  backend/
    pom.xml
    wikiforge-common/
    wikiforge-core-service/
    wikiforge-worker-service/
    wikiforge-gateway/        # 后续预留
  frontend/
  deploy/
  .github/workflows/
```

Docker 采用少服务微服务镜像：

- `wikiforge-core-service`
- `wikiforge-worker-service`
- `wikiforge-ui`
- `mysql:8`

不把前端静态资源打进 Spring Boot jar。

CI/CD 最小检查：

- 后端 `mvn test`。
- 后端 `mvn package`。
- Flyway 空库迁移校验。
- 前端 `npm ci`。
- 前端 `npm run build`。
- Docker build。
- Docker Compose smoke test。

运行时路径、API Key 和数据库配置必须通过环境变量或外部配置注入，不能写死在镜像中。

## 2026-05-23 AI / MCP 延后决策

AI 辅助整理不进入 MVP 0/1/2 的编码范围。

MVP 4 AI 辅助整理首轮已确认：

- 模型调用契约先落在 Core Service，字段包括 `providerName`、`modelName`、`baseUrl`、`providerType`、`configSource`。
- Agent 账本使用 `agent_runs`、`agent_steps`、`review_items`。
- MVP4 首轮步骤名为 `draft_review`，后续再扩展 Normalize / Classify / Integrate。
- MiniMax 通过 OpenAI-compatible `/chat/completions` 接入，密钥只读环境变量。
- Review Queue 与 Obsidian 写入的交接规则仍采用“先人工审核，再改写知识层”；自动写入放到下一节点。

MVP 5 前必须确认：

- MCP Tool Schema 标准格式。
- 是否使用官方 `io.modelcontextprotocol:java-sdk`。
- 首批工具列表：`search_sources`、`get_source`、`create_import_job`、`get_obsidian_note`、`open_obsidian_note`。
