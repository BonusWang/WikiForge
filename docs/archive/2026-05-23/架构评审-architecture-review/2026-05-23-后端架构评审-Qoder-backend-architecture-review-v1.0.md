WikiForge 架构评审意见（Java 后端专项）

结论
是否建议进入 MVP 编码：有条件

技术栈选型本身没有问题，但数据模型物理设计、文件操作异常恢复策略、导入任务状态机三个方向存在需要编码前敲定的设计缺口。预计补充设计需要 1 天，之后可以开始 MVP 0。

---

P0 阻塞问题

P0-1：sources 表 raw_text longtext 必须物理拆离

sources 是全系统访问频率最高的表。MVP 1 路径扫描 1000 个文件后，文件列表页每次查询都会命中该表。InnoDB 存储 longtext 时，行内只保留前 768 字节指针，溢出内容存 off-page，但 MyBatis-Plus 的 selectList / selectPage 如果不手动排除该列，仍会触发额外 I/O（即使实际值为 NULL，MySQL 仍需判断并跳过溢出指针）。更关键的是，后续 MVP 3 写入 raw_text 后，单表体积膨胀将直接影响索引查询性能。

必须在写第一个 Flyway migration 前决定：

-- V1__create_sources.sql 不包含 raw_text
CREATE TABLE sources ( ... 不含 raw_text 列 ... );

