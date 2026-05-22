# 2026-05-23 WikiForge 架构评审材料 v0.1

## 1. 评审目标

本次评审目标是确认 WikiForge MVP 是否可以进入编码阶段。

请评审方重点判断：

- MVP 边界是否足够收敛，是否还能更小。
- 当前技术栈是否适合个人本地优先知识库系统。
- MySQL、Obsidian Vault、Raw Sources 三者职责是否清晰。
- 文件扫描、复制归集、hash 去重、Markdown 写入方案是否有明显风险。
- 当前数据模型是否能支撑 MVP，同时不被长期能力拖重。
- 后续 MCP、向量库、在线文档、个人记录是否已有合理预留。

## 2. 当前项目阶段

当前处于：

```text
MVP 设计冻结后 -> 工程编码前 -> 架构评审阶段
```

最新冻结结论：

- MVP 先做本地源文件归集整理。
- MVP 打通最小 Obsidian Source Note 归档闭环。
- 飞书 / 腾讯文档、完整 MCP、向量库、个人记录、办公室视图放到 V1/V2。
- 技术栈采用 Java + Spring Boot + Vue + MySQL。

## 3. 评审前必读文档

请先阅读最新日期快照：

- `docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-v0.1.md`
- `docs/archive/2026-05-23/2026-05-23-需求文档-knowledge-base-prd-v0.2.md`
- `docs/archive/2026-05-23/2026-05-23-技术架构-technical-architecture-v0.2.md`
- `docs/archive/2026-05-23/2026-05-23-数据模型-data-model-v0.2.md`
- `docs/archive/2026-05-23/2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.1.md`
- `docs/archive/2026-05-23/2026-05-23-架构决策-DECISIONS-v0.1.md`

再阅读当前主文档：

- `docs/需求文档-knowledge-base-prd.md`
- `docs/技术架构-technical-architecture.md`
- `docs/数据模型-data-model.md`
- `docs/2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md`
- `docs/架构决策-DECISIONS.md`

## 4. MVP 核心闭环

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

## 5. MVP 必须实现

- 本地路径扫描。
- 源文件复制归集。
- 文件 hash 去重。
- 文件类型识别。
- MySQL 索引。
- Obsidian Vault 目录初始化。
- Source Note Markdown 生成。
- Markdown 预览。
- `obsidian://open` 打开链接。
- 基础 Web UI。
- 单 LLM 多步骤 AI 辅助整理。
- 人工审核后归档。

## 6. MVP 暂不实现

- 飞书 / 腾讯文档自动读取。
- OpenClaw / Hermes 自动写入。
- 完整 MCP Server。
- 个人记录完整处理。
- 向量库。
- hybrid search。
- 办公室等距视图。
- Lint / Maintain Agent。
- 音视频多模态解析。
- 登录和权限系统。

## 7. 当前技术方案摘要

后端：

- Java 21 LTS，兼容 Java 17。
- Spring Boot 3.x。
- Spring MVC REST API。
- MyBatis-Plus / MyBatis。
- MySQL 8.x。
- Flyway。
- Spring Validation。
- springdoc-openapi。
- Actuator。

前端：

- Vue 3。
- Vite。
- TypeScript。
- Element Plus。
- Pinia。
- Vue Router。
- Axios。

文件与知识层：

- Raw Sources：原始源文件归集目录，默认复制，不移动、不删除原文件。
- Obsidian Vault：长期知识正文、Source Note、人工编辑层。
- MySQL：控制平面、结构化索引、导入任务、Agent 流程账本、轻量内容缓存。

MVP 暂不引入：

- Redis。
- Elasticsearch / OpenSearch。
- Kafka。
- 工作流引擎。
- 向量数据库。
- OCR 重型能力。
- 登录权限系统。

## 8. 关键架构决策

### 8.1 MySQL 不作为最终知识正文主库

MySQL 保存索引、状态、任务、映射、日志和轻量内容缓存。

