# WikiForge 架构决策记录

## 2026-05-22 MVP 范围

MVP 先做本地源文件整理和最小 Obsidian 归档闭环。

暂缓：

- 飞书 / 腾讯文档自动读取
- 完整 MCP Server
- 向量库
- 个人记录完整处理
- 办公室等距视图
- 复杂多 Agent 编排

## 2026-05-22 技术栈

- 后端主栈：Java 21/17 + Spring Boot 3.x
- 前端主栈：Vue 3 + Vite + TypeScript
- 数据库：MySQL 8.x
- 数据访问：MyBatis-Plus / MyBatis
- 数据库迁移：Flyway
- UI 组件：Element Plus

选择原因：

- 国内开发主流。
- 文档和生态成熟。
- 便于后续维护。
- 避免 MVP 引入冷门框架和过重基础设施。

## 2026-05-22 知识存储边界

Obsidian 是长期知识正文和人工编辑层。

MySQL 是控制平面、结构化索引库、Agent 流程账本和轻量内容缓存，不作为最终知识正文主库。

Raw Sources 是原始源文件归集目录，MVP 默认复制文件，不移动、不删除原始文件。

## 2026-05-22 产品路线

WikiForge 采用 LLM Wiki + GBrain 融合路线：

- LLM Wiki 表达层：Raw Sources、Obsidian Vault、Source Note、Schema、index、log。
- GBrain 运行层：MySQL、Agent Orchestrator、MCP、向量库、个人记录、定时任务。

MVP 优先表达层最小闭环，V1/V2 再增强运行层。
