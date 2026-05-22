# WikiForge 架构评审意见 — 前端产品体验

**评审角色**：前端产品体验评审
**评审范围**：MVP Web UI 页面是否够用
**评审日期**：2026-05-23

---

## 结论

- **是否建议进入 MVP 编码**：有条件

MVP UI 核心闭环页面已覆盖，但存在 2 个需要开工前确认的 UX 缺口，6 个建议优化的交互细节。MVP 页面可支持"配置 → 扫描 → 归集 → 预览 → 审核 → 打开 Obsidian"闭环，但部分跨页状态衔接和 MVP 0 骨架范围需要明确。

---

## P0 阻塞问题

### P0-1：Source Note 生成与 Obsidian 写入的交接流程在 UI 层缺失显式引导

**问题描述**：当前 PRD/MVP 计划中，"生成 Source Note 草案"和"写入 Obsidian"是两个独立操作，中间需要一次人工审核。但文件列表页、Source 详情页和审核队列页之间如何衔接没有明确说明——用户在一堆文件列表里点哪个按钮进入审核？审核通过后谁来触发 Obsidian 写入？

**影响**：MVP 闭环断点在审核到写入之间。

**建议**：
- 在 Source 详情页明确"进入审核"按钮，点击后状态流转到 `review_items`，审核队列页才出现这条记录。
- 审核通过后明确下一步：自动写入 Obsidian，还是用户手动点"写入"？MVP 建议人工点一次"写入 Obsidian"，不要全自动，避免用户困惑。
- Source 详情页需要有明确的当前状态和可用操作提示（当前状态 → 可用操作）。

---

## P1 高风险问题

### P1-1：文件列表页到 Source 详情页的跳转需要双向筛选状态保持

**问题描述**：文件列表页有 5 个筛选项（类型、状态、去重、Source Note 生成、任务），Source 详情页点返回时状态丢失。用户在大量文件里找到一批待处理项后，想逐个点进去操作，返回后又要重新筛选，体验断层。

**建议**：文件列表 → Source 详情 → 返回时，保持筛选条件和分页/滚动位置。或在 Source 详情页提供"返回列表"的同时显示当前筛选条件摘要。

### P1-2：审核队列页缺少批量操作能力

**问题描述**：AI 辅助整理后，一批文件会集中进入审核队列。如果每条都要点开、读草案、点批准/驳回，效率很低。大量同批次文件（如 20 份文档）逐个审核是不可接受的。

**建议**：
- 审核队列页支持勾选多条 → 批量批准/驳回（同类操作时）。
- 批量批准时显示总览：共 X 条，来源是哪些，批量写入 Obsidian。
- MVP 如果暂不支持批量操作，至少要提供"同批次来源"筛选，方便用户按导入任务批量处理。

### P1-3：扫描任务页的"重试失败任务"语义不清

**问题描述**：`POST /api/import-jobs/{id}/retry-failed` 只重试失败项，但用户可能同时需要"重新扫描整个目录"和"只补扫失败项"两种操作。当前 API 和 UI 都没有区分。

**建议**：扫描任务详情页展示失败文件列表，每条显示失败原因（权限拒绝、文件被占用、无法解析等），让用户决定是重试单条、跳过单条还是全部重试。

---

## P2 优化建议

### P2-1：配置页保存后需要成功/失败反馈和引导

当前配置页操作（测试路径、初始化 Vault、测试模型连接）没有明确 UI 反馈。测试连接时应该显示 loading → 成功/失败原因，不能静默。

### P2-2：Source 详情页需要展示文件在 Raw Sources 中的实际位置

用户可能需要直接打开归集文件所在目录（不是 Obsidian）。Source 详情页应该提供"打开归集文件所在目录"按钮，对应系统文件管理器。

### P2-3：Markdown 预览页需要支持 frontmatter 折叠和纯正文预览切换

