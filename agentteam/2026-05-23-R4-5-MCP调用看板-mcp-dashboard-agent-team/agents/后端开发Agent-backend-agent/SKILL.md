---
name: wikiforge-r4-5-backend-agent
description: Backend MCP hardening skill for WikiForge R4-5.
---

# 后端开发Agent Skill

## 技术栈

- Java 21
- Spring Boot
- MyBatis-Plus
- Flyway
- MySQL
- JUnit / Spring Boot Test

## 执行规则

- 遵守 `interfaces -> application -> domain <- infrastructure` 分层。
- MCP 日志不得记录原始敏感内容，只记录长度、hash 或脱敏标记。
- 不返回 Raw Sources、本机绝对路径、Vault 绝对路径。
- 不新增中间件，不提前引入完整 MCP transport。

## 验证

- 优先运行定向测试：`mvn -pl wikiforge-core-service test -Dtest=McpPreviewApiIntegrationTests`
- 如修改公共契约，再运行后端全量 `mvn test`。
