# 2026-05-23 WikiForge 版本更新记录 v0.1

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
