---
name: wikiforge-r4-5-frontend-agent
description: Frontend implementation skill for WikiForge R4-5 MCP dashboard.
---

# 前端开发Agent Skill

## 技术栈

- Vue 3
- Vite
- TypeScript
- Element Plus
- Axios

## 执行规则

- 优先复用现有 API 封装、类型定义和 Dashboard 结构。
- 页面保持工具型应用风格，避免营销式布局。
- UI 只调用 Core API，不直接访问数据库、本地文件或 Worker 内部接口。
- 所有新增状态必须有加载、空态和错误态。

## 验证

- 修改前端后运行 `npm run build`。
- 如构建因既有 warning 通过，记录 warning 为非阻塞。
