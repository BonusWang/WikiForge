# 2026-05-24 WikiForge MVP0 需求文档 MVP0 PRD

## 1. MVP0 结论

WikiForge 从 MVP0 重新开始，定位为个人私有知识库工具，优先解决资料入口分散、文件杂乱、归档不稳定的问题。

第一版学习 ChatPDF 的统一入口体验：用户可以通过本机路径扫描和浏览器上传把资料送入系统。但第一版不优先做“上传后聊天问答”，也不把真实向量检索作为主线依赖。

WikiForge 的核心知识形态采用 LLM Wiki 模式：

- Raw Sources 是不可变事实源，只复制、登记、追溯，不由 AI 修改。
- Obsidian Wiki 是 LLM 自动维护的知识表达层，系统按规则生成、更新、索引和记录日志。
- Schema / index / log 是约束层，分别定义写作规则、页面目录和演进记录。

## 2. 业务诉求

### 2.1 当前痛点

- 文件散落在本机多个文件夹、下载目录、项目目录和临时目录中，长期缺少统一归集。
- 同一资料可能重复保存，用户难以判断哪个版本已整理、哪个版本仍待处理。
- 已整理内容没有稳定进入 Obsidian，导致资料虽然存在，但难以复用。
- 现有项目已提前堆叠 MCP、向量导出、LifeOS、知识体检、辅助开发工程等高级能力，主流程被稀释。
- 前端单页和后端大服务类已经影响后续按原子能力迭代。

### 2.2 MVP0 目标

MVP0 只追求一个可验证闭环：

```text
路径扫描 / 浏览器上传
  -> Raw Sources 复制收纳
  -> hash 去重、类型识别、基础分类
  -> 文本抽取和 SourceFile 账本
  -> LLM Wiki 规则整理
  -> 写入 Obsidian Wiki 页面
  -> 更新 index.md / log.md
  -> UI 展示任务状态和结果
```

成功标准：

- 用户可以从 Web UI 提交本机路径或上传文件。
- 系统默认复制文件到 Raw Sources，不移动、不删除原文件。
- 系统能识别重复文件并避免重复复制。
- 文档类资料能抽取正文；图片、二进制、大文件先保存元数据和来源。
- 系统能按规则自动写入 Obsidian Wiki，并记录来源、hash、时间和写入结果。
- UI 主流程只围绕“收纳、资料箱、Wiki、日志/设置”展开。

## 3. 功能范围

### 3.1 必须包含

- 统一收纳入口：
  - 本机路径扫描。
  - 浏览器上传文件。
  - 任务创建、执行状态、失败原因。
- Raw Sources 管理：
  - 复制收纳。
  - hash 去重。
  - 文件类型识别。
  - 原始路径和归集路径登记。
- 资料处理：
  - Markdown / TXT / PDF / DOCX 正文抽取。
  - ImportJob / SourceFile / SourceContent 账本。
  - 文件状态：待处理、已收纳、已解析、已写入 Wiki、失败。
- Obsidian LLM Wiki：
  - Vault 初始化。
  - Source page / topic page / project page 写入规则。
  - `index.md` 内容目录。
  - `log.md` 追加式演进记录。
  - 写入失败不影响 Raw Sources 已收纳结果。
- UI 工作台：
  - 收纳入口。
  - 待整理资料箱。
  - Wiki 写入结果。
  - 运行日志。
  - 基础设置。

### 3.2 明确延后或退役

- 文档问答、引用页码回答、多文件聊天。
- 真实向量库、Hybrid Search、rerank。
- 完整 MCP Server / Client。
- 飞书、腾讯文档、Notion 等真实 OAuth 连接器。
- LifeOS 个人记录周期总结。
- 知识体检自动修复。
- Agent 办公室视图。
- 辅助开发工程服务。

### 3.3 退役

以下现有能力退出主流程：

