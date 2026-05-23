# WikiForge 架构样式 Architecture Style

## 来源与取舍

本规范吸收 CDP AI 开发范式中的设计理念：

- 全局强约束、脚手架规范、条线型服务规范分层。
- 每个服务都有清晰职责、包结构、API、数据表归属、异常处理和协作边界。
- 通过文档先约定，再进入编码，降低多人和多 AI 并行开发冲突。

不直接照搬 CDP 的业务、版本和基础设施：

- 不采用 CDP 的业务模型。
- 不强制引入 Nacos、Kafka、Redis、XXL-JOB、TDSQL、StarRocks。
- 不把 MVP 拖入完整微服务治理。

## MVP 架构选择

用户选择 B 方案：少服务微服务。

MVP 0/1 目标运行单元：

```text
wikiforge-ui
wikiforge-core-service
wikiforge-worker-service
mysql
```

MVP 0/1 可预留但不强制运行：

```text
wikiforge-gateway
```

V1/V2 再逐步拆分：

```text
wikiforge-agent-service
wikiforge-connector-service
wikiforge-mcp-service
wikiforge-vector-service
wikiforge-record-service
```

## 架构原则

- 领域优先拆分，不按技术爱好拆分。
- 先拆最稳定边界：Core 负责状态和索引，Worker 负责耗时文件任务。
- 避免分布式复杂度提前进入 MVP。
- 数据库先可共享实例，但表归属必须明确。
- 跨服务调用先用 REST，同步路径简单明确。
- 事件、消息队列、调度中心在任务量和并发压力出现后再引入。

## 知识系统分层

```text
LLM Wiki 表达层:
  Raw Sources
  Obsidian Vault
  Source Note
  Wiki Page
  Markdown / HTML Artifact

GBrain 运行层:
  MySQL control plane
  Core Service
  Worker Service
  Agent Service
  MCP Service
  Vector Export
  Personal Record
```

MVP 先保证表达层闭环可用，再逐步建设运行层。

## 不允许的架构动作

- 不在 MVP 0/1 引入完整服务治理来替代业务闭环。
- 不把文件扫描、Agent 调用、MCP、向量库全部塞进 Core。
- 不让 Worker 直接承载 UI 查询职责。
- 不让 UI 直接访问 Worker 内部任务实现。
- 不绕过 API 和表归属直接跨服务读写数据。
