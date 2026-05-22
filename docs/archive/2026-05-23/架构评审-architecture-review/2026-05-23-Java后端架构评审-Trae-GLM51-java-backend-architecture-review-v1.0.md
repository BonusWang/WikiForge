# WikiForge 架构评审意见

> 评审角色：Java 后端架构师
> 评审日期：2026-05-23
> 评审范围：Spring Boot、MySQL、MyBatis、Flyway、文件扫描、导入任务、目录写入、异常恢复、模块边界、CI/CD 与 Docker 发布

---

## 结论

**是否建议进入 MVP 编码：是（有条件）**

当前架构设计在 MVP 边界、技术栈选择、数据模型分层、CI/CD 发布方案四个维度均已达到可开工水平。以下 P0/P1 问题不阻塞 MVP 0（项目骨架），但建议在 MVP 1（源文件归集）编码前完成确认。

---

## P0 阻塞问题

### P0-1：MyBatis-Plus vs 原生 MyBatis 需最终决策

文档中反复出现 "MyBatis-Plus / MyBatis" 的二选一表述（[技术架构-6.0](file:///E:\github\WikiForge\docs\技术架构-technical-architecture.md#L571-L573)、[实施计划-3](file:///E:\github\WikiForge\docs\2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md#L76-L77)），但未给出最终选择。

**分析**：
- MVP 数据模型以单表 CRUD 为主（sources、source_files、import_jobs、obsidian_notes），MyBatis-Plus 的 `BaseMapper` 可减少大量模板代码。
- MVP 有 15+ 张 MVP 必需表，如果手写 MyBatis XML，会产生大量重复映射工作。
- MyBatis-Plus 的 `LambdaQueryWrapper` 对于多条件筛选（文件类型 + 状态 + 扫描任务 + 去重状态）比手写动态 SQL 更易维护。

**建议**：选择 **MyBatis-Plus**。原因：
1. MVP CRUD 占比极高，MyBatis-Plus 减少 60%+ 样板代码。
2. 国内 Spring Boot 生态中 MyBatis-Plus 是事实标准，不存在冷门风险。
3. 如需复杂 SQL，MyBatis-Plus 可同时使用原生 MyBatis XML，两者兼容。

**影响范围**：`pom.xml` 依赖声明、`application.yml` 配置、所有 Mapper 接口和 Service 层写法。

### P0-2：文件扫描目标路径的安全校验需制定明确规则

文档多次提到 "文件扫描路径必须由用户显式配置"、"防止路径穿越"（[实施计划-8](file:///E:\github\WikiForge\docs\2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md#L569-L573)），但缺少具体校验规则的定义。

**当前缺失的定义**：
1. 白名单根路径的格式和校验方式（绝对路径？是否允许通配符？）
2. 路径穿越检测的具体实现（`Path.normalize()` + `startsWith` 比对是否足够？）
3. 符号链接的处理策略（跟进还是跳过？）
4. 跨盘符扫描行为（Windows C: 和 D: 的边界）

**建议**：在 MVP 0 编码前明确以下规则，写入 `DECISIONS.md`：

```text
路径安全规则 v1.0：
1. 扫描根路径必须为绝对路径，不支持通配符和相对路径。
2. 必须是已存在的目录。
3. 禁止配置系统盘根目录（如 C:\、/）。
4. 禁止配置 %USERPROFILE%、%APPDATA% 等系统关键目录。
5. 路径穿越检测：resolvedPath = Path.of(root).resolve(relative).normalize()；
   resolvedPath.startsWith(Path.of(root).normalize()) 必须为 true。
6. 符号链接默认跳过，仅后续按需开启。
```

**影响范围**：`ImportService` 路径校验逻辑、`SystemSettings` 中 `path_scan_roots` 校验规则。

### P0-3：Flyway migration 脚本的命名和组织规则未定义

Flyway 已确定为数据库迁移工具（[技术架构-6.0](file:///E:\github\WikiForge\docs\技术架构-technical-architecture.md#L576-L577)），但未定义：
1. migration 脚本的命名规范
2. 存放路径
3. 版本号策略
4. baseline 策略（空库首次启动 vs 已有数据的库）

**建议**：
```text
Flyway 规则 v1.0：
- 路径：backend/src/main/resources/db/migration/
- 命名：V{YYYYMMDDHHmm}__{描述}.sql（如 V202605231200__init_schema.sql）
- 版本号：使用时间戳避免多人协作冲突
- baseline-on-migrate: true（允许非空库首次接入）
- 每张表独立一个 migration 文件（MVP 阶段）
```

**影响范围**：`backend/pom.xml`（flyway 依赖）、`application.yml`（flyway 配置）、migration SQL 目录结构。

---

## P1 高风险问题

### P1-1：`raw_text` 字段的 longtext 存储策略需要在 MVP 1 前细化

数据模型中 `sources.raw_text` 为 `longtext` 类型（[数据模型-3](file:///E:\github\WikiForge\docs\数据模型-data-model.md#L84-L87)），文档描述为 "分级保存的解析文本，可为空"。虽然制定了 `raw_text_policy` 分级策略（save / skip_large_file / skip_sensitive / skip_binary / metadata_only），但存在以下风险：

1. **MySQL longtext 性能**：单行 `raw_text` 可能包含几 MB 的解析文本。对于几百个文件的批量导入，`sources` 表可能迅速膨胀到几百 MB 甚至 GB。
2. **查询性能**：`SELECT * FROM sources` 在 Web UI 列表页会加载所有 `raw_text`，导致严重性能问题。
3. **MyBatis-Plus 映射**：默认 `selectById` 会加载 `raw_text`，需要显式排除。

**建议**：
1. MVP 阶段将 `raw_text` 的存储上限设为 1 MB，超过上限的文件只存摘要和路径。
2. 列表查询 API（`GET /api/sources`）默认不返回 `raw_text` 字段，仅在详情 API（`GET /api/sources/{id}`）返回。
3. MyBatis-Plus 使用 `@TableField(select = false)` 或在列表查询 Mapper 中排除 `raw_text` 列。
4. 后续如有大量文本存储需求，可考虑将 `raw_text` 拆为独立表 `source_texts`，按需 JOIN。

**影响范围**：`sources` 表 DDL、Mapper 映射、Source API 响应 DTO、ImportService 解析策略。

### P1-2：批量文件复制缺少进度反馈和取消机制

MVP 1 的导入流程（[实施计划-3](file:///E:\github\WikiForge\docs\2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md#L107-L165)）是批量扫描和复制，但未设计：
1. 实时进度反馈（Web UI 如何知道复制了多少文件？）
2. 任务取消机制（用户点了取消，正在复制的大文件如何处理？）
3. 并发复制的线程模型（顺序复制还是线程池？）

**建议**：
1. ImportJob 执行时，每完成一个文件更新 `import_jobs.success_count/skipped_count/failed_count`。
2. 前端通过轮询 `GET /api/import-jobs/{id}` 获取实时进度（MVP 不引入 WebSocket）。
3. 取消机制：`POST /api/import-jobs/{id}/cancel` 设置 `import_jobs.status = 'cancelling'`，任务线程在下一个文件处理前检查状态并退出。
4. 使用单个后台线程（`@Async` + `ThreadPoolTaskExecutor`）顺序处理，不引入复杂并发。

**影响范围**：ImportJob 状态枚举（需新增 `cancelling`、`cancelled`）、ImportService 执行逻辑、前端轮询。

### P1-3：Obsidian Vault 目录初始化与已有 Vault 的冲突处理

MVP 2 需要初始化 Obsidian Vault 目录结构（[实施计划-3](file:///E:\github\WikiForge\docs\2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md#L178-L209)），但未定义：
1. 如果目标路径已存在 Vault（用户已有 Obsidian 库），是否允许合并？
2. 如何检测已有 `.obsidian/` 配置目录？
3. 目录冲突时的策略（跳过、报错、询问用户？）

**建议**：
1. `POST /api/settings/init-vault` 执行前检测目标路径：
   - 如果为空目录 → 直接初始化。
   - 如果已存在 `.obsidian/` 目录 → 返回 `vault_exists: true`，提示用户确认是否合并。
   - 如果非空但非 Vault → 返回 `directory_not_empty`，提示用户选择空目录或合并。
2. Vault 初始化只创建 WikiForge 子目录（`00_Inbox/`、`01_Projects/` 等），不覆盖已有文件。
3. 合并模式下，已有 `.obsidian/` 配置不修改，仅追加 WikiForge 目录。

**影响范围**：Settings API、ObsidianWriter 初始化逻辑。

### P1-4：同名不同 hash 文件的命名冲突策略未细化

实施计划中提到 "同名不同 hash 文件追加短 hash 后缀"（[实施计划-3](file:///E:\github\WikiForge\docs\2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md#L148-L149)），但：
1. "短 hash" 的具体格式未定义（前 8 位？前 12 位？）
2. 后缀添加到文件名还是扩展名之前？
3. 已经追加过 hash 的文件再次重名怎么办？

**建议**：
```text
命名冲突规则：
- 目标文件名 = {原文件名无扩展名}_{hash前8位}.{扩展名}
- 示例：report.pdf → report_a1b2c3d4.pdf
- 如果仍冲突（极低概率），追加序号：report_a1b2c3d4_2.pdf
- 记录在 source_files.managed_path 中
```

**影响范围**：RawSourceOrganizer 文件复制逻辑。

### P1-5：前后端 Docker 镜像的 Nginx 反向代理配置缺少细节

架构文档中提到 "`wikiforge-frontend`：Nginx 托管前端静态资源，并反向代理 `/api` 到后端"（[技术架构-6.7.1](file:///E:\github\WikiForge\docs\技术架构-technical-architecture.md#L795-L796)），但：
1. Nginx 配置文件模板未定义
2. API 代理地址如何注入（环境变量 `BACKEND_URL`？）
3. 前端开发模式（`npm run dev`）的 API 代理如何与生产模式保持一致？

**建议**：
1. 在 `deploy/docker/` 下提供 `nginx.conf.template`，使用 `envsubst` 在容器启动时替换 `${BACKEND_URL}`。
2. 前端 Vite 开发模式使用 `vite.config.ts` 的 `server.proxy` 代理 `/api` 到 `http://localhost:8080`。
3. Nginx 配置示例：
```nginx
server {
    listen 80;
    location /api/ {
        proxy_pass ${BACKEND_URL};
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }
}
```

**影响范围**：`deploy/docker/frontend.Dockerfile`、`deploy/docker/nginx.conf.template`、`docker-compose.yml`。

### P1-6：CI/CD 流水线中缺少 Flyway migration 干跑校验

架构文档 CI 步骤（[技术架构-6.7.1](file:///E:\github\WikiForge\docs\技术架构-technical-architecture.md#L807-L814)）包含 `mvn test` 和 `mvn package`，但未包含 Flyway migration 的校验步骤。

**风险**：多人协作时，migration 脚本冲突或语法错误要到 Docker Compose 启动时才暴露，排查成本高。

**建议**：
1. CI 中增加一步：启动 Testcontainers MySQL + 执行 `flyway:migrate` + 校验成功。
2. 或使用 `flyway:migrate` 的 `-dryRunOutput` 选项检查 SQL 语法。
3. Maven profile `integration-test` 中使用 Testcontainers 验证 migration。

**影响范围**：`.github/workflows/ci.yml`、`backend/pom.xml`（testcontainers 依赖）。

---

## P2 优化建议

### P2-1：数据模型中 MVP 预留表建议打上注释标记

数据模型定义了 24 个实体（[数据模型](file:///E:\github\WikiForge\docs\数据模型-data-model.md)），其中 MVP 必需的仅 9 张表（[实施计划-6](file:///E:\github\WikiForge\docs\2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md#L508-L528)），MVP 预留 6 张。建议在 migration SQL 和实体类上添加 `@MVP 预留` 注释，避免开发者在 MVP 阶段误用未实现的表。

### P2-2：`sources` 表建议按 `status` 和 `collected_at` 建立联合索引

当前数据模型对 `sources` 表的索引建议（[数据模型-3](file:///E:\github\WikiForge\docs\数据模型-data-model.md#L100-L105)）缺少联合索引。MVP Web UI 的典型查询是 "按状态筛选 + 按时间排序"，联合索引 `idx_sources_status_collected_at(status, collected_at)` 比单列索引效率更高。

### P2-3：文件类型识别建议采用双重检测策略

当前文档建议使用 "Java NIO + MIME 探测"（[技术架构-6.4](file:///E:\github\WikiForge\docs\技术架构-technical-architecture.md#L696-L698)）。建议实际实现使用双重检测：
1. `Files.probeContentType(path)` 获取 MIME 类型。
2. 同时检查文件扩展名作为 fallback。
3. 如果 MIME 类型与扩展名不一致，以 MIME 为准但记录 warning。

### P2-4：`SystemSettings` 不建议用 MySQL 存储文件路径类配置

`system_settings` 表中包含了 `obsidian_vault_path`、`raw_sources_path`、`path_scan_roots` 等文件路径配置（[数据模型-25](file:///E:\github\WikiForge\docs\数据模型-data-model.md#L567-L585)）。这存在一个鸡蛋问题：如果 MySQL 还没启动，应用无法读取 Vault 路径；而 Flyway 迁移的 `application.yml` 已经有 `spring.datasource.*` 配置。

**建议**：
- 文件系统路径（Vault 路径、Raw Sources 路径、扫描根路径）放在 `application.yml` 或环境变量中。
- 业务配置（模型供应商、默认模型、自动归档阈值）放在 `system_settings` 表中。
- 或者：`system_settings` 只存可在运行时动态修改的配置，静态路径配置放在 `application.yml`。

### P2-5：建议在 MVP 0 引入 `springdoc-openapi` 但不强制使用

`springdoc-openapi` 已在技术架构和 MVP 0 计划中列出。建议在 MVP 0 就配置好，但不需要为每个 API 写详细文档注解。好处是：
1. Swagger UI 可以作为开发阶段的 API 调试工具。
2. 前端开发者可以直接从 Swagger UI 查看 API 契约。
3. 后续接入 MCP 时，OpenAPI 规范可以辅助生成 tool schema。

### P2-6：健康检查端点建议扩展自定义指标

当前计划使用 `GET /actuator/health`（[技术架构-6.7.1](file:///E:\github\WikiForge\docs\技术架构-technical-architecture.md#L844-L847)），建议扩展为：
- `/actuator/health`：基础健康检查（DB 连接、磁盘空间）。
- `/actuator/health/readiness`：就绪检查（MySQL 可连接、Vault 路径可读写）。
- `/actuator/health/liveness`：存活检查（应用进程存活）。

这在 Docker Compose `healthcheck` 中很有用，`readiness` 检查可确保 Flyway 迁移完成后再接受流量。

### P2-7：建议预留 `ImportJob.organize_mode = 'dry_run'`

当前 `import_jobs.organize_mode` 枚举为 `copy`、`move`、`index_only`（[数据模型-5](file:///E:\github\WikiForge\docs\数据模型-data-model.md#L152-L153)）。建议新增 `dry_run` 模式：
- 扫描和计算 hash，但不实际复制文件。
- 让用户先预览"会复制哪些文件、会发现多少重复"，确认后再执行实际复制。
- 降低用户首次使用的心理负担。

### P2-8：建议项目根目录提供 `.env.example` 模板

架构文档提到 "是否需要为 NAS / 私有服务器部署预留 `.env.example` 和配置模板"（[架构评审材料-9.7](file:///E:\github\WikiForge\docs\2026-05-23-架构评审材料-WikiForge-architecture-review.md#L237-L238)）。明确建议：是。在 MVP 0 就创建 `.env.example`，包含所有环境变量的默认值和说明注释。

---

## MVP 范围建议

### 建议保留

| 项目 | 所属阶段 | 理由 |
| --- | --- | --- |
| 项目骨架（Spring Boot + Vue + MySQL + Flyway） | MVP 0 | 工程基础，无可争议 |
| 本地路径扫描 + 文件复制归集 + hash 去重 | MVP 1 | MVP 核心价值 |
| Source/File/ImportJob 索引 | MVP 1 | 数据平面基础 |
| Obsidian Vault 目录初始化 + Source Note 模板 | MVP 2 | 最小归档闭环 |
| Markdown 预览 + `obsidian://open` | MVP 2 | 用户体验闭环 |
| 基础 Web UI（设置、导入任务、文件列表、详情） | MVP 1-2 | 必要交互界面 |
| CI/CD 自动构建 + Docker 镜像 + Compose 启动 | MVP 0 | 架构约束，必须同步建设 |

### 建议移出

| 项目 | 当前归属 | 理由 |
| --- | --- | --- |
| 单 LLM 多步骤 AI 辅助整理 | MVP 3 | 评审材料（[架构评审材料-11](file:///E:\github\WikiForge\docs\2026-05-23-架构评审材料-WikiForge-architecture-review.md#L304-L311)）已倾向先砍掉。AI 辅助依赖 MVP 1-2 完成才有测试素材，且 Model Provider Adapter 开发 + Prompt 调试周期不确定。建议将 MVP 压缩为：**路径扫描 → 文件复制归集 → MySQL 索引 → Source Note 模板 → 人工编辑 → Obsidian 写入**，等 MVP 1-2 验收通过后再启动 MVP 3 AI 辅助。 |
| Agent 流水线（agent_runs / agent_steps / agent_orchestrator） | MVP 3 | 如 AI 辅助移出，Agent 流水线也跟随移出。MVP 1-2 阶段只需要记录导入任务状态，不需要完整的 Agent 编排。 |
| Review 队列（review_items） | MVP 3 | 没有 AI 辅助生成草案，审核队列的 MVP 价值不明确。MVP 2 可以先做 Source Note 模板 + 人工在 Obsidian 中编辑，确认内容后标记 `status = archived`。 |

### 建议新增

| 项目 | 理由 |
| --- | --- |
| 文件复制 dry-run 预览 | 降低用户首次使用风险，先看结果再执行复制 |
| 导入任务取消机制 | 避免用户等待大量文件复制时无法中断 |
| 文件列表页的多条件筛选 API | MVP 1 Web UI 文件列表需要按文件类型、状态、去重状态、扫描任务筛选。后端需提供 `GET /api/sources?type=pdf&status=copied&import_job_id=1` |

### 调整后的 MVP 阶段

| 阶段 | 内容 | 核心交付 |
| --- | --- | --- |
| MVP 0 | 项目骨架 + CI/CD + Docker Compose | 工程跑通、自动构建 |
| MVP 1 | 源文件归集（扫描、复制、去重、索引、Web UI） | 文件从分散到集中 |
| MVP 2 | Obsidian Source Note（Vault 初始化、模板、Markdown 预览、obsidian://open） | 文件进入知识层 |
| MVP 3 | AI 辅助整理（Model Provider Adapter、Agent Pipeline、Review Queue） | AI 辅助提炼 |
| MVP 4 | 轻量 MCP 预览版 | 外部 Agent 接入基础 |

---

## 技术栈建议

### 后端

| 选项 | 建议 | 理由 |
| --- | --- | --- |
| Java 版本 | **Java 21 LTS** | 新项目首选，虚拟线程在文件复制和扫描中可选使用 |
| 框架 | **Spring Boot 3.x** | 已确认，成熟稳定 |
| 数据访问 | **MyBatis-Plus 3.5.x** | 减少 CRUD 样板代码，MVP 表多 CRUD 多 |
| 数据库迁移 | **Flyway 10.x** | 版本化迁移，Spring Boot 集成成熟 |
| 构建工具 | **Maven 3.9+** | 国内主流通用 |
| API 文档 | **springdoc-openapi 2.x** | 开发调试和前端契约 |
| 监控 | **Spring Boot Actuator** | Docker healthcheck 依赖 |
| 文件解析 | **Apache POI** (Word) + **Apache PDFBox** (PDF) + **flexmark-java** (Markdown) | 各司其职，避免 Tika 重依赖 |
| 测试 | **JUnit 5 + Mockito + Testcontainers** | MVP 0 就配置好 |
| 日志 | **SLF4J + Logback** | Spring Boot 默认，导入/复制/错误全部落日志 |

### 前端

| 选项 | 建议 | 理由 |
| --- | --- | --- |
| 框架 | **Vue 3 + Vite + TypeScript** | 已确认 |
| UI 库 | **Element Plus** | 已确认，后台管理首选 |
| Markdown 渲染 | **markdown-it + DOMPurify** | markdown-it 更灵活，DOMPurify 防 XSS |

### 数据库

**MySQL 8.0.x**（已确认）。建议：
- 字符集：`utf8mb4`，排序规则：`utf8mb4_unicode_ci`
- InnoDB 引擎（所有表）
- `sources.content_hash` 列使用 `VARCHAR(64)` + 索引，SHA-256 输出 64 位十六进制

### 文件解析

| 文件类型 | MVP 工具 | 解析深度 |
| --- | --- | --- |
| Word (.docx) | Apache POI (XWPF) | 提取正文文本 |
| PDF | Apache PDFBox | 提取文本，失败标记 `parse_status=failed` |
| Markdown (.md) | flexmark-java | 提取正文 + frontmatter (YAML) |
| 图片 (.jpg/.png) | metadata-extractor | 仅元数据，不 OCR |
| 纯文本 (.txt) | Java NIO | 直接读取 |
| 未知类型 | - | `parse_status = pending`，仅建索引 |

---

## CI/CD 与部署建议

### 总体评价

当前架构（[技术架构-6.7.1](file:///E:\github\WikiForge\docs\技术架构-technical-architecture.md#L763-L867)）的 CI/CD 设计已经相当完善，覆盖了：
- 前后端分离镜像
- Docker Compose 管理 MySQL
- Volume 持久化
- 环境变量注入
- Flyway 自动迁移
- Actuator 健康检查

以下是对各子项的评审：

### 构建流程：通过 ✅

建议的 7 步 CI 流水线（mvn test → mvn package → npm ci → npm build → docker build backend → docker build frontend → compose smoke test）设计合理。

**补充建议**：
- 增加 Flyway migration 干跑校验（参见 P1-6）。
- `npm run lint` 加入前端 CI 步骤，利用 ESLint 配置在 MVP 0 就约束代码风格。

### Docker 镜像：通过 ✅

前后端分离镜像 (`wikiforge-backend` + `wikiforge-frontend`) 策略正确。前端独立 Nginx 镜像比打进 jar 更清晰。

**补充建议**：
- 后端镜像使用多阶段构建：`maven:3.9-eclipse-temurin-21` 构建 → `eclipse-temurin:21-jre` 运行，减小镜像体积。
- 前端镜像：`node:20-alpine` 构建 → `nginx:1.25-alpine` 运行。

### Docker Compose：通过 ✅

Compose 管理 backend + frontend + mysql 三层，结构清晰。

**补充建议**：
- `docker-compose.yml` 和 `docker-compose.dev.yml` 分离：
  - `docker-compose.yml`：生产模式，前端 Nginx 代理 `/api` 到后端。
  - `docker-compose.dev.yml`：开发模式，前端 `npm run dev`、后端 `mvn spring-boot:run`、MySQL 容器。
- 增加 `depends_on` + `condition: service_healthy` 确保 MySQL 就绪后启动后端。

### 配置与环境变量：通过 ✅

8 个关键环境变量（`SPRING_DATASOURCE_URL`、`WIKIFORGE_RAW_SOURCES_PATH` 等）覆盖了运行必需配置。

**补充建议**：
- API Key 类配置（`WIKIFORGE_MODEL_API_KEY`）应在 Compose 中使用 `env_file` 或 Docker secrets，不在 `docker-compose.yml` 中明文。
- 提供 `.env.example` 模板（参见 P2-8）。

### Volume 挂载：通过，有补充建议 ⚠️

建议的 4 个挂载点（mysql、raw-sources、obsidian-vault、logs、config）合理。

**补充建议**：
- Obsidian Vault 在 Docker 容器内挂载时，需要确认 `obsidian://open` URI 的路径转换。Docker 容器内的路径（如 `/data/wikiforge/obsidian-vault/note.md`）与宿主机路径不同。建议在 `obsidian_notes.absolute_path` 中存储宿主机路径（通过环境变量 `WIKIFORGE_HOST_OBSIDIAN_VAULT_PATH` 注入），用于生成 `obsidian://open` URI。
- 这是一个需要 **MVP 0 验证** 的关键点。

### 健康检查：通过 ✅

`/actuator/health` + Docker healthcheck 方案可行。

**补充建议**：
- 参见 P2-6，扩展 readiness/liveness 端点。
- Compose healthcheck 示例：
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### 需要提前调整的架构点

1. **Obsidian URI 路径双轨**（见上）：容器内路径 vs 宿主机路径，MVP 0 必须验证 `obsidian://open` 在 Docker 部署下的可行性。
2. **Flyway 在空库上的 migration**：CI 必须包含 Testcontainers MySQL 校验。
3. **前端 API 地址**：开发模式 `vite proxy` 和生产模式 `nginx proxy` 必须保持一致的路由前缀 `/api/`。

---

## 数据模型建议

### 需要保留

| 表 | 理由 |
| --- | --- |
| `sources` | MVP 核心实体，所有资料的中心对象 |
| `source_files` | 文件导入信息，MVP 1 必需 |
| `import_jobs` | 导入任务追踪，MVP 1 必需 |
| `obsidian_notes` | Obsidian 文件映射，MVP 2 必需 |
| `model_providers` | 模型供应商配置，MVP 0 预留 |
| `system_settings` | 系统配置，MVP 0 必需 |

### 需要收敛

| 表 | 建议 |
| --- | --- |
| `projects` / `topics` / `entities` / `tags` / `actions` | MVP 1-2 阶段 Sources 的 project/topic/tag 可以先存为 `sources` 表的 JSON 字段（或简单的逗号分隔字符串），不需要完整的多对多关联表。完整的多维分类体系放到 MVP 3（AI 辅助整理）时再建表。**理由**：MVP 1-2 阶段没有 AI 自动分类，用户手动输入 project/topic/tag 只是文本标签，不需要独立实体。 |
| `agent_runs` / `agent_steps` / `review_items` | 如果 AI 辅助移出 MVP 范围（见 MVP 范围建议），这三张表跟随移出。MVP 1-2 只需要 `import_jobs` 追踪状态。 |
| `content_chunks` / `embedding_jobs` | 预留表，MVP 阶段只建表不填充数据。 |
| `mcp_servers` / `mcp_tool_calls` | 预留表，MVP 4 才使用。 |
| `personal_records` | 预留表，V1 才使用。 |
| `agent_office_status` | 预留表，V2 才使用。 |

### 需要新增

| 表/字段 | 建议 |
| --- | --- |
| `import_jobs.organize_mode` | 增加 `dry_run` 枚举值（参见 P2-7） |
| `import_jobs.status` | 增加 `cancelling` 和 `cancelled` 枚举值 |
| `sources` 联合索引 | `idx_sources_status_collected_at(status, collected_at)`（参见 P2-2） |

### MVP 数据表收敛方案

```text
MVP 0-2 实际建表：
  - sources（核心 + project/topic/tag 先存为简单字段）
  - source_files
  - import_jobs
  - obsidian_notes
  - model_providers（预留配置）
  - system_settings

MVP 0-2 只建空表（预留结构，不实现业务逻辑）：
  - content_chunks
  - embedding_jobs
  - mcp_servers
  - mcp_tool_calls
  - personal_records
  - agent_office_status

MVP 3 建表：
  - projects / topics / entities / tags / actions（多维分类体系）
  - agent_runs / agent_steps / review_items（Agent 流水线）
  - source_projects / source_topics / source_entities / source_tags / source_actions（多对多关联）
```

---

## 模块边界评审

### ImportService 边界：清晰 ✅

职责：扫描路径 → 识别文件 → 计算 hash → 调用 RawSourceOrganizer 复制 → 创建 Source 记录。边界清晰，输入为 `ImportJob` 配置，输出为 `sources` + `source_files` 记录。

### RawSourceOrganizer 边界：清晰 ✅

职责：接收文件路径和 hash → 判断去重 → 复制到 Raw Sources 规范目录 → 返回 managed_path。与 ImportService 通过接口解耦，便于后续支持不同整理策略。

### ObsidianWriter 边界：清晰 ✅

职责：接收 Source + 模板 → 生成 Markdown → 写入 Vault → 更新 obsidian_notes。与 ImportService 和 Agent Orchestrator 通过接口解耦。

### 文件解析层边界：清晰 ✅

各解析器（PDFBox、POI、flexmark）独立封装，通过统一 `FileParser` 接口返回 `ParsedDocument`。新增文件类型只需新增 Parser 实现。

### Model Provider Adapter 边界：清晰 ✅

通过 `ModelProvider` 接口屏蔽不同供应商差异。MVP 可先实现 DeepSeek adapter，后续扩展 MiniMax 等。

---

## 异常恢复评审

当前文档的异常处理策略（[实施计划-7](file:///E:\github\WikiForge\docs\2026-05-22-MVP实施计划-WikiForge-mvp-implementation-plan.md#L529-L567)）覆盖了：文件扫描失败、复制失败、重复文件、解析失败、AI 调用失败、Obsidian 写入失败。整体合理。

**补充建议**：

1. **ImportJob 断点续传**：如果任务在执行中被取消或崩溃，重启后应根据 `source_files` 中已成功的记录跳过已处理文件，而不是全量重扫。实现方式：扫描阶段先收集文件列表（不复制），然后逐个处理并写入 `source_files`，下次扫描时对比 hash 跳过已存在的。

2. **文件复制的事务边界**：每个文件的复制和数据库写入应该是原子操作。单个文件失败不应回滚整个任务。建议伪代码：
```java
for (FileInfo file : scannedFiles) {
    try {
        String managedPath = organizer.copy(file);
        sourceFileMapper.insert(createRecord(file, managedPath, "copied"));
        importJobMapper.incrementSuccessCount(jobId);
    } catch (Exception e) {
        sourceFileMapper.insert(createRecord(file, null, "failed"));
        importJobMapper.incrementFailedCount(jobId);
        log.error("Failed to process file: {}", file.getPath(), e);
    }
}
```

3. **Obsidian 写入的原子性**：先写临时文件（`note.md.tmp`），写入成功后 rename 为正式文件名（`note.md`），避免写入中断导致文件损坏。

---

## 最终建议

### 下一步是否可以开始 MVP 0 项目骨架：**可以**

当前架构设计在以下维度已满足开工条件：

| 维度 | 状态 | 说明 |
| --- | --- | --- |
| MVP 边界 | ✅ | 源文件归集 + Obsidian Source Note 闭环明确 |
| 技术栈 | ✅ | Java 21 + Spring Boot 3 + Vue 3 + MySQL 8，全主流 |
| 数据模型 | ✅ | MVP 表收敛方案已提出，可执行 |
| CI/CD 发布 | ✅ | 构建流程、镜像、Compose、volume、健康检查方案完整 |
| 模块边界 | ✅ | ImportService / Organizer / Writer / Parser 边界清晰 |
| 异常恢复 | ✅ (有补充) | 基本策略已定义，断点续传和原子写入已补充 |

### MVP 0 启动前必须完成的 3 件事

1. **确认 MyBatis-Plus**（P0-1）
2. **制定路径安全规则并写入 DECISIONS.md**（P0-2）
3. **制定 Flyway migration 规范**（P0-3）

### MVP 0 的交付清单建议

```text
MVP 0 交付物：
  backend/
    pom.xml（Spring Boot 3.x + MyBatis-Plus + Flyway + MySQL + Testcontainers）
    src/main/resources/application.yml
    src/main/resources/db/migration/V202605231200__init_schema.sql（MVP 表）
    HealthController + /actuator/health
  frontend/
    package.json（Vue 3 + Vite + Element Plus + markdown-it）
    vite.config.ts（含 /api proxy）
    基础布局 + 系统设置页
  deploy/
    docker/backend.Dockerfile（多阶段构建）
    docker/frontend.Dockerfile（多阶段构建）
    docker/nginx.conf.template
    docker-compose.yml
    docker-compose.dev.yml
  .github/workflows/ci.yml（mvn test + npm build + docker build + compose smoke test）
  .env.example
```

---

> **评审结论：有条件通过。解决 3 个 P0 问题后可立即启动 MVP 0 编码。**
>
> P1 问题不阻塞 MVP 0 骨架，但必须在 MVP 1（源文件归集）编码前解决，否则会影响文件扫描、复制和索引的核心体验。
>
> 建议将 AI 辅助整理从 MVP 范围暂时移出，进一步降低 MVP 复杂度和不确定性，等 MVP 1-2 验收通过后再启动。
