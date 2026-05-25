# 2026-05-24 WikiForge MVP0 Obsidian LLM Wiki 设计

## 1. 设计目标

MVP0 的 Obsidian 不是聊天问答库，也不是向量库结果展示页。

它是 WikiForge 自动维护的知识表达层：

- Raw Sources 保持不可变，只做事实源和追溯。
- Obsidian LLM Wiki 保存整理后的 Markdown 页面。
- Schema / index / log 约束写入规则、目录和演进记录。
- 系统只写托管区块，尽量不覆盖用户在 Obsidian 中手写的内容。

## 2. Vault 边界

WikiForge 只管理 Obsidian Vault 内的一个目录：

```text
WikiForge/
```

规则：

- `WikiForge/` 之外的用户笔记，MVP0 不写、不改、不删。
- Raw Sources 不放进 Obsidian Vault，避免原始文件污染知识库。
- Wiki 页面只保存来源页、索引和运行记录。
- 所有路径必须是 Vault 相对路径，不允许绝对路径或 `..` 逃逸。

## 3. 目录结构

MVP0 初始化 Vault 时创建：

```text
WikiForge/
  index.md
  log.md
  00_Rules_规则/
    LLM-Wiki写入规则.md
    来源页模板.md
  10_Sources_来源/
    YYYY/
      MM/
        {fileUid}-{安全标题}.md
```

| 路径 | 作用 |
| --- | --- |
| `index.md` | 总目录，展示来源页和最近写入 |
| `log.md` | 追加式演进日志 |
| `00_Rules_规则/` | LLM Wiki 写作规则和页面模板 |
| `10_Sources_来源/` | 每个 SourceFile 对应一个来源页 |

## 4. 页面类型

### 4.1 来源页

来源页是一份资料的事实摘要和追溯入口。

路径：

```text
WikiForge/10_Sources_来源/YYYY/MM/{fileUid}-{安全标题}.md
```

来源页包含：

- 文件 UID。
- 内容 hash。
- Raw Sources 相对路径。
- 原始文件名。
- 文件类型和大小。
- 抽取正文摘要。
- 本次写入状态和失败原因。

来源页不保存宿主机敏感绝对路径。

### 4.2 index.md

`index.md` 是 WikiForge 托管目录的入口页。

内容包括：

- 最近写入的来源页。
- 最近失败记录。
- Raw Sources 和 Vault 当前配置摘要。

### 4.3 log.md

`log.md` 是追加式演进记录。

每次 Wiki ingest 至少追加：

- 时间。
- SourceFile UID。
- 来源页路径。
- 写入状态中文码值。
- 失败原因。

`log.md` 不做历史改写。

## 5. 托管区块

为避免覆盖用户手写内容，WikiForge 只更新带标记的托管区块。

格式：

```markdown
<!-- wikiforge:managed:start type=source fileUid=xxx runUid=xxx -->
系统生成内容
<!-- wikiforge:managed:end -->
```

规则：

- 托管区块内可由系统覆盖。
- 托管区块外不自动修改。
- 如果页面不存在，系统创建完整页面。
- 如果页面存在但没有托管区块，系统只追加新托管区块。
- 用户手写备注建议放在托管区块外。

## 6. Frontmatter 规则

每个 WikiForge 页面都带 YAML frontmatter。

来源页示例：

```yaml
---
wikiforge: true
页面类型: 来源页
文件UID: sf_xxx
内容Hash: sha256_xxx
写入状态: 已写入
失败原因:
创建时间: 2026-05-24T00:00:00+08:00
更新时间: 2026-05-24T00:00:00+08:00
---
```

要求：

- 用户可见状态使用中文码值。
- 不写入宿主机敏感绝对路径。
- Raw Sources 路径只写相对路径或内部 UID。
- 时间使用带时区格式。

## 7. 写入流程

### 7.1 正常写入

```text
SourceFile + SourceContent
  -> 读取 LLM Wiki 写入规则
  -> 生成来源页托管区块
  -> 更新 index.md
  -> 追加 log.md
  -> 记录 wiki_ingest_runs
```

### 7.2 规则式写入

当前 MVP0 先按规则式 Markdown 写入：

- 仍创建来源页。
- 摘要使用规则式正文截断。
- `wiki_ingest_runs.write_status_code` 成功时记为“已写入”。
- 写入失败时使用 `failure_reason` 记录原因。

模型整理和跨资料归档后续单独设计，不在当前接口或状态中预留分支。

### 7.3 失败处理

以下情况记为失败：

- Vault 未配置或不可写。
- 写入路径逃逸 Vault。
- 文件名安全化后为空。
- index/log 写入失败。

失败时：

- 不删除 Raw Sources 文件。
- 不回滚 SourceFile 账本。
- `wiki_ingest_runs.status_code` 记为“失败”。
- UI 展示失败原因中文说明。

## 8. 命名规则

### 8.1 文件名

文件名格式：

```text
{fileUid}-{安全标题}.md
```

安全标题规则：

- 去除路径分隔符。
- 去除控制字符。
- 最大长度 80 个字符。
- 空标题使用“未命名资料”。
- 冲突时追加短 hash。

## 9. 原子能力拆分

| 原子能力 | 归属 | 说明 |
| --- | --- | --- |
| Vault 路径校验 | Common / Core | 确保相对路径不逃逸 |
| 目录初始化 | Core | 创建 WikiForge 托管目录 |
| 页面命名 | Core | 生成安全文件名和 Vault 相对路径 |
| 模板渲染 | Core | 生成来源页内容 |
| 托管区块替换 | Core | 只更新系统托管内容 |
| index 更新 | Core | 重建或更新总目录 |
| log 追加 | Core | 追加演进记录 |
| 原子写入 | Core | 临时文件写入后原子替换 |
| 规则式写入 | Core | 生成当前 MVP0 基础 Markdown |

## 10. 与现有实现的关系

现有 `ObsidianVaultService` 可适配：

- Vault 路径校验。
- 目录创建。
- Markdown 原子写入。
- Obsidian URI 生成。

旧 `WikiCompileService` 已退役删除：

- 旧 `wiki_pages` / `wiki_integrations` / `agent_runs` 语义不进入 MVP0 主流程。
- MVP0 使用 `wiki_ingest_runs` 记录写入结果。
- Source Note 历史模板改为 LLM Wiki 来源页模板。

## 11. 验收规则

后续实现必须满足：

- Vault 初始化只创建 `WikiForge/` 托管目录。
- 系统不写 `WikiForge/` 之外的文件。
- 用户手写内容在托管区块外不会被覆盖。
- `index.md` 和 `log.md` 每次写入后可打开阅读。
- 写入结果记录到 `wiki_ingest_runs`。
- 状态码来自 `system_dictionaries`，用户可见为中文。
- 当前规则式写入失败时记录中文失败原因。

验证命令：

```powershell
.\mvnw -pl wikiforge-core-service test
git diff --check
```
