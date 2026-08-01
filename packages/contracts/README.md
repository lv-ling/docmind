# @docmind/contracts

DocMind 的跨端契约包，是 Web、Java API 和 Python AI 服务之间数据结构的唯一事实源。

## 目录

```text
src/            TypeScript 契约和包入口
openapi/        HTTP API 的 OpenAPI 文档
json-schema/    持久化、任务和模型输出使用的 JSON Schema
events/         CloudEvents 事件 Schema
dist/           TypeScript 构建产物，不提交 Git
generated/      根据契约生成的代码，不手工修改、不提交 Git
```

## 命令

```bash
pnpm --filter @docmind/contracts build
pnpm --filter @docmind/contracts generate
pnpm --filter @docmind/contracts typecheck
pnpm --filter @docmind/contracts test
```

手写业务契约从 `src/index.ts` 统一导出；OpenAPI 生成类型通过 `@docmind/contracts/openapi` 子路径导入。`build` 会先重新生成类型，`generated/` 不手工修改和提交。
