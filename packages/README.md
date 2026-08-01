# 共享包

> 导航：[仓库首页](../README.md) / 共享包

`packages/` 是 pnpm 工作区中的可复用模块。共享包可以被应用引用，但不得反向依赖 `apps/*` 或具体后端服务。

```text
packages/
├── contracts/    跨端类型、OpenAPI、JSON Schema 和事件契约
├── editor/       受控文档模型、模板绑定、校验和安全 HTML
└── ui/           无业务状态的 Vue 组件与设计令牌
```

- [`@docmind/contracts`](contracts/README.md) 应作为跨进程结构的事实源。
- [`@docmind/editor`](editor/README.md) 负责可复用的文档领域能力。
- [`@docmind/ui`](ui/README.md) 只提供通用展示组件和样式令牌。

各包使用自己的 `build`、`typecheck` 和 `test` 脚本，也可以从根目录通过 pnpm filter 执行。