-- V1__create_source_contents.sql
CREATE TABLE source_contents (
  source_id BIGINT NOT NULL PRIMARY KEY,
  raw_text LONGTEXT,
  raw_text_saved TINYINT(1) NOT NULL DEFAULT 0,
  raw_text_policy VARCHAR(64) NOT NULL DEFAULT 'metadata_only',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_source_contents_source FOREIGN KEY (source_id) REFERENCES sources(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

MyBatis-Plus 侧对应独立 Entity + Mapper，列表查询永远不 JOIN 该表。

P0-2：文件扫描与复制的异常恢复策略未定义

MVP 实施计划描述了"失败不中断任务"，但缺少以下关键场景的恢复方案：

1. 应用崩溃恢复：扫描到第 500 个文件时进程被杀。import_jobs.status = running，但 source_files 只写入了 300 条（事务批量提交）。重启后如何恢复？
2. 文件复制中断：复制大文件（200 MB）时断电。目标目录存在一个不完整文件。再次运行扫描时如何检测并修复？
3. Hash 冲突竞态：同一批次中两个不同路径的文件 hash 相同，几乎同时进入复制逻辑。谁先写入？后者如何安全标记为 duplicate？

必须在 MVP 1 开发前确定的恢复策略：
复制采用"写临时文件 + 原子 rename"模式：先写入 目标路径.tmp，完成后 Files.move(src, target, ATOMIC_MOVE)。临时文件存在即视为上次失败残留，重启时清理。
import_jobs 增加 last_scanned_path TEXT 或 scanned_count INT 字段，标记扫描进度断点。重启后对 status=running 的 job 执行补偿：跳过已有 source_file 记录的文件（按 content_hash + original_path 联合判断），继续扫描。
hash 冲突处理必须在数据库层用唯一约束保证：source_files 表加 UNIQUE(content_hash, import_job_id) 或在 service 层用 INSERT ... ON DUPLICATE KEY UPDATE 保证幂等。

P0-3：Flyway migration 分层策略未约定

当前数据模型定义了 27 张表，但 MVP 0 只需要 system_settings 和 model_providers，MVP 1 需要 sources、source_files、import_jobs，MVP 2 需要 obsidian_notes，MVP 3 需要 agent_runs、agent_steps、review_items。如果第一个 migration 就创建全部 27 张表，会引入大量未使用的外键约束和索引，增加 CI 验证复杂度，也会让开发者误以为所有功能都应该立即实现。

建议 migration 分版本号段：

V1.0.001__create_system_settings.sql
V1.0.002__create_model_providers.sql
V1.1.001__create_sources.sql
V1.1.002__create_source_contents.sql
V1.1.003__create_source_files.sql
V1.1.004__create_import_jobs.sql
V1.2.001__create_obsidian_notes.sql
V1.3.001__create_agent_runs.sql
V1.3.002__create_agent_steps.sql
V1.3.003__create_review_items.sql

预留表（content_chunks、embedding_jobs、mcp_servers、mcp_tool_calls、personal_records、agent_office_status）的 DDL 放到 V1.9.0xx 段，建空表但不建外键约束（因为被引用的业务逻辑还不存在）。

---

P1 高风险问题

P1-1：导入任务状态机缺少取消和暂停语义

当前 import_jobs.status 定义了 pending / running / completed / failed 四态。但用户在 Web UI 创建了一个扫描 50,000 文件的任务后，发现路径配错，当前设计下只能等任务跑完或杀进程。

建议增加：
cancelling：用户请求取消，扫描循环检测到后停止。
cancelled：取消完成。

实现方式：ImportService 持有 ConcurrentHashMap<Long, AtomicBoolean> cancelFlags，文件扫描循环每处理一批（如 100 个文件）检查一次 flag。

P1-2：批量写入 source_files 的事务粒度

一次路径扫描可能产出数千条 source_files 记录。如果整个扫描放在一个事务中：
锁持有时间过长。
内存中 MyBatis SqlSession 缓存膨胀。
任何一条 insert 失败导致全部回滚。

建议采用分批提交策略：

// 伪代码
int BATCH_SIZE = 200;
List<SourceFile> batch = new ArrayList<>(BATCH_SIZE);
Files.walkFileTree(scanRoot, new SimpleFileVisitor<>() {
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        batch.add(buildSourceFile(file, attrs));
        if (batch.size() >= BATCH_SIZE) {
            sourceFileMapper.insertBatch(batch); // 独立事务
            updateJobProgress(job, batch.size());
            batch.clear();
        }
        return FileVisitResult.CONTINUE;
    }
});
// flush 剩余
if (!batch.isEmpty()) { sourceFileMapper.insertBatch(batch); }

每批 200 条一个事务，失败只丢一批，不影响已提交的记录。import_jobs 的 success_count / failed_count 在每批提交后增量更新。

P1-3：MyBatis-Plus JSON 列 TypeHandler 必须统一注册

数据模型中有大量 JSON 类型列（summary_structured、input_json、output_json、token_usage、metadata_json、aliases、config_json、tools_json、structured_json、frontmatter_json）。MyBatis-Plus 不会自动序列化/反序列化 JSON 列，必须：

1. 全局注册 JacksonTypeHandler 或自定义 JsonTypeHandler。
2. 每个 Entity 的 JSON 字段用 @TableField(typeHandler = JacksonTypeHandler.class) 注解。
3. 对应的 Mapper XML（如果有）中也需要指定 typeHandler。

如果忘记配置，运行时会抛 ClassCastException 或静默存入 toString() 结果。建议在 MVP 0 阶段建立一个 common-mybatis 模块或配置类，统一注册 TypeHandler 并在单元测试中验证 JSON 序列化往返正确性。

P1-4：文件扫描必须处理 Windows 长路径问题

用户环境是 Windows（从项目路径 E:\github\WikiForge 可见）。Windows 默认 MAX_PATH = 260 字符。扫描嵌套较深的路径时，加上 WikiForge_RawSources/01_Documents_文档/子目录/子目录/... 的归集路径，很容易超限。

Java NIO 在 Windows 上可以通过 \\?\ 前缀突破长路径限制（Java 11+ 默认支持），但需要确认：
Files.copy 和 Files.move 在长路径下正常工作。
MySQL 存储的路径字段使用 TEXT 类型（已满足）。
前端展示时路径过长需要截断。

建议在 application.yml 中配置 Raw Sources 根目录时做路径长度预检：根路径 + 最大文件名估计 < 200，超限时给出警告。

P1-5：obsidian://open URI 路径编码

Obsidian URI 格式为 obsidian://open?vault=VaultName&file=path/to/file。其中 file 参数值是 vault 内相对路径，需要 URL encode。中文路径、空格、特殊字符必须正确编码。

Java 侧生成 URI 时建议使用：

String uri = "obsidian://open?vault=" + URLEncoder.encode(vaultName, UTF_8)
"&file=" + URLEncoder.encode(relativeVaultPath, UTF_8);

注意 URLEncoder.encode 会把空格编码为 +，而 URI 规范用 %20。建议统一替换或使用 URI 类构建。这个细节如果在 MVP 2 阶段忽略，会导致含空格或中文的文件无法通过 URI 打开。

P1-6：Spring Scheduler 不适合长时间文件扫描任务

文档建议使用 Spring Scheduler 处理定时扫描。但路径扫描任务可能耗时数分钟，而 Spring @Scheduled 默认使用单线程池。如果扫描任务阻塞了调度线程，其他定时任务（如心跳、状态检查）也会被阻塞。

建议将文件扫描任务改为 @Async + 自定义线程池执行：

@Configuration
public class AsyncConfig {
    @Bean("importTaskExecutor")
    public Executor importTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("import-");
        return executor;
    }
}

