# P07 抽取方案编辑器

## 页面定义

- 路由：`/schemas/:schemaId/edit` 或 `/schemas/new`，对应原型 `#schema-editor`。
- 目标：用字段树配置严格 JSON Schema，实时发现错误并发布不可变版本。

## 布局

- 顶栏：返回、方案名、基于版本、草稿保存状态、保存草稿、发布。
- 左栏：可展开字段树，支持添加、复制、排序和删除。
- 中栏：选中字段属性表单。
- 右栏：生成的 Draft 2020-12 JSON Schema、错误清单和示例预览；不是可任意执行的代码编辑器。

## 方案字段

名称、业务描述、版本说明、双人复核适用说明。字段节点至少包含：

| 分组 | 字段 |
| --- | --- |
| 标识 | 唯一 key、JSON Path、展示名、业务描述 |
| 类型 | string/number/integer/boolean/date/datetime/object/array、nullable |
| 约束 | required、enum、format、RE2 pattern、长度/数值边界、数组项/对象子字段 |
| 默认值 | none 或受类型约束的 literal；禁止脚本和表达式 |
| 敏感 | 敏感等级、掩码策略、字段查看能力 |
| 抽取 | 提示、示例、未找到处理方式 |

## 交互规则

- key、JSON Path、同层 position 唯一；拖拽后持久化稳定 position。
- 改类型时列出会失效的约束，确认后清理，不静默保留非法值。
- object/array 递归添加子字段；限制深度、节点数和总大小。
- 前端 Ajv 即时校验，服务端再次生成并校验权威 Schema。
- 发布前显示变更摘要和所有 warning/error；error 阻断。
- 已发布版本只读；点击“创建新草稿”复制定义。

## 实现拆分

- `SchemaFieldTree`、`SchemaFieldInspector`、`GeneratedSchemaPreview`、`SchemaValidationPanel`。
- 纯 model 负责表单 <-> 契约转换、类型迁移、默认值和约束验证。
- 草稿自动保存可防抖，但发布必须显式并带幂等键。

## 验收

- 所有 PRD 类型和约束可配置，非法组合无法发布。
- 空字符串 literal 与 none 明确区分，nullable 规则正确。
- RE2 不接受回溯特性；错误定位到具体字段。
- 发布后历史版本内容不再变化。

## 子任务

- P07-T1：升级字段契约为 Draft 2020-12 并补限制。
- P07-T2：实现字段树、属性面板和纯转换模型。
- P07-T3：实现保存/发布/版本只读与完整校验测试。

