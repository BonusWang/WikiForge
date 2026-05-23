# WikiForge 服务边界 Service Boundaries

## MVP 服务边界

### wikiforge-ui

职责：

- 提供 Web UI 看板。
- 展示导入任务、Source 列表、审核队列、Obsidian 预览。
- 调用 Core API，不直接调用数据库。
- MVP 阶段不直接调用 Worker，除非 Core 暴露转发接口。

### wikiforge-core-service

职责：

- 系统配置。
- Source、SourceFile、ImportJob、ReviewItem、ObsidianNote 元数据管理。
- 接收 UI 请求。
- 创建导入任务并调用 Worker 执行。
- 管理任务状态、审核状态、Obsidian 映射。
- 暴露对外 REST API。

Core 拥有：

- `system_settings`
- `model_providers`
- `sources`
- `source_files`
- `import_jobs`
- `obsidian_notes`
- `review_items`

### wikiforge-worker-service

职责：

- 执行本地路径扫描。
- 复制和归集 Raw Sources 文件。
- 计算 SHA-256。
- 识别文件类型、大小、基础元数据。
- 后续执行文档解析、轻量 OCR、Source Note 草案生成前置处理。

Worker 不拥有用户交互 API。Worker 通过 Core 接收任务，处理完成后回写 Core API 或归属表。

Worker 拥有或写入：

- `import_job_steps`，后续可引入。
- `source_contents`，MVP 3 后引入。
- 文件系统 Raw Sources。

### wikiforge-orchestration-service

职责：

- 管理开发编排任务、Agent 状态、工作区、验证命令和 Handoff。
- 为 `wikiforge-orchestration-ui` 提供只读任务控制台 API。
- 后续同步 GitHub Issue、管理 worktree、记录 runner 心跳和执行日志。

Orchestration 不拥有知识库业务对象，不直接读写 Source、SourceFile、ReviewItem、ObsidianNote 等业务表。

第一版不自动执行本机命令，不自动创建、关闭或修改 GitHub Issue。

### wikiforge-orchestration-ui

职责：

- 展示开发任务看板、状态统计、当前节点、验证命令和 Handoff 要求。
- 调用 Orchestration Service，不直接调用 Core / Worker / 数据库。

## 后续服务边界

### wikiforge-agent-service

- 模型调用。
- Normalize / Classify / Summarize / Generate Note。
- Agent run 和 step 记录。
- Review 队列候选生成。

### wikiforge-connector-service

- 飞书、腾讯文档、网页、微信收藏、B 站、知乎等连接器。
- OpenClaw / Hermes / Native Connector 适配。

### wikiforge-mcp-service

- WikiForge MCP Server。
- 外部 Agent tool 调用日志。
- `create_source`、`search_sources`、`get_obsidian_note` 等工具。

### wikiforge-vector-service

- Source Note / Wiki Page 分块。
- Embedding job 状态。
- 向量库导出或写入。

### wikiforge-record-service

- 消费、账单、邮件、人际关系、个人事件记录。
- Personal Record 标准化与总结。

## 跨服务约定

- UI 只面向 Core 或 Gateway。
- Core 是 MVP 对外业务 API 入口。
- Worker 只做任务执行，不持有 UI 状态。
- 后续 Gateway 统一 `/api/v1/{domain}` 路由。
- 每个服务的新增表必须写明归属。
- 跨服务同步调用必须定义 request / response DTO。
- 耗时任务必须有 task id、status、start time、end time、error message。
