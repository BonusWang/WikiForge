# 2026-05-23 WikiForge 架构评审结论 v1.0

## 1. 结论

WikiForge 架构评审最终结论：

```text
可以进入 MVP 0 项目骨架阶段。
```

但进入的是 **MVP 0：项目骨架 + CI/CD + Docker + 基础配置 + Flyway 基线**，不是直接进入文件扫描业务开发。

当前无剩余 P0 阻塞问题。外部 AI 评审中标记为 P0 的问题，经过复核后分为两类：

- 真正影响 MVP 0/1 的问题，已转化为架构决策和实施约束。
- 只影响 MVP 3/4 的 AI/MCP 问题，降级为后续阶段开工前约束，不阻塞 MVP 0。

## 2. 已规范的评审材料目录

外部 AI 评审结果已统一归档到：

```text
docs/archive/2026-05-23/架构评审-architecture-review/
```

目录规则：

- 主题目录采用中文名 + EnglishName。
- 文件采用 `YYYY-MM-DD-中文名-EnglishName-版本号.md`。
- 同一主题下多份评审材料放在主题目录内。

本次评审材料：

| 文件 | 角色 |
| --- | --- |
| `2026-05-23-前端产品评审-Claude-Minimax27-frontend-product-review-v1.0.md` | 前端产品体验 |
| `2026-05-23-本地文件系统安全评审-CodeBuddy-Auto-local-filesystem-security-review-v1.0.md` | 本地文件系统安全 |
| `2026-05-23-后端架构评审-Qoder-backend-architecture-review-v1.0.md` | 后端架构 |
| `2026-05-23-Java后端架构评审-Trae-GLM51-java-backend-architecture-review-v1.0.md` | Java 后端架构 |
| `2026-05-23-数据模型DBA评审-Trae-data-model-dba-review-v0.1.md` | DBA / 数据模型 |
| `2026-05-23-数据专家评审-Trae-GLM51-data-expert-review-v1.0.md` | 数据专家 |
| `2026-05-23-AI与MCP扩展架构评审-Trae-DeepSeekV4Pro-ai-mcp-architecture-review-v1.0.md` | AI / MCP 扩展 |

## 3. 评审意见综合判断

### 3.1 采纳

以下意见采纳，并写入 `DECISIONS.md`、数据模型或实施计划：

- 采用 MyBatis-Plus 3.5.x，复杂 SQL 可补充 MyBatis XML。
- Flyway 按 MVP 阶段分段建表，不一次性创建全部长期规划表。
- MVP 0 同步建设 CI/CD、Docker Compose、`.env.example` 和健康检查。
- Docker 采用前后端分离镜像，不把前端打进 Spring Boot jar。
- 路径安全必须使用规范化路径和白名单校验。
- 默认不跟随符号链接。
- MVP 不暴露 `move` 模式。
- Hash 统一使用 SHA-256，并流式计算。
- 文件复制和 Obsidian 写入采用临时文件 + 原子 rename。
- `obsidian://open` 必须使用 Vault 内相对路径并 URL encode。
- Docker 模式下区分容器内路径和宿主机路径。
- `sources` 表不承载大文本正文，后续如需保存解析文本，拆到 `source_contents`。
- `sources.status` 统一为固定状态枚举。
- MVP 阶段一个 Source 对应一个 Source File。

### 3.2 部分采纳

以下意见方向正确，但不作为 MVP 0 阻塞：

- AI / MCP 评审提出的 `ModelProvider`、`AgentStepName`、MCP Tool Schema，作为 MVP 3/4 开工前约束。
- Review Queue 独立表是否进入 MVP 2，暂不提前强制；MVP 2 可先通过 Source 状态完成最小审核，MVP 3 再引入完整 `review_items`。
- `content_chunks`、`embedding_jobs` 等向量库表只保留长期设计，不进入 MVP 0/1。
- `personal_records` 不进入 MVP，放到 V1。

### 3.3 不采纳

以下意见暂不采纳：

- 不在 MVP 0 引入完整 MCP SDK。
- 不在 MVP 0/1/2 实现 AI 辅助整理。
- 不在 MVP 0 一次性创建全部长期规划表。
- 不为 MVP 1/2 引入 Apache Tika 作为统一解析层。
- 不把项目拆成复杂 Maven 多模块，MVP 先使用单后端模块，包结构清晰即可。

## 4. 最终 MVP 阶段

### MVP 0：项目骨架

目标：

- 后端可启动。
- 前端可启动。
- MySQL 可连接。
- Flyway 可迁移空库。
- CI 可自动测试和构建。
- Docker Compose 可启动 backend、frontend、mysql。

范围：

- `backend/`
- `frontend/`
- `deploy/`
- `.github/workflows/`
- `.env.example`
- MyBatis-Plus 基础配置
- Flyway MVP 0 migration
- Actuator health
- system settings / model providers 最小表

### MVP 1：源文件归集

目标：

- 扫描指定本地路径。
- 复制文件到 Raw Sources。
- SHA-256 hash 去重。
- 写入 `sources`、`source_files`、`import_jobs`。
- Web UI 查看扫描任务和文件列表。

MVP 1 开工前必须实现或明确：

- `FileSystemSecurityService`。
- 白名单根路径校验。
- 符号链接策略。
- 最大扫描深度。
- 大文件阈值和绝对上限。
- 原子复制策略。

### MVP 2：Obsidian Source Note

目标：

- 初始化 Vault 目录。
- 生成 Source Note Markdown。
- UI 预览 Markdown。
- 安全生成 `obsidian://open`。
- 写入 `obsidian_notes`。

### MVP 3：AI 辅助整理

目标：

- 定义并实现 `ModelProvider`。
- 单 LLM 多步骤生成摘要、标签、Source Note 草案。
- 引入 `source_contents`、`agent_runs`、`agent_steps`、`review_items`。

### MVP 4：轻量 MCP 预览

目标：

- 引入官方 MCP Java SDK。
- 实现最小 MCP tools。
- 记录 MCP 调用日志。

## 5. 下一阶段执行建议

下一阶段可以开始：

```text
MVP 0 项目骨架
```

建议第一批开发任务：

1. 创建 `backend/` Spring Boot 项目。
2. 创建 `frontend/` Vue 3 + Vite 项目。
3. 创建 `deploy/` Docker Compose 和 Dockerfile 草案。
4. 创建 `.github/workflows/ci.yml`。
5. 创建 `.env.example`。
6. 配置 MySQL、Flyway、MyBatis-Plus、Actuator。
7. 创建 MVP 0 migration：`system_settings`、`model_providers`。
8. 跑通本地启动、CI 构建和 Docker Compose 基础启动。

## 6. 最终判断

WikiForge 当前架构已经具备进入编码阶段的条件。

下一步不要先写文件扫描业务，也不要先写 AI。先把工程底座打牢：

```text
工程骨架 -> 自动构建 -> Docker 可运行 -> 数据库 migration 可验证 -> 再进入源文件归集
```
