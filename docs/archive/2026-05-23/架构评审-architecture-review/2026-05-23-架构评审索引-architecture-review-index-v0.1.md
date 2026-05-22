# 2026-05-23 架构评审索引 v0.1

## 目录说明

本目录保存 WikiForge 在 2026-05-23 编码前架构评审阶段的外部 AI 评审材料和最终结论。

目录路径：

```text
docs/archive/2026-05-23/架构评审-architecture-review/
```

## 命名规则

文件采用：

```text
YYYY-MM-DD-中文名-EnglishName-版本号.md
```

## 评审输入材料

| 文件 | 角色 |
| --- | --- |
| `2026-05-23-前端产品评审-Claude-Minimax27-frontend-product-review-v1.0.md` | 前端产品体验 |
| `2026-05-23-本地文件系统安全评审-CodeBuddy-Auto-local-filesystem-security-review-v1.0.md` | 本地文件系统安全 |
| `2026-05-23-后端架构评审-Qoder-backend-architecture-review-v1.0.md` | 后端架构 |
| `2026-05-23-Java后端架构评审-Trae-GLM51-java-backend-architecture-review-v1.0.md` | Java 后端架构 |
| `2026-05-23-数据模型DBA评审-Trae-data-model-dba-review-v0.1.md` | DBA / 数据模型 |
| `2026-05-23-数据专家评审-Trae-GLM51-data-expert-review-v1.0.md` | 数据专家 |
| `2026-05-23-AI与MCP扩展架构评审-Trae-DeepSeekV4Pro-ai-mcp-architecture-review-v1.0.md` | AI / MCP 扩展 |

## 评审结论

最终结论文件：

```text
2026-05-23-架构评审结论-WikiForge-architecture-review-conclusion-v1.0.md
```

结论：

```text
可以进入 MVP 0 项目骨架阶段。
```

进入的是工程底座阶段，不是直接进入文件扫描业务。

## 关键采纳项

- MyBatis-Plus 3.5.x。
- Flyway 分阶段 migration。
- CI/CD + Docker Compose 进入 MVP 0。
- 前后端分离镜像。
- 路径白名单和 `Path.toRealPath()` 安全校验。
- 默认不跟随符号链接。
- SHA-256 流式 hash。
- 临时文件 + 原子 rename。
- `sources` 不承载大文本正文，后续拆 `source_contents`。
- AI / MCP 延后到 MVP 3/4。
