# 2026-05-23 WikiForge 版本更新记录

## 0.04 - MVP2 Obsidian Source Note 闭环

发布日期：待发布

本版本目标是把 MVP1 已整理出的 Source File 沉淀为可读、可编辑、可打开的 Obsidian Source Note，完成“整理后归档”的第一条可用链路。

### 更新内容

- 新增 Core Service Obsidian API：
  - `POST /api/v1/obsidian/init`
  - `POST /api/v1/source-files/{fileUid}/obsidian-note/draft`
  - `POST /api/v1/source-files/{fileUid}/obsidian-note/write`
  - `GET /api/v1/obsidian/notes/{noteUid}/preview`
- 新增 `obsidian_notes` 表，用于记录 Source / Source File 与 Obsidian Markdown 文件之间的映射。
- 新增 Source Note Markdown 模板，包含 frontmatter、Source UID、Source File UID、原始路径、归档路径、hash 和后续处理占位。
- 写入 Vault 时采用服务端路径校验、Vault 内相对路径解析、临时文件写入和原子 rename。
- 生成 `obsidian://open` URI，并对 Vault 名和 Vault 内路径进行 URL encode。
- Web UI Dashboard 新增：
  - `初始化 Vault`
  - Source Files 表格 `Source Note` 操作
  - Source Note Markdown 编辑抽屉
  - 写入 Vault、读取预览、打开 Obsidian
- Docker Compose 支持通过 `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH` 将宿主机 Vault 挂载到容器内 `/data/wikiforge/obsidian-vault`。

### 验证结果

- 后端 Maven 多模块测试：通过。
- 前端 `npm run build`：通过，有 Vite / Rollup 非阻塞 warning。
- `docker compose -f deploy/docker-compose.yml config`：通过。
- Docker 镜像构建与 Compose 启动：通过。
- 容器健康检查：`mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui` 全部 healthy。
- `POST /api/v1/obsidian/init`：通过。
- Docker 端到端 Source Note 烟测：通过。
- 浏览器检查 `http://localhost:3000`：MVP2、初始化 Vault、Source Note 可见，console error 为空。
- 发布前自检清单：通过，未发现编译产物、本地 `.env`、运行数据或 Vault 内容被 Git 跟踪；本地已存在的 `node_modules`、`dist`、`target`、`data` 由 `.gitignore` 忽略。
- GitHub Actions CI：待推送后以远程 CI 结果为准。

端到端烟测结果：

```text
JobUid: job_20260523_0ae896c3e383
JobStatus: completed
SourceFileUid: file_e6a481e4083449579246366252a410a5
NoteUid: note_20260523_1cf541982149
Host note path: E:\WikiForgeVault\00_Inbox_收集箱\Sources_来源\roadmap-source-note.md-src_f916b6b2d202461a8d49bfa721532290.md
Preview contains title: true
```

### 版本边界

本版本完成的是 MVP2 的“Source Note 归档”闭环。

尚未实现：

- 文档正文解析。
- AI 摘要、分类、标签生成。
- 已写 Note 的列表化管理和重复写入策略 UI。
- MCP 服务。
- 向量库导入。
- 飞书 / 腾讯文档 / 邮件 / 账单 / 人际关系等连接器。

下一阶段建议进入 MVP2.1：补齐已写 Note 状态、重复写入策略、Vault 状态面板和一条命令式端到端验收脚本。

## 0.03 - MVP1 本地源文件归集整理闭环

发布日期：2026-05-23

本版本是 WikiForge 从工程骨架进入 MVP1 业务闭环的第一个版本，核心目标是把“指定本地路径 -> 扫描文件 -> 归集到 Raw Sources -> 写入 MySQL 索引 -> Web UI 查看状态”跑通。

### 更新内容

- 完成 Core / Worker / UI 的本地源文件导入链路：
  - UI 创建本地导入任务。
  - Core 校验路径、创建 `import_jobs` 并派发 Worker。
  - Worker 扫描本地目录、按类型复制到 Raw Sources。
  - Worker 回调 Core 更新任务状态并提交 `source_files` 明细。
- 新增 MVP1 数据表：
  - `import_jobs`
  - `sources`
  - `source_files`
- 支持本地路径安全规则：
  - 限制扫描根目录。
  - 校验 Raw Sources 根目录必须与配置一致。
  - 禁止输入目录与 Raw Sources 目录重叠。
  - MVP1 默认不跟随 symlink / junction。
- 支持基础文件归类：
  - `01_Documents_文档`
  - `02_Images_图片`
  - `03_PDFs_PDF`
  - `90_Unknown_待确认`