当前 MVP 预览页只说"预览 Markdown"，但 Source Note frontmatter 字段较多（15+ 字段），用户真正需要看的是正文内容。建议默认展示折叠的 frontmatter 和正文，frontmatter 可展开查看。

### P2-4：审核队列页的 Markdown 草案预览需要区分"AI 建议内容"和"原始摘要"

当前 review_items 表的 `markdown_draft` 是整篇 Source Note 草案，但用户在审核时需要先看"AI 摘要和建议结论"，再决定是否深入看草案。审核页应该分层展示：顶部一行摘要 + 标签建议 → 中部草案正文 → 底部批准/驳回按钮。

### P2-5：文件列表页的"生成 Source Note"操作需要有状态反馈

用户点击"生成 Source Note"后，后端可能需要数秒处理（解析、调用 LLM）。当前文件列表页没有操作中/完成的状态提示，用户不知道请求是否还在跑。

### P2-6：`obsidian://open` URI 跳转前需要确认 Vault 路径配置已生效

用户点击"打开 Obsidian"时，如果 Vault 路径配置错误，会跳转到 Obsidian 但找不到文件，体验很差。建议在系统设置页验证 Vault 可访问后，才允许文件列表页显示"打开 Obsidian"按钮，或者点之前做一次路径存在性检查。

---

## MVP 范围建议

### 建议保留（页面）：

- **配置页**（系统设置）：Vault 路径、Raw Sources 路径、扫描根路径、MySQL 连接、模型供应商配置。MVP 必须有且功能完整。
- **扫描任务页**（导入任务页）：创建扫描任务、查看任务状态和进度、展示失败项。MVP 必须有，支持重试失败文件。
- **文件列表页**：展示所有 Source/File，分类型/状态/去重筛选，支持按任务筛选。MVP 必须有，批量操作可后续补。
- **审核队列页**：展示所有待审核 review_items，查看草案和 Agent 建议，执行批准/驳回。MVP 必须有，建议支持同批次批量处理。
- **Markdown 预览页**：渲染 Source Note Markdown，展示 frontmatter 和正文。MVP 必须有，frontmatter 折叠是优化建议但不影响开工。
- **打开 Obsidian**：通过 `obsidian://open` URI 跳转。MVP 必须有，需要配合 Vault 配置验证。

### 建议移出：

- **项目看板**（按项目查看资料）：V1/V2 能力，MVP 只需按文件类型、状态、来源筛选。
- **Agent 日志页**（独立页面）：MVP 1-3 只需在 Source 详情页展示当前 Source 的处理日志，不需要独立 Agent 日志页面。
- **办公室视图**：明确 V2，不在 MVP 讨论范围内。

### 建议新增（补闭环缺口）：

- **Source 详情页状态流转引导**：当前 MVP 页面清单有 Source 详情页，但没有明确说明页面内的状态文案引导（当前状态 → 下一步操作）。MVP 0 骨架阶段就要把 Source 状态机写清楚，避免后续返工。
- **批量审核能力入口**：MVP 审核队列页至少要有"按导入任务筛选"的手段，暗示后续会扩展为批量操作。

---

## 技术栈建议

### 后端：

- Java 21 + Spring Boot 3.x + MyBatis-Plus：不变，MVP 够用。
- 文件扫描用 `java.nio.file.Files.walk()`：不需要额外库。

### 前端：

- **Vue 3 + Vite + TypeScript**：不变。
- **Element Plus**：表格组件（`el-table`）支撑文件列表和审核队列，表单组件（`el-form`）支撑配置页，Dialog 支撑 Source 详情和 Markdown 预览。MVP 够用。
- **Pinia**：状态管理，MVP 阶段建议只管"当前筛选条件"和"当前选中 Source"，不要过度设计。
- **markdown-it 或 marked + DOMPurify**：Markdown 渲染够用。
- **不需要引入状态机库**：Source 状态流转通过后端状态驱动，前端只负责展示和触发操作。

### 数据库：

- MySQL 8.x：不变，Flyway 管理 DDL。

### 文件解析：