最终可读、可编辑、可长期维护的知识正文沉淀在 Obsidian Markdown 中。

### 8.2 Raw Sources 默认复制，不移动源文件

MVP 目标是先整理和归集，不破坏用户原始资料位置。

默认策略：

- 复制到 `WikiForge_RawSources`。
- 不删除原文件。
- 不移动原文件。
- 同 hash 文件只复制一份。

### 8.3 先 Console UI，后办公室视图

MVP 先做普通后台 UI，保证扫描、归集、审核、预览、打开 Obsidian 的闭环。

办公室视图作为 V2 体验增强。

### 8.4 先单 LLM 多步骤，后多 Agent 编排

MVP 可以用一个模型调用适配器完成摘要、分类、标签、Source Note 草案生成。

复杂多 Agent 编排放到后续阶段。

## 9. 需要重点评审的问题

### 9.1 MVP 范围

- 当前 MVP 是否仍然过大？
- “单 LLM 多步骤 AI 辅助整理”是否应该进入 MVP，还是放到 MVP 3？
- 是否应该先做纯文件归集 + Obsidian Source Note，再做 AI？

### 9.2 技术栈

- Java 21 / Spring Boot 3.x / Vue 3 / MySQL 8.x 是否合理？
- MyBatis-Plus 是否适合当前复杂度，还是直接 MyBatis 更稳？
- 是否需要在 MVP 就引入 Apache Tika，还是先用 PDFBox / POI / Markdown 解析器？

### 9.3 数据模型

- `sources`、`source_files`、`import_jobs`、`obsidian_notes` 是否足够支撑 MVP？
- 当前数据模型是否预留过多，是否会影响第一版开发效率？
- `raw_text` 是否应该存 MySQL，还是只存摘要和文件路径？

### 9.4 文件系统安全

- 指定路径扫描是否需要白名单根目录？
- 是否需要限制扫描深度、文件大小、文件类型？
- 对重复文件、同名不同 hash 文件、隐藏文件、临时文件的处理是否合理？
- `obsidian://open` 打开本地文件是否有路径注入风险？

### 9.5 Obsidian 写入

- Source Note 命名规则是否足够稳定？
- Frontmatter 字段是否需要再收敛？
- 先写入 Review 目录，人工审核后再进入正式目录，这个流程是否合理？

### 9.6 后续扩展

- 当前架构是否方便后续接 MCP？
- 当前架构是否方便后续导出向量库？
- 当前架构是否方便后续接飞书 / 腾讯文档？
- 当前架构是否方便后续接 OpenClaw / Hermes 写入个人记录？

## 10. 建议评审输出格式

请评审方按以下格式输出：

```markdown
# WikiForge 架构评审意见

## 结论

- 是否建议进入 MVP 编码：是 / 否 / 有条件

## 阻塞问题

- P0：必须先解决的问题

## 高风险问题

- P1：可以开工，但需要尽快调整的问题

## 优化建议

- P2：不阻塞 MVP，但建议记录的问题

## MVP 范围建议

- 建议保留：
- 建议移出：
- 建议新增：

## 技术栈建议

- 后端：
- 前端：
- 数据库：
- 文件解析：

## 数据模型建议

- 需要保留：
- 需要收敛：
- 需要新增：

## 最终建议

- 下一步是否可以开始 MVP 0 项目骨架：
```

## 11. 当前倾向结论

从当前文档看，建议架构评审重点不是推翻技术路线，而是做 MVP 收敛。

建议默认启动顺序：

1. MVP 0：项目骨架。
2. MVP 1：源文件归集。
3. MVP 2：Obsidian Source Note。
4. MVP 3：AI 辅助整理。
5. MVP 4：轻量 MCP 预览。

如评审方认为 MVP 仍然偏大，优先砍掉 AI 辅助整理，把第一版压缩为：

```text
路径扫描 -> 文件复制归集 -> MySQL 索引 -> Source Note 模板 -> 人工审核 -> Obsidian 写入
```
