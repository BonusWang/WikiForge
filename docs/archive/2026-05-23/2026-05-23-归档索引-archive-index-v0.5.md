# 2026-05-23 WikiForge 文档归档索引 v0.5

## 归档目的

本目录用于保存 WikiForge 在 2026-05-23 的需求基线、技术方案、开发者日志和参考资料快照。同一天产生的归档文件统一放在本日期目录下。

主文档会继续迭代，归档文档不再直接修改，用于追踪完整需求演进过程。

## 命名规则

归档文件采用：

```text
日期-中文名-EnglishName-版本号.md
```

示例：

```text
2026-05-23-需求文档-knowledge-base-prd-v0.2.md
```

`README.md` 作为项目入口文件保留英文通用命名；归档快照可继续使用 `YYYY-MM-DD-README-vX.Y.md`。

如果同一类文档存在多个版本，优先阅读版本号最大的文件。

## 本次归档文件

| 文件 | 版本 | 说明 |
| --- | --- | --- |
| `2026-05-23-需求文档-knowledge-base-prd-v0.2.md` | v0.2 | 当前 PRD 需求基线 |
| `2026-05-23-技术架构-technical-architecture-v0.2.md` | v0.2 | 当前技术架构基线 |
| `2026-05-23-技术架构-technical-architecture-v0.3.md` | v0.3 | 补充 CI/CD、Docker 打包和部署架构后的技术架构 |
| `2026-05-23-技术架构-technical-architecture-v0.4.md` | v0.4 | 补充少服务微服务架构、Core/Worker 服务边界和 AI 开发 Skill 分层后的技术架构 |
| `2026-05-23-数据模型-data-model-v0.2.md` | v0.2 | 当前数据模型基线 |
| `2026-05-23-数据模型-data-model-v0.3.md` | v0.3 | 架构评审后收敛 MVP DDL、状态枚举和索引设计的数据模型 |
| `2026-05-23-数据模型-data-model-v0.4.md` | v0.4 | 补充少服务微服务下的表归属和 Core/Worker 数据边界 |
| `2026-05-23-归档索引-archive-index-v0.2.md` | v0.2 | 补充外部 AI 架构评审目录和最终结论后的归档索引 |
| `2026-05-23-归档索引-archive-index-v0.3.md` | v0.3 | 补充 aruis/codex-cookbook 参考后的归档索引 |
| `2026-05-23-归档索引-archive-index-v0.4.md` | v0.4 | 补充 MVP 0 工程骨架启动和验证结果后的归档索引 |
| `2026-05-23-归档索引-archive-index-v0.5.md` | v0.5 | 补充少服务微服务选择和 WikiForge AI 开发 Skill 后的归档索引 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.1.md` | v0.1 | MVP 实施计划 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.2.md` | v0.2 | 架构评审后补充 MVP 0、CI/CD、Docker 和分阶段执行规则的实施计划 |
| `2026-05-23-MVP实施计划-WikiForge-mvp-implementation-plan-v0.3.md` | v0.3 | 补充 MVP 0 少服务微服务骨架、Core/Worker 拆分和新开发顺序 |
| `2026-05-23-MVP0项目骨架-WikiForge-mvp0-project-skeleton-v0.1.md` | v0.1 | MVP 0 工程骨架开发规格、执行清单和验证结果 |
| `2026-05-23-微服务架构与AI开发Skill设计-WikiForge-microservice-ai-skill-design-v0.1.md` | v0.1 | 微服务 B 方案、CDP 范式吸收点、WikiForge AI 开发 Skill 和多人协作约定 |
| `2026-05-23-需求完整度自检-WikiForge-requirements-completeness-review-v0.1.md` | v0.1 | 需求完整度自检 |
| `2026-05-23-参考项目清单-WikiForge-reference-projects-v0.1.md` | v0.1 | 参考项目清单 |
| `2026-05-23-参考项目清单-WikiForge-reference-projects-v0.2.md` | v0.2 | 补充 aruis/codex-cookbook 开发实施方法参考 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.1.md` | v0.1 | 开发者日志 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.2.md` | v0.2 | 补充外部 AI 架构评审归档和最终结论后的开发者日志 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.3.md` | v0.3 | 补充 aruis/codex-cookbook 开发实施参考后的开发者日志 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.4.md` | v0.4 | 补充 MVP 0 工程骨架启动、验证结果和环境记录后的开发者日志 |
| `2026-05-23-开发者日志-WikiForge-developer-log-v0.5.md` | v0.5 | 补充少服务微服务架构选择、CDP AI 范式学习和 WikiForge AI 开发 Skill |
| `2026-05-23-架构决策-DECISIONS-v0.1.md` | v0.1 | 架构决策记录 |
| `2026-05-23-架构决策-DECISIONS-v0.2.md` | v0.2 | 架构评审后的最终决策记录 |
| `2026-05-23-架构决策-DECISIONS-v0.3.md` | v0.3 | 补充少服务微服务选择和 AI 开发 Skill 决策 |
| `2026-05-23-AI开发规则-AGENTS-v0.1.md` | v0.1 | 开发 AI 前置规则 |
| `2026-05-23-AI开发规则-AGENTS-v0.2.md` | v0.2 | 补充“读取最高版本归档索引”的开发 AI 前置规则 |
| `2026-05-23-AI开发规则-AGENTS-v0.3.md` | v0.3 | 补充读取 WikiForge 项目内 AI 开发 Skill 的规则 |
| `2026-05-23-架构评审材料-WikiForge-architecture-review-v0.1.md` | v0.1 | MVP 编码前架构评审材料 |
| `2026-05-23-架构评审材料-WikiForge-architecture-review-v0.2.md` | v0.2 | 补充 CI/CD、Docker 打包和部署评审项后的架构评审材料 |
| `架构评审-architecture-review/` | - | 外部 AI 架构评审材料、评审索引和最终结论 |
| `架构评审-architecture-review/2026-05-23-架构评审索引-architecture-review-index-v0.1.md` | v0.1 | 外部评审材料目录索引 |
| `架构评审-architecture-review/2026-05-23-架构评审结论-WikiForge-architecture-review-conclusion-v1.0.md` | v1.0 | 架构评审最终结论 |
| `2026-05-23-README-v0.1.md` | v0.1 | 项目入口说明 |
| `2026-05-23-README-v0.2.md` | v0.2 | 更新当前状态为 MVP 0 工程骨架阶段的项目入口说明 |
| `2026-05-23-README-v0.3.md` | v0.3 | 更新当前状态为少服务微服务目标架构 |
| `AI开发Skill-WikiForge-development-skill-v0.1/` | v0.1 | WikiForge 项目内 AI 开发 Skill 及 references 快照 |