- MVP 用 **PDFBox**（PDF）、**Apache POI**（Word）、**commonmark-java**（Markdown）、**metadata-extractor**（图片元数据）：先小而稳，Tika 后续评估。
- 大文件（>100MB）只索引不解析正文：MVP 阶段不需要引入流式解析。

---

## 数据模型建议

### 需要保留：

- `sources`：MVP 核心，Source 是所有资料的锚点。
- `source_files`：MVP 1 文件归集必需。
- `import_jobs`：MVP 1 扫描任务必需。
- `obsidian_notes`：MVP 2 Obsidian 写入必需。
- `review_items`：MVP 3 审核必需。
- `agent_runs` / `agent_steps`：MVP 3 AI 辅助必需，但 MVP 0 骨架阶段可先不实现完整步骤记录，留空表和接口即可。
- `system_settings`：MVP 0 配置页必需。
- `model_providers`：MVP 0 配置页和 MVP 3 AI 调用必需。

### 需要收敛：

- `content_chunks`、`embedding_jobs`、`embedding_status`：**收敛到 MVP 3 之后**，MVP 0-2 完全不涉及向量化，不要在 MVP 0 骨架里建这些表和逻辑。
- `mcp_servers`、`mcp_tool_calls`：**收敛到 MVP 4**，MVP 0-3 完全不涉及 MCP，MVP 0 骨架不需要这些表。
- `personal_records`：**收敛到 V1**，MVP 0-4 不涉及个人记录写入。
- `agent_office_status`：明确是 V2，MVP 0-4 不需要。
- `wiki_integrations`：当前 MVP 没有 Wiki 页面间整合场景，MVP 0-3 不需要，先留空。

### 需要新增：

- `source_files.organize_status` 需要有 `need_review` 状态（文件类型无法识别、同名不同 hash 等需要人工确认的场景），当前状态枚举缺少这个。
- `import_jobs.failed_details_json`：JSON 字段存储失败文件列表和失败原因，避免每次查失败要 JOIN source_files。MVP 0 建表时预留。

---

## 最终建议

### 下一步是否可以开始 MVP 0 项目骨架：是，但有两个前提

**前提 1：确认 Source 状态机定义**
MVP 0 骨架需要把 Source 的状态流转逻辑写进代码里，不是留在文档里。建议在 MVP 0 阶段：
- 后端定义 `SourceStatus` 枚举和状态转换规则（哪些状态可以转哪些）。
- 前端 Source 详情页根据状态显示可用操作按钮。
- 状态机文档作为 `AGENTS.md` 的补充存入 `docs/archive/`。

**前提 2：审核队列到 Obsidian 写入的交接流程在 MVP 0 阶段就要设计接口**
不要把"审核通过后写入 Obsidian"当成 MVP 3 的细节。MVP 0 骨架就要定义：
- `POST /api/review-items/{id}/approve` 的返回结果包含什么？直接返回 `obsidian_uri` 还是只更新状态让前端自己调写入接口？
- 建议：`approve` 接口返回时同步写入 Obsidian，不要拆分两个操作，降低 MVP 3 的实现复杂度。

### MVP 0 骨架验收标准（前端视角）

MVP 0 前端骨架做完时应满足：

1. 前端可本地启动（`npm run dev`），能展示配置页表单。
2. 配置页保存路径后，后端 `/actuator/health` 返回 UP。
3. 前端 Pinia store 定义了 `useSourceStore`、`useSettingsStore`、`useReviewStore`，但数据来自 mock 或空 API，MVP 1 再替换为真实调用。
4. Markdown 预览组件可渲染任意 Markdown 字符串（用 markdown-it），不依赖后端。
5. `obsidian://open` URI 拼接逻辑已有单元测试，覆盖 Vault 名和相对路径转义。

---

**评审人**：Claude Code（MiniMax-M2.7）
**评审版本**：v1.0
**下次复审时机**：MVP 0 骨架完成后，MVP 1 开工前