- 支持按内容 hash 识别单次导入中的重复文件，重复文件不重复复制，记录为 `duplicate`。
- 修复本地与 Docker 烟测中发现的问题：
  - MySQL 8 `recursive` 保留字导致 Flyway migration 失败，数据库字段改为 `recursive_scan`。
  - MyBatis Plus 自动别名触发 MySQL 保留字问题，持久化实体改为 `recursiveScan`。
  - Worker 默认 HTTP request factory 不支持 `PATCH` 回调，改为 `JdkClientHttpRequestFactory`。
  - Core 服务重启后 `jobUid` 自增序号碰撞，改为 `job_yyyyMMdd_<12位uuid>`。
  - UI 容器 healthcheck 使用 `127.0.0.1`，避免容器内 `localhost` IPv6 解析导致误判 unhealthy。
- 补充 MVP1 契约文档、开发者日志和归档索引。

### 验证结果

- 后端 Maven 多模块测试：通过。
- 后端 Maven 打包：通过。
- 前端 `npm run build`：通过，有 Vite / Rollup 非阻塞 warning。
- `docker compose -f deploy/docker-compose.yml config`：通过。
- `docker compose -f deploy/docker-compose.dev.yml config`：通过。
- Docker 镜像构建：通过。
- Docker Compose 启动：通过。
- 容器健康检查：`mysql`、`wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui` 全部 healthy。
- 容器级端到端导入：通过。

容器级端到端验收结果：

```text
Entry: http://localhost:3000/api/v1/import-jobs/local
InputPath: /data/wikiforge/imports/test-input
RawSourcesRoot: /data/wikiforge/raw-sources
Status: completed
TotalCount: 5
SuccessCount: 4
SkippedCount: 0
FailedCount: 0
SourceFileTotal: 5
```

### 版本边界

本版本完成的是 MVP1 的“源文件收集整理”闭环，不代表完整知识库已经完成。

尚未实现：

- 文档正文解析和内容抽取。
- Source Note Markdown 草案生成。
- Obsidian Vault 自动写入。
- 在线文档连接器。
- MCP 服务。
- 向量库导入。
- 多 Agent 知识提炼流水线。

下一阶段建议进入 MVP1.1：在已归集的 `source_files` 基础上，选择少量 Markdown / Word / PDF 样例，生成可人工审核的 Obsidian Source Note 草案。

## 0.02 - MVP0 工程骨架与 Agent 协作基线

发布日期：2026-05-23

本版本是 WikiForge 从需求和架构文档阶段进入可开发工程基线阶段的第一个小版本。

### 更新内容

- 完成 MVP0 少服务微服务工程骨架：
  - `wikiforge-common`
  - `wikiforge-core-service`
  - `wikiforge-worker-service`
  - `wikiforge-ui`
- 建立 Java Maven monorepo 后端结构，保留 Core / Worker 服务边界。
- 新增 Core Service 健康检查、Flyway MVP0 初始化 migration、MyBatis-Plus 基础配置。
- 新增 Worker Service 健康检查骨架，为后续文件扫描和整理任务预留服务入口。
- 新增 Vue 3 + Vite + TypeScript 前端骨架和独立 UI 看板入口。
- 新增 Docker Compose 发布结构：
  - MySQL
  - Core Service
  - Worker Service
  - UI
- 新增 GitHub Actions CI：
  - 后端多模块测试与打包
  - 前端构建
  - Docker 镜像构建校验
- 补充 WikiForge 项目内 AI 开发 Skill，约束后续架构、代码、CI/CD、Docker、Agent、MCP 和多人协作开发。
- 补充并行开发规则：
  - 主编排 Agent 负责任务拆解、专家选择、文件边界和最终集成。
  - 高冲突文件串行修改。
  - 子 Agent 输出 Handoff Packet。
- 补充 Git 提交规则，明确 `node_modules/`、`dist/`、`.vite/`、`target/`、`.env`、运行数据和本地知识库数据不提交。

### 验证结果

- 后端 Maven 多模块测试：通过。
- 后端 Maven 打包：通过。
- 前端 `npm run build`：通过，有 Vite / Rollup 非阻塞 warning。
- `docker compose -f deploy/docker-compose.yml config`：通过。
- `docker compose -f deploy/docker-compose.dev.yml config`：通过。
- Docker 镜像实构建：受本机 Docker Desktop Linux engine 未启动影响，暂未完成。

### 版本边界

本版本仍属于 MVP0 工程基线，不包含 MVP1 业务闭环。

尚未实现：

- 本地路径扫描。
- Raw Sources 归集复制。
- 文件解析。
- Source Note 草案生成。
- Obsidian Vault 写入。
- MCP 服务。
- 向量库导入。

下一阶段应先冻结 MVP1 API、DTO、DDL、状态枚举和路径安全策略，再进入文件收集整理闭环开发。
