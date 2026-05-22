# WikiForge 文档归档说明

## 归档规则

所有历史文档按日期目录归档，同一天产生的归档文件放在同一个日期目录下。

目录格式：

```text
docs/archive/YYYY-MM-DD/
```

文件格式：

```text
YYYY-MM-DD-中文名-EnglishName-版本号.md
```

示例：

```text
docs/archive/2026-05-23/2026-05-23-需求文档-knowledge-base-prd-v0.2.md
```

主文档建议采用：

```text
中文名-EnglishName.md
```

带日期的过程文档建议采用：

```text
YYYY-MM-DD-中文名-EnglishName.md
```

## 每个日期目录建议包含

- 当日 PRD 快照
- 当日技术架构快照
- 当日数据模型快照
- 当日实施计划或迭代计划
- 当日开发者日志
- 当日参考资料快照
- 当日归档索引

## AI 开发前置规则

参与开发的 AI Agent 在进行代码开发、文档修改、方案设计或需求分析前，必须先查看最新日期快照：

1. 查找 `docs/archive/YYYY-MM-DD/` 中日期最新的目录。
2. 先阅读该目录下的 `YYYY-MM-DD-归档索引-archive-index-vX.Y.md`。
3. 再按任务需要阅读同目录中的需求文档、技术架构、数据模型、实施计划和开发者日志快照。
4. 最后再阅读 `docs/` 下的当前主文档。

如果最新快照、当前主文档和用户当前指令存在冲突，不要自行猜测，应先指出冲突并等待确认。

## 注意

- `docs/archive/` 根目录只放本说明文件。
- 具体归档文件不要直接放在 `docs/archive/` 根目录下。
- 主文档继续放在 `docs/` 下迭代。
- 归档文档作为快照，不再直接修改。
- `README.md` 保持通用入口命名，不强制改成中文 + 英文格式；归档快照可保留 `YYYY-MM-DD-README-vX.Y.md`。
