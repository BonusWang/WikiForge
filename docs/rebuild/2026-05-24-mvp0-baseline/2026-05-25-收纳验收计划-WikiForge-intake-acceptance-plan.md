# 2026-05-25 WikiForge 收纳验收计划 Intake Acceptance Plan

## 1. 验收目标

本计划用于验收 WikiForge MVP0 的“收纳”心智是否成立。

验收目标不是证明系统能上传文件，而是证明系统能把个人电脑里的散落资料纳入统一资料仓库，并为后续正文抽取和 Obsidian Wiki 写入提供稳定来源。

目标闭环：

```text
散落资料
  -> 进入统一资料仓库
  -> SourceFile 账本登记
  -> 正文抽取
  -> Obsidian LLM Wiki 写入
  -> index.md / log.md 更新
```

## 2. 已确认产品决策

### 2.1 正式资料仓库

MVP0 验收时，正式资料仓库放在 Obsidian Vault 的 WikiForge 托管目录内：

```text
E:\WikiForgeVault\WikiForge\30_Resources_资源\
```

`E:\github\WikiForge\data\raw-sources\` 仅作为开发测试目录，不作为用户正式资料仓库验收口径。

### 2.2 收纳不是单一复制动作

“收纳”定义为：让资料进入 WikiForge 可追踪、可抽取、可整理的管理范围。

不同来源使用不同入库策略：

| 来源场景 | 默认策略 | 说明 |
| --- | --- | --- |
| 浏览器上传 | 复制入库 | 上传天然创建一份新文件，复制到资料仓库是合理默认 |
| 本地散落文件整理 | 预览后移动入库 | 用于清理磁盘散落文件，仓库成为唯一可信位置 |
| 已在稳定资料库或 Vault 中的文件 | 引用入库 | 不复制、不移动，只登记路径、hash 和抽取结果 |
| 已经在 WikiForge 资料仓库内的文件 | 跳过或更新账本 | 避免重复入库 |

## 3. 验收前置环境

### 3.1 服务

本地验收需要以下服务运行：

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| Frontend | `http://127.0.0.1:3000` | 用户验收入口 |
| Core | `http://127.0.0.1:8080` | 前端只通过 `/api` 代理调用 |
| Worker | `http://127.0.0.1:8081` | 只接受 Core 内部调用 |
| MySQL | `127.0.0.1:3306` | MVP0 最小表集合 |

### 3.2 路径

| 路径 | 用途 |
| --- | --- |
| `E:\WikiForgeVault\` | 本机 Obsidian Vault |
| `E:\WikiForgeVault\WikiForge\30_Resources_资源\` | 正式资料仓库 |
| `E:\WikiForgeAcceptance\inbox\` | 散落文件整理验收输入目录 |
| `E:\vault\ai-context\profile.md` | 引用入库验收样本 |

### 3.3 测试样本

验收前准备：

```text
E:\WikiForgeAcceptance\inbox\待归仓文件.md
E:\WikiForgeAcceptance\inbox\待归仓PDF.pdf
E:\vault\ai-context\profile.md
一个大于 2MB 且小于 100MB 的浏览器上传文件
```

## 4. 验收用例

### 4.1 浏览器上传复制入库

步骤：

1. 打开 `http://127.0.0.1:3000`。
2. 进入“收纳”。
3. 在“浏览器上传”区域选择一个大于 2MB 且小于 100MB 的文件。
4. 点击“上传收纳”。

通过标准：

- 页面不出现 `Network Error`。
- 收纳任务显示“已完成”。
- 文件出现在 `E:\WikiForgeVault\WikiForge\30_Resources_资源\` 下。
- 资料箱出现对应 SourceFile 记录。
- SourceFile 记录保留 `browser-upload://...` 来源标识。
- SourceFile 记录包含文件大小、hash、托管路径和中文状态。

失败标准：

