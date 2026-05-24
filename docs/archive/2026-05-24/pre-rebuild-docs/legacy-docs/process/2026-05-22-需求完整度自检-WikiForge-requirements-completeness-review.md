# 2026-05-22 知识熔炉 WikiForge 初版需求完整度自检

## 自检结论

当前初版需求整体完整，可以进入用户复核和 MVP 实施计划拆分阶段。

需求已经明确区分：

- 长期愿景：LLM Wiki 表达层 + GBrain 运行层。
- MVP 目标：本地杂乱源文件归集整理 + 最小 Obsidian 归档闭环。
- V1/V2 拓展：在线文档、个人记录、MCP、向量库、办公室视图、维护 Agent。

## 已明确内容

### 产品定位

- 产品名：知识熔炉 WikiForge。
- 本地优先，可私有化部署。
- 不是普通 RAG，而是 LLM Wiki + GBrain 融合系统。
- Obsidian 是稳定知识表达层。
- MySQL 是运行账本和结构化索引库。

### MVP 范围

MVP 只做：

- 本地路径扫描。
- 源文件复制归集。
- hash 去重。
- 文件基础分类。
- MySQL Source/File/ImportJob 索引。
- Obsidian Vault 初始化。
- Source Note 模板和 frontmatter。
- Web UI 基础后台。
- Markdown 预览和 `obsidian://open`。
- 单 LLM 多步骤轻量 AI 辅助整理。

MVP 暂缓：

- 飞书 / 腾讯文档自动读取。
- OpenClaw / Hermes 自动写入。
- 完整 MCP Server。
- 个人记录完整处理。
- 向量库和 hybrid search。
- 办公室等距视图。
- Lint / Maintain Agent。
- 多模态音视频处理。

### 阶段规划

- MVP 0：项目骨架。
- MVP 1：源文件归集。
- MVP 2：Obsidian Source Note。
- MVP 3：AI 辅助整理。
- MVP 4：轻量 MCP 预览版。
- V1：在线资料与个人记录。
- V2：知识运行层。

### 技术路线

- 后端：Java 21 / 17 + Spring Boot 3.x。
- 前端：Vue 3 + Vite + TypeScript。
- 数据库：MySQL。
- 数据访问：MyBatis-Plus / MyBatis。
- 数据库迁移：Flyway。
- 文件解析：Apache POI、Apache PDFBox、commonmark-java / flexmark-java、metadata-extractor。
- 模型调用：Provider Adapter + OpenAI-compatible API。
- MCP：MVP 预留，MVP4 使用官方 Java SDK 评估落地。

## 当前仍需在实施计划中细化的点

这些不是需求阻塞项，但在进入开发前需要拆成实施细节：

1. **文件归集规则**
   - 文件命名冲突如何处理。
   - 同 hash 不同路径是否只记录重复关系。
   - 不同 hash 同名文件如何命名。
   - 大文件大小上限。
   - 扫描隐藏文件、系统文件、临时文件的规则。

2. **Source Note 模板**
   - MVP frontmatter 最小字段。
   - Source Note 正文结构。
   - index / log 的初始内容。
   - 是否按日期或类型分目录。

3. **Web UI 页面字段**
   - 扫描任务页字段。
   - 文件列表筛选条件。
   - Source 详情页字段。
   - 审核页交互。
   - 系统设置页配置项。

4. **API 设计**
   - Import Job 创建、启动、停止、查询。
   - Source 查询、详情、更新处理意图。
   - Obsidian Note 生成、预览、打开。
   - Review approve / reject / modify。
   - Model Provider 配置。

5. **错误处理和恢复**
   - 文件复制失败。
   - MySQL 写入成功但文件复制失败。
   - Obsidian 写入失败。
   - 模型调用失败。
   - 重试、跳过、失败任务恢复策略。

6. **安全边界**
   - 允许扫描的路径根。
   - 防路径穿越。
   - API Key 本地存储方式。
   - 无登录模式下是否只绑定 localhost。

7. **测试验收**
   - 典型文件夹扫描测试。
   - 重复文件测试。
   - Word / PDF / Markdown / JPG 解析测试。
   - Obsidian 文件生成测试。
   - UI 预览和打开 Obsidian 测试。

## 风险评估

### MVP 风险较低

原因：

- 已经收敛到本地文件整理和 Obsidian 归档。
- 暂缓了在线文档、向量库、完整 MCP、办公室视图、个人记录等复杂能力。
- 技术栈采用 Java / Spring Boot / Vue / MySQL 主流组合。

### 主要风险

- 文件系统操作需要谨慎，避免误删、误移动、误覆盖。
- PDF / Word 解析质量可能不稳定，需要允许 partial 状态。
- Obsidian Markdown 生成需要保持可读性，不能只追求结构化。
- AI 输出必须人工审核，不能默认覆盖 Wiki 页面。

## 建议下一步

建议下一步生成单独的 MVP 实施计划文档，内容包括：

- 模块拆分。
- 页面清单。
- API 清单。
- 数据库 DDL 初稿。
- Source Note 模板。
- 文件归集规则。
- 验收用例。