- MCP Preview：退役，MVP0 代码不保留运行入口。
- Vector Export：退役，真实向量库方案确认前不继续投入。
- Personal Record / LifeOS：退役，不进入 MVP0。
- Knowledge Maintenance：退役，不进入 MVP0。
- AI Review / Review Items：退役，不承载 Wiki ingest。
- 旧 Wiki Compile / Source Note / Link Source：退役，MVP0 改用 Wiki ingest 主流程。
- Orchestration Service / Orchestration UI：退役，不作为 WikiForge 产品能力继续规划。

## 4. 四层业务能力

| 层级 | WikiForge 口径 | 第一版能力 |
| --- | --- | --- |
| 轻应用层 | 用户可见入口和工作台 | 收纳入口、资料箱、Wiki 页面、日志、设置 |
| 决策层 | 规则、策略和 AI 整理决策 | 分类规则、重复策略、敏感/大文件策略、Wiki 写入规则、失败处理策略 |
| 指令执行层 | 将决策转成任务流 | 路径导入任务、上传任务、解析任务、Wiki ingest 任务、index/log 更新任务 |
| 原子能力层 | 可复用最小能力 | 扫描、复制、hash、类型识别、正文抽取、Markdown 写入、路径安全、模型调用 |

## 5. 用户流程

### 5.1 路径扫描

1. 用户在收纳入口填写本机目录。
2. 系统校验路径存在、类型为目录、未与 Raw Sources 重叠。
3. Worker 扫描文件，跳过隐藏文件、临时文件和不安全路径。
4. 系统复制文件到 Raw Sources 分类目录。
5. Core 记录 ImportJob、SourceFile、SourceContent。
6. 系统按规则触发 Wiki ingest。
7. UI 显示收纳结果、重复文件和 Wiki 写入状态。

### 5.2 浏览器上传

1. 用户拖放或选择文件。
2. Core 创建上传任务并保存临时文件。
3. 系统计算 hash，按 Raw Sources 分类目录写入最终文件。
4. 后续处理与路径扫描共享同一条 SourceFile / Wiki ingest 流程。

### 5.3 LLM Wiki 自动归档

1. 系统读取 SourceContent 和 SourceFile 元数据。
2. 系统根据 schema 生成或更新 Source page。
3. 系统选择或创建 Topic / Project 页面。
4. 系统写入来源、摘要、关键点、交叉链接和状态。
5. 系统更新 `index.md`。
6. 系统追加 `log.md`。
7. 系统记录写入结果，供 UI 查看和后续重跑。

## 6. 数据与安全边界

- MVP0 数据库做减法，目标是最小表集合，而不是继续沿用历史阶段全部表结构。
- MVP0 只规划收纳任务、资料文件、正文内容、Wiki 写入运行结果这几类数据。
- 状态码统一进入字典表，前端展示中文码值和中文说明，不直接展示英文状态。
- MCP、向量、LifeOS、知识体检、辅助开发工程相关表不进入 MVP0 数据模型。
- 新表按需要新增，不提前为未来能力预建表。
- 现有历史表在后续数据库清理节点中评估迁移、退役或删除；MVP0 fresh schema 不再创建已退役高级能力表。
- Raw Sources、Obsidian Vault、本地 `.env`、运行日志和数据库数据不得提交到 Git。
- 文件收纳默认复制，不移动、不删除用户原文件。
- 上传文件不得覆盖已有文件；同名文件使用 hash 和安全后缀处理。
- Obsidian 写入只能发生在配置的 Vault 内 `WikiForge/` 托管目录。
- UI 和外部 API 不返回宿主机敏感绝对路径，必要时只返回 Vault 相对路径或内部 UID。
- 当前 MVP0 先使用规则式 Markdown 生成 Wiki 页面；模型整理能力后续单独设计。

## 7. MVP0-0 文档交付边界

本轮只交付 MVP0 基座：

- 新版整体需求。
- 四层架构设计。
- Drawio 图纸。
- 现有资源盘点。
- MVP0 路线。

后续代码实施已按本基座推进：Orchestration 辅助开发工程、独立 UI、Dockerfile 和 `agentteam/` 工作区已退役删除。
