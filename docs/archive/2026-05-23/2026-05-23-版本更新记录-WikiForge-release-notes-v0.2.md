# 2026-05-23 WikiForge 版本更新记录 v0.2

## 0.03 - MVP1 本地源文件归集整理闭环

发布日期：2026-05-23

本版本是 WikiForge 从工程骨架进入 MVP1 业务闭环的第一个版本，核心目标是把“指定本地路径 -> 扫描文件 -> 归集到 Raw Sources -> 写入 MySQL 索引 -> Web UI 查看状态”跑通。

### 更新内容

- 完成 Core / Worker / UI 的本地源文件导入链路。
- 新增 `import_jobs`、`sources`、`source_files` 三张 MVP1 数据表。
- 支持本地路径安全校验、Raw Sources 根目录一致性校验和目录重叠防护。
- 支持按扩展名复制归类到 Documents、Images、PDFs、Unknown 四类目录。
- 支持单次导入内基于内容 hash 的重复文件识别。
- 修复 MySQL 8 `recursive` 保留字、MyBatis 别名、Worker `PATCH`、Core 重启后 `jobUid` 碰撞和 UI healthcheck 问题。

### 验证结果

- 后端 Maven 多模块测试：通过。
- 后端 Maven 打包：通过。
- 前端 `npm run build`：通过。
- Compose 配置校验：通过。
- Docker 镜像构建：通过。
- Docker Compose 启动：通过。
- 容器健康检查：全部 healthy。
- 容器级端到端导入：通过。

```text
Entry: http://localhost:3000/api/v1/import-jobs/local
Status: completed
TotalCount: 5
SuccessCount: 4
SkippedCount: 0
FailedCount: 0
SourceFileTotal: 5
```

### 版本边界

本版本完成 MVP1 源文件收集整理闭环，尚未包含文档正文解析、Source Note 生成、Obsidian 写入、MCP、向量库和多 Agent 知识提炼。

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
