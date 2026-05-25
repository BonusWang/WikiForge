# WikiForge MVP0 新起点交付记录

## 1. 交付定位

本次交付作为 WikiForge 新起点进入 `main`。

项目方向从历史阶段重新收束为 MVP0：个人私有知识库工具，优先解决文件统一收纳、Raw Sources 规整、正文抽取、Obsidian LLM Wiki 写入。历史的 R7、MCP、向量检索、LifeOS、知识体检、Orchestration、多 Agent 并行和辅助开发工程不再作为当前主线。

## 2. 当前主流程

```text
本地路径扫描 / 浏览器上传
  -> Raw Sources 复制收纳
  -> hash 去重和类型识别
  -> SourceFile 账本
  -> 正文抽取
  -> Wiki 写入运行
  -> Obsidian Vault: E:\WikiForgeVault
  -> WikiForge/index.md 与 WikiForge/log.md 更新
```

## 3. 已完成修正

- 前端入口收束为 `收纳 / 资料箱 / Wiki / 日志 / 设置`。
- 浏览器上传入口已接入 `/api/v1/upload-sources`。
- 本地路径扫描入口继续使用 `/api/v1/import-jobs/local`。
- Wiki 写入入口已接入 `/api/v1/source-files/{fileUid}/wiki-ingest-runs`。
- 用户可见状态按中文码值返回和展示。
- `DashboardView.vue` 单体入口已退役。
- Docker Compose 默认不再启动 Orchestration。
- 浏览器上传后的 `md/txt/pdf/docx` 会同步生成正文抽取记录，Wiki 写入可以读取正文摘录。
- 当前运行配置已将 Obsidian Vault 指向 `E:\WikiForgeVault`，不再写入项目内测试 Vault。

## 4. 当前运行端口

| 端口 | 用途 | 说明 |
| --- | --- | --- |
| `5174` | 前端开发入口 | 验收页面使用 `http://127.0.0.1:5174/capture` |
| `8080` | Core API | 前端只应通过 Core API 调用后端能力 |
| `8081` | Worker | 仅供 Core 内部调度，不是用户验收入口 |
| `3306` | MySQL | 当前使用 Docker MySQL |

## 5. Obsidian Vault 约定

MVP0 正式本地 Vault：

```text
E:\WikiForgeVault
```

系统只写入 Vault 内的托管目录：

```text
E:\WikiForgeVault\WikiForge\
```

当前会生成：

- `WikiForge/10_来源/yyyy/MM/*.md`
- `WikiForge/index.md`
- `WikiForge/log.md`

Raw Sources 不进入 Obsidian Vault，仍位于项目数据目录：

```text
E:\github\WikiForge\data\raw-sources
```

## 6. 验证记录

已完成验证：

- Core 健康检查：`UP`
- Worker 健康检查：`UP`
- 路径导入 Windows 本机路径成功。
- 浏览器上传成功。
- 浏览器上传后正文抽取成功写入 `source_contents`。
- Wiki 写入成功落盘到 `E:\WikiForgeVault\WikiForge\10_来源\2026\05\`。
- `E:\WikiForgeVault\WikiForge\index.md` 已更新。
- `E:\WikiForgeVault\WikiForge\log.md` 已更新。
- Core 集成测试通过：`ImportJobApiIntegrationTests`、`ObsidianApiIntegrationTests`，共 16 个测试。
- `git diff --check` 通过。

真实资料补写验证：

```text
E:\WikiForgeVault\WikiForge\10_来源\2026\05\file_23acdf843da7434bb376a2cd6a34cf47-LLM Wiki 与 GBrain：AI 知识库的两条技术路线.md.md
```

2026-05-25 Task 8 补充验收：

- 路径导入样例：`job_20260525_b3acc664f2c2`，状态 `已完成`，共 2 个文件，1 个复制收纳，1 个 hash 重复。
- 浏览器上传样例：`job_20260525_238af8a8e035`，状态 `已完成`，上传文件 `file_7726a38d4c2a47b59faca05802099080` 已复制收纳并完成正文抽取。
- Wiki ingest 样例：`wir_20260525_a6886a87da66`，状态 `已写入`，来源页写入 `WikiForge/10_来源/2026/05/file_f39634dd13e34b208e9d9a145e7347b9-mvp0-e2e-clean-20260525-114206-duplicate.md.md`。
- `WikiForge/index.md` 已包含本次 `fileUid`，`WikiForge/log.md` 已包含本次 `runUid`。
- Raw Sources 作为输入路径时被拦截，验证输入路径不得与 Raw Sources 重叠。
- `source_contents` 中可查到本次路径导入文件和上传文件对应的 2 条正文抽取记录。

## 7. 后续开发强约定

后续所有新功能、新服务、新 API、新表、新状态、新原子能力，都必须先更新：

```text
docs/current/项目架构强约定-WikiForge-project-architecture-conventions.md
```

然后再补充对应设计文档和实现。不能绕开四层架构、五入口前端结构、MVP0 最小数据库集合和中文状态码约定。

## 8. 明确不继续的历史方向

以下内容从本节点起不再作为主线继续：

- R7 历史阶段口径。
- 多 Agent 并行产品架构。
- Orchestration 辅助工程服务。
- MCP 主流程入口。
- 向量检索和 Hybrid Search。
- LifeOS。
- 知识体检。
- 旧 Dashboard。

如后续确实需要恢复某项能力，必须重新作为扩展类原子能力出设计文档，并通过架构强约定登记。