## 当前阶段结论

当前已从 MVP 方向冻结进入 MVP 0 工程骨架阶段：

- MVP 先做本地源文件归集整理。
- MVP 打通最小 Obsidian Source Note 归档闭环。
- 飞书/腾讯文档、完整 MCP、向量库、个人记录、办公室视图放到 V1/V2。
- 技术栈采用 Java + Spring Boot + Vue + MySQL。
- 架构评审需覆盖 CI/CD、Docker 镜像打包、Docker Compose 发布、volume 挂载和健康检查。
- 架构评审最终结论为：可以进入 MVP 0 项目骨架阶段。
- MVP 0 工程骨架已创建后端、前端、CI、Docker Compose、Flyway 与健康检查基线。
- 本地验证已通过后端测试、后端打包、前端构建和 Compose 配置校验；Docker 镜像实构建受本机 Docker Desktop Linux engine 未启动影响暂未完成。
- 用户已选择 B 方案：少服务微服务。
- MVP 0/1 目标服务调整为 `wikiforge-core-service`、`wikiforge-worker-service`、`wikiforge-ui`、`mysql`。
- 已建立 WikiForge 项目内 AI 开发 Skill，用于约束多人和多 AI 后续开发。

## 后续归档建议

后续每次完成关键迭代时，新增一个日期目录，例如：

```text
docs/archive/2026-06-01/
```

不要把归档文件直接堆在 `docs/archive/` 根目录下；`docs/archive/` 根目录只保留归档说明文件。

每个归档目录应至少包含：

- PRD
- 技术架构
- 数据模型
- 实施计划或迭代计划
- 开发者日志
- 归档索引
