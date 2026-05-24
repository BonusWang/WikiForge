# WikiForge 前端标准 Frontend Standard

## 技术栈

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios

## 目录结构

```text
frontend/
  src/
    api/
      settings/
      sources/
      import-jobs/
      obsidian/
      review/
      agents/
    assets/
      styles/
    components/
    layout/
    router/
      modules/
    stores/
    types/
    utils/
    views/
      dashboard/
      settings/
      sources/
      import-jobs/
      obsidian/
      review/
      agents/
```

## 分层规则

- 页面放在 `views/{domain}`。
- 业务 API 放在 `api/{domain}`。
- 业务状态放在 `stores/{domain}`。
- 通用 UI 放在 `components/`。
- API 类型放在 `types/`，不要散落在页面里。
- 请求封装统一在 `utils/request.ts` 或 `services/http.ts`。

## UI 原则

- MVP 以 Console 看板为主。
- 办公室视图作为后续 Agent Office 体验，不阻塞 MVP。
- 操作型页面重视密度、筛选、状态和失败原因。
- 不做营销首页。
- 不把使用说明大段放在页面里，文档说明放到 docs。

## API 约定

前端只调用：

- MVP：Core API。
- 后续：Gateway API。

前端不直接调用 Worker 内部接口，不直连数据库，不读取本地文件系统。

## 构建验证

每次前端改动至少运行：

```text
npm ci
npm run build
```

已有依赖安装完成时，可只运行：

```text
npm run build
```
