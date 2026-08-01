# `@docmind/editor`

> 导航：[仓库首页](../../README.md) / [共享包](../README.md) / 编辑器内核

`@docmind/editor` 提供共享的受控文档模型、模板绑定、文档校验以及安全 HTML 序列化/反序列化。当前包是编辑领域内核，不包含页面级编辑器框架、鉴权、业务 API 调用或租户状态。

## 目录结构

```text
packages/editor/src/
├── model/         ControlledDocument v1、节点与样式类型
├── template/      Schema 字段占位符与数组重复块绑定
├── validation/    文档结构和边界校验
└── html/          白名单策略与确定性 HTML 双向转换
```

## 当前能力

`ControlledDocument` v1 覆盖段落、标题、列表、表格、图片、页眉页脚、目录、分页标记、字段占位符和数组重复块。节点使用稳定 ID；占位符只接受纯文本、清洗后的富文本或受控资源引用，不支持表达式和任意 HTML。

安全 HTML 由受控模型确定性生成，并在惰性 `<template>` 中保存转义后的版本化模型元数据。反序列化只接受匹配白名单版本和固定 CSS 的 DocMind 文档，不把外部 HTML 猜测为内部模型。脚本、事件处理器、外链资源和不安全 CSS 必须在进入模型前移除。

## 开发与验证

```bash
pnpm --filter @docmind/editor typecheck
pnpm --filter @docmind/editor test
pnpm --filter @docmind/editor build
```

不兼容的模型变化必须提升模型版本、提供显式迁移器并补充往返与安全测试。业务页面留在 [`apps/web`](../../apps/web/README.md)，通用展示组件留在 [`packages/ui`](../ui/README.md)。
