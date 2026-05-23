# 2026-05-23 WikiForge MVP3 文档解析实施计划 Document Parsing Implementation Plan

## 版本信息

- 文档版本：v0.3
- 当前分支：`codex/mvp3-document-parsing`
- 父分支：`codex/mvp2.1-usability-hardening`
- 当前阶段：S5 / R2 MVP3 文档解析
- 当前测试门禁：T2 后端 Maven 全量测试已通过，T3 Compose 配置校验待提交前复核

## 当前执行指针

- ( ) R2-1 冻结 `source_contents` 正文存储契约
- ( ) R2-2 Worker 支持 Markdown / TXT 解析
- ( ) R2-3 PDF 基础文本抽取
- ( ) R2-4 Word 基础文本抽取
- ( ) R2-5 Source Note 模板加入正文摘录
- ( ) R2-6 文档归档、验证和提交推送

## 节点清单

| 完成 | 节点 | 状态 | 事项 | 测试门禁 |
| --- | --- | --- | --- | --- |
| [x] | R2-1 | Done | 冻结并落地 `source_contents` 表、领域模型、仓储与内部提交契约 | T1 |
| [x] | R2-2 | Done | Worker 对 `.md/.txt` 抽取纯文本并随 Source File 批量提交 | T1 |
| [x] | R2-3 | Done | PDF 基础文本抽取，失败时返回明确原因 | T1 / T2 |
| [x] | R2-4 | Done | Word `.docx` 基础文本抽取 | T1 / T2 |
| [x] | R2-5 | Done | Source Note 草案加入正文摘录和解析状态 | T2 |
| [x] | R2-6 | Done | 更新路线图、开发者日志、归档索引并提交推送 | T0 |

## 契约冻结

### `source_contents` 最小字段

- `content_uid`
- `source_id`
- `source_file_id`
- `parser_name`
- `content_type`
- `raw_text`
- `text_hash`
- `char_count`
- `raw_text_saved`
- `parse_status`
- `parse_error`
- `created_at`
- `updated_at`

### Worker -> Core 扩展字段

`SubmitSourceFileItem` 增加可选字段：

- `parserName`
- `contentType`
- `parsedText`
- `textHash`
- `charCount`
- `rawTextSaved`
- `parseError`

### MVP3 第一轮解析策略

- `.md`：读取 UTF-8 文本，去掉文件开头 YAML frontmatter 后保存正文。
- `.txt`：读取 UTF-8 文本并保存正文。
- 输出正文上限：1 MB 字符串内容，超出则标记 `partial`。
- PDF：使用 Apache PDFBox 提取文本。
- Word：使用 Apache POI 提取 `.docx` 段落文本。
- 其他格式暂保持 `pending`。
- 不引入 AI 摘要、标签、MCP 或向量库。

### 本轮实现结果

- Core 已新增 `source_contents` Flyway migration、领域模型、MyBatis 实体/Mapper/仓储。
- Worker 已支持 `.md/.txt` UTF-8 文本抽取，并提交 `parserName`、`parsedText`、`textHash`、`charCount`、`rawTextSaved` 等字段。
- Markdown 解析会去除文件开头 YAML frontmatter。
- Source Note 草案已加入 `正文摘录 Content Excerpt`。
- PDF / Word 已补齐基础文本抽取，图片、扫描件和 OCR 仍为后续能力。
- 使用 Apache PDFBox 3.0.7 和 Apache POI 5.5.1，属于主流 Java 文档解析依赖。

## 目标文件

后端 Core：

- `backend/wikiforge-core-service/src/main/resources/db/migration/`
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/domain/model/`
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/domain/repository/`
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/infrastructure/persistence/`
- `backend/wikiforge-core-service/src/main/java/com/wikiforge/core/application/service/`
- `backend/wikiforge-core-service/src/test/java/com/wikiforge/core/`

Worker：

- `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/application/dto/`
- `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/application/service/`
- `backend/wikiforge-worker-service/src/main/java/com/wikiforge/worker/infrastructure/filesystem/`
- `backend/wikiforge-worker-service/src/test/java/com/wikiforge/worker/`

文档：

- `docs/current/2026-05-23-项目整体计划-WikiForge-project-roadmap.md`
- `docs/current/2026-05-23-开发者日志-WikiForge-developer-log.md`
- `docs/archive/2026-05-23/2026-05-23-归档索引-archive-index-v2.5.md`

## 验证命令

T1：

```text
mvn -B -s <temp-settings> -gs <temp-settings> test
```

T2：

```text
npm run build
```

T3 可选：

```text
docker compose -f deploy/docker-compose.yml config --quiet
```

## 本轮验证结果

```text
mvn -B -s <temp-settings> -gs <temp-settings> -pl wikiforge-worker-service -am "-Dtest=TextContentExtractorTests" "-Dsurefire.failIfNoSpecifiedTests=false" test: pass
mvn -B -s <temp-settings> -gs <temp-settings> test: pass
npm run build: pass
docker compose -f deploy/docker-compose.yml config --quiet: pass
```