ImportService.startScan() 用 @Async("importTaskExecutor") 执行，API 立即返回 job ID，前端通过轮询 GET /api/import-jobs/{id} 获取进度。

---

P2 优化建议

P2-1：建议 sources 表增加 file_size 冗余字段

当前 file_size 只在 source_files 表中。但文件列表页排序、大文件筛选、扫描统计都需要文件大小。如果每次都 JOIN source_files，在 MVP 1 路径扫描场景（1 source : 1 source_file）下是无意义的 JOIN 开销。建议在 sources 上冗余一个 file_size BIGINT 字段。

P2-2：content_hash 统一为 SHA-256 并加数据库索引

建议在实施计划中明确：
算法：SHA-256（64 字符 hex）。
字段长度：VARCHAR(64)，当前设计为 VARCHAR(128) 有余量但浪费索引空间，建议改为 CHAR(64) 定长（索引效率更高）。
source_files 表上 content_hash 加索引用于去重查询。
hash 计算使用 Java MessageDigest + NIO FileChannel，对大文件采用流式读取（8KB buffer），不一次性加载到内存。

P2-3：MyBatis-Plus 分页应使用 MybatisPlusInterceptor + PaginationInnerInterceptor

文件列表页和 Source 列表页必然需要分页。建议 MVP 0 阶段就配置好分页拦截器，避免后续每个 Mapper 手写 LIMIT：

@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
}

P2-4：建议引入乐观锁防止 import_job 状态竞态更新

import_jobs 的 status 更新可能存在竞态（API 层面用户点击 retry 时任务恰好完成）。建议：
import_jobs 增加 version INT NOT NULL DEFAULT 0。
MyBatis-Plus Entity 上 @Version 注解。
或者在 UPDATE 时显式 WHERE status = 'expected_status' 并检查 affected rows。

P2-5：建议 Obsidian Writer 的文件写入采用 Write-to-Temp-then-Rename 模式

Path tempFile = targetPath.resolveSibling(targetPath.getFileName() + ".wf.tmp");
Files.writeString(tempFile, markdownContent, UTF_8);
Files.move(tempFile, targetPath, StandardCopyOption.ATOMIC_MOVE);

这样即使写入过程中崩溃，也不会产生半成品 Markdown 文件污染 Obsidian Vault。启动时扫描 Vault 目录清理所有 .wf.tmp 残留。

P2-6：日志规范建议

MVP 必须覆盖的日志点（SLF4J + Logback）：
文件扫描开始/结束/跳过（INFO）
文件复制成功/失败（INFO/ERROR）
Hash 计算耗时超过 1 秒的大文件（WARN）
Import Job 状态变更（INFO）
Agent 调用开始/结束/超时（INFO/WARN）
Obsidian 写入成功/失败（INFO/ERROR）
模型 API 调用耗时和 token 用量（INFO）

建议在 logback-spring.xml 中为 com.wikiforge.import 和 com.wikiforge.agent 包配置独立 appender 输出到 logs/import.log 和 logs/agent.log，与主日志分离，方便排查。

P2-7：system_settings 表建议增加类型校验

当前 setting_value 是 JSON 类型，任何值都能存。建议增加 value_type VARCHAR(32)（string / integer / boolean / json_object / path），Service 层写入时按类型做基础校验。避免前端误传导致配置损坏（例如 obsidian_vault_path 存了一个 JSON 对象）。

---

模块边界建议

建议的 Maven 模块划分（MVP 阶段）

wikiforge-backend/
  pom.xml (parent)
  wikiforge-common/         -- 通用工具、TypeHandler、异常定义、常量
  wikiforge-domain/         -- Entity、枚举、DTO、VO
  wikiforge-mapper/         -- MyBatis-Plus Mapper 接口
  wikiforge-service/        -- 业务逻辑
  wikiforge-web/            -- Controller、全局异常处理、配置
  wikiforge-app/            -- Spring Boot 启动类、application.yml