- 文件仍写入 `E:\github\WikiForge\data\raw-sources\`。
- 上传大于 1MB 文件时报错。
- 页面只显示英文技术状态。

### 4.2 本地散落文件预览后移动入库

步骤：

1. 在“收纳”页输入：

```text
E:\WikiForgeAcceptance\inbox\
```

2. 选择或默认使用“移动入库”。
3. 点击“扫描预览”。
4. 确认预览中的文件数量、目标路径、重复文件判断。
5. 点击“确认入库”。

通过标准：

- 系统先展示预览，不直接移动文件。
- 确认后，文件从 `E:\WikiForgeAcceptance\inbox\` 移走。
- 文件进入 `E:\WikiForgeVault\WikiForge\30_Resources_资源\`。
- SourceFile 记录保留原路径和新托管路径。
- 收纳状态显示为中文成功态。

失败标准：

- 未预览就直接移动。
- 原路径仍保留同一份文件。
- 目标仓库缺少文件。
- 账本没有记录原路径。

### 4.3 稳定资料引用入库

步骤：

1. 在“收纳”页输入：

```text
E:\vault\ai-context\profile.md
```

2. 选择“引用入库”。
3. 点击“确认入库”。

通过标准：

- 原文件仍保留在 `E:\vault\ai-context\profile.md`。
- `30_Resources_资源` 不新增重复副本。
- SourceFile 记录显示该文件为引用入库。
- SourceFile 记录可进入正文抽取和 Wiki 写入流程。

失败标准：

- 系统复制出第二份 `profile.md`。
- 系统移动或修改原文件。
- 引用文件不能进入资料箱。

### 4.4 hash 去重

步骤：

1. 对同一份文件重复执行一次入库。
2. 刷新“最近收纳任务”和“资料箱”。

通过标准：

- 系统识别内容 hash 已存在。
- 不生成第二份托管文件。
- 新记录标记为重复，或原记录被复用。
- 用户能看懂重复原因。

失败标准：

- 仅因文件名不同就生成重复副本。
- 重复文件没有可见提示。

### 4.5 Wiki 写入

步骤：

1. 进入“资料箱”。
2. 选择一条已入库资料。
3. 触发“整理到 Wiki”。

通过标准：

- `E:\WikiForgeVault\WikiForge\10_Sources_来源\` 生成对应来源页。
- `E:\WikiForgeVault\WikiForge\index.md` 更新。
- `E:\WikiForgeVault\WikiForge\log.md` 追加记录。
- 写入不逃逸 `E:\WikiForgeVault\WikiForge\`。
- SourceFile 最新 Wiki 状态变为中文成功态。

失败标准：

- 写入 Vault 根目录或 `WikiForge/` 外部目录。
- 覆盖用户托管区块外内容。
- Wiki 写入失败但无错误说明。

## 5. 工程验收

每轮实现完成后必须执行：

```powershell
git diff --check
mvn -f backend\pom.xml test
npm --prefix frontend run build
docker compose -f deploy\docker-compose.yml config
```

完成本机联调后，还需要真实调用：

```powershell
Invoke-RestMethod http://127.0.0.1:3000/api/health
Invoke-RestMethod http://127.0.0.1:3000/api/v1/obsidian/status
```

并通过浏览器完成 4.1 到 4.5 的人工验收。

## 6. 实现拆分建议

### 阶段一：验收口径对齐

目标：

- 文档和设置页统一将正式资料仓库指向 `30_Resources_资源`。
- `data/raw-sources` 降级为开发测试目录。

验收：

- 文档、设置页、运行配置不再互相冲突。

### 阶段二：本地入库策略

目标：

- `organizeMode` 支持 `copy`、`move`、`reference`。
- 本地散落文件默认使用 `move`。
- 稳定资料可选择 `reference`。
- Worker 按策略复制、移动或仅登记。

验收：

- 4.2、4.3、4.4 通过。

### 阶段三：预览确认

目标：

- 新增扫描预览，不直接写账本、不移动文件。
- 前端展示文件数量、目标路径、重复判断和风险提示。
- 用户确认后才创建实际收纳任务。

验收：

- 4.2 中“未预览就直接移动”的失败标准被消除。

### 阶段四：端到端验收

目标：

- 浏览器上传、本地移动、引用入库、去重、Wiki 写入全部跑通。

验收：

- 4.1 到 4.5 全部通过。
- 工程验收命令全部通过。

## 7. 当前已知缺口

截至本计划建立时，当前代码仍存在以下缺口：

- 本地路径收纳会直接创建任务并调 Worker，还没有“扫描预览 -> 用户确认”。
- 已在资料仓库内的文件尚未按“跳过或更新账本”单独优化。

这些缺口就是下一轮实现和验收的工作清单。
