# 2026-05-24 WikiForge V1 在线资料与个人记录 LifeOS Work Order

## 版本信息

- 文档版本：v0.1
- 当前分支：`codex/v1-lifeos-connectors`
- 阶段：R5 / V1 在线资料与个人记录
- 测试门禁：T2 前后端构建验证；后端 API 至少覆盖集成测试

## 目标

V1 先把 WikiForge 从“本地文件知识库”推进到“个人 LifeOS 收集入口”：用户可以把在线链接、网页收藏、飞书/腾讯文档地址、消费、账单、邮件、人际关系和事件记录先统一录入系统，再决定是否进入 AI 提炼、Obsidian Wiki 编译和后续向量库。

## 当前执行指针

- ( ) R5-0 契约冻结与计划优化
- ( ) R5-1 链接类 Source REST API
- ( ) R5-2 个人记录 REST API
- ( ) R5-3 个人记录 Obsidian 归档模板
- ( ) R5-4 Web UI LifeOS 操作区
- (x) R5-5 集成测试、构建验证、文档归档

## V1 范围

- [x] 支持链接类 Source 创建：手工输入标题、URL、平台、正文或备注，落入现有 `sources` / `source_files` / `source_contents` 链路。
- [x] 支持个人记录创建：`expense`、`bill`、`email`、`relationship`、`event`、`note`。
- [x] 支持个人记录列表、详情、状态筛选和基础汇总。
- [x] 支持把个人记录写入 Obsidian Vault 的 `00_Inbox_收集箱/Personal_个人记录` 目录。
- [x] Web UI 增加 LifeOS 操作区，能创建链接资料、创建个人记录、查看记录和触发 Obsidian 归档。
- [x] 保持 MCP Preview 的 `create_personal_record` 可继续写入同一张表，REST API 与 MCP 数据互通。

## V1 不做

- [ ] 不做真实飞书/腾讯文档 OAuth 或企业应用授权抓取。
- [ ] 不做向量库写入。
- [ ] 不做自动定时 Agent 重组知识。
- [ ] 不做登录、加密和多用户权限。
- [ ] 不把 Vault、Raw Sources、运行数据或真实密钥提交到 Git。

## 验收清单

- [x] `POST /api/v1/link-sources` 可创建链接资料。
- [x] `POST /api/v1/personal-records` 可创建个人记录。
- [x] `GET /api/v1/personal-records` 可分页筛选。
- [x] `GET /api/v1/personal-records/{recordUid}` 可查看详情。
- [x] `POST /api/v1/personal-records/{recordUid}/obsidian-note` 可写入 Vault 并返回相对路径和 Obsidian URI。
- [x] `GET /api/v1/personal-records/summary` 可返回类型和状态汇总。
- [x] Dashboard 能完成链接录入、个人记录录入、记录查看、归档操作。
- [x] 后端定向集成测试通过。
- [x] 前端构建通过。
- [x] `git diff --check` 通过。

## 完成记录

- 后端新增 `LinkSourceService`、`PersonalRecordService` 和对应 REST Controller。
- 数据库 migration `V20260524_001__extend_personal_records_for_v1.sql` 增加个人记录 Obsidian 归档字段。
- 前端新增 `frontend/src/api/lifeos/`、`frontend/src/types/lifeos.ts`，Dashboard 增加 LifeOS 收集区。
- `V1LifeOsApiIntegrationTests` 覆盖链接资料创建、个人记录创建/查询/汇总/归档。
- 验证通过：后端全量 Maven 测试、前端构建、Docker Compose config、`git diff --check`。