或者更简单的单模块方案（MVP 推荐）：

wikiforge-backend/
  src/main/java/com/wikiforge/
    common/          -- 工具类、TypeHandler、异常、常量
    config/          -- Spring 配置、MyBatis 配置、Async 配置
    domain/          -- Entity、枚举
    dto/             -- 请求/响应 DTO
    mapper/          -- MyBatis-Plus Mapper
    service/
      settings/      -- SystemSettingsService
      imports/       -- ImportService、RawSourceOrganizer、FileHashService
      obsidian/      -- ObsidianWriter、ObsidianPreviewService
      agent/         -- AgentOrchestrator、ModelProviderAdapter
      review/        -- ReviewService
    controller/      -- REST Controllers
    WikiForgeApplication.java

MVP 阶段建议单模块。理由：单人或小团队开发，多模块 Maven 增加构建复杂度但不带来实际隔离收益。等 MVP 完成、模块边界通过实践验证稳定后，再拆分为多模块。

核心 Service 职责边界

| Service | 输入 | 输出 | 不该做的事 |
|---------|------|------|-----------|
| ImportService | 扫描路径、任务配置 | import_job 记录 + 触发扫描 | 不负责文件复制 |
| FileScanner（内部组件） | Path + 过滤规则 | Stream\<ScannedFile\> | 不写数据库 |
| RawSourceOrganizer | ScannedFile 列表 | 复制后的文件路径 + source_files 记录 | 不调模型 |
| FileHashService | Path 或 InputStream | SHA-256 hex string | 不做文件复制 |
| ObsidianWriter | Source + 模板 + 审核结论 | Obsidian 文件 + obsidian_notes 记录 | 不决定是否审核 |
| AgentOrchestrator | Source ID | agent_run + agent_steps + review_item | 不直接写 Obsidian |
| ReviewService | review_item ID + 用户决策 | 更新状态 + 触发归档 | 不调模型 |
| ModelProviderAdapter | prompt + schema | 结构化 JSON 响应 | 不了解业务上下文 |

关键约束：
ImportService 和 RawSourceOrganizer 之间是同步调用，在同一个 @Async 线程内顺序执行。不需要消息队列。
AgentOrchestrator 和 ObsidianWriter 之间通过 ReviewService 间接衔接。Agent 只生成草案和建议，不直接写磁盘。
ModelProviderAdapter 是无状态的，只做 HTTP 调用封装。重试逻辑在 Adapter 内部，业务层不感知。

---

技术栈建议
后端：Java 21 + Spring Boot 3.x + Maven，合理，无需调整。
前端：不在本次评审范围。
数据库：MySQL 8.x，建议建库时指定 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci。utf8mb4_0900_ai_ci（MySQL 8 默认）也可以但对中文排序略有差异，建议显式声明。
文件解析：commonmark-java（Markdown）、Apache POI 5.x（Word）、Apache PDFBox 3.x（PDF）、metadata-extractor（图片）。不建议 MVP 引入 Tika。
连接池：HikariCP 默认配置（maximumPoolSize=10）对单用户本地系统足够。建议把 connectionTimeout 设为 5000ms，idleTimeout 设为 300000ms。
JSON：Jackson（Spring Boot 默认）。MyBatis JSON 列用 JacksonTypeHandler。不引入 Gson 或 FastJSON。

---

最终建议

下一步是否可以开始 MVP 0 项目骨架：可以，条件如下：

1. 将 P0-1（raw_text 拆表方案）写入 DECISIONS.md 并体现在 Flyway V1 DDL 设计中。
2. 将 P0-2（异常恢复三策略：原子写入 + 断点续扫 + 幂等去重）写入实施计划"错误处理策略"章节。
3. 将 P0-3（Flyway 分段版本号规则）写入实施计划"开发顺序"章节。

以上三项可在一天内完成，不需要写代码，只需要补充设计文档。完成后即可开始 spring init 建项目。
以上是从 Java 后端架构师视角的专项评审。核心结论是：技术选型正确、模块边界清晰、数据流合理，但在物理表设计（longtext 拆表）、运行时异常恢复（崩溃续扫 + 原子写入）和 Flyway 分版本管理三个点上需要先补充明确约定，之后可以正式动手写代码。