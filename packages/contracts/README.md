# `@docmind/contracts`

> 导航：[仓库首页](../../README.md) / [共享包](../README.md) / 跨端契约

`@docmind/contracts` 是 Web、Java API 和 Python AI 服务之间数据结构的事实源。代码实现应遵循已审查的契约，不能在各端复制一套名称相近但语义不同的结构。

## 目录结构

```text
packages/contracts/
├── src/            手写 TypeScript 领域契约与统一导出
├── openapi/        HTTP API 的 OpenAPI v1 描述
├── json-schema/    模型输出、事件信封和权威 Diff Schema
├── events/         事件契约导航及后续版本化产物入口
├── generated/      OpenAPI 生成的 TypeScript 类型，不手工修改、不提交 Git
└── dist/           TypeScript 构建产物，不提交 Git
```

`src/index.ts` 是手写 TypeScript 契约的公共入口；OpenAPI 生成类型通过 `@docmind/contracts/openapi` 子路径导入。领域目录目前覆盖 common、identity、source、schema、sensitive、extraction、review、template、instance、diff 和 events。

## 契约入口

- [OpenAPI v1](openapi/README.md)：对外 HTTP 接口目标契约。
- [JSON Schema](json-schema/README.md)：跨语言运行时校验产物。
- [领域事件](events/README.md)：CloudEvents 类型、信封和敏感数据边界。

## 开发与验证

从仓库根目录执行：

```bash
pnpm --filter @docmind/contracts generate
pnpm --filter @docmind/contracts typecheck
pnpm --filter @docmind/contracts test
pnpm --filter @docmind/contracts build
```

`generate` 从 `openapi/v1.yaml` 重建 `generated/openapi.d.ts`；`build` 会先执行生成。修改契约时应同步更新对应测试和消费者，并保持命名、可空性、枚举、版本与敏感字段边界在 TypeScript、OpenAPI、JSON Schema 和服务实现之间一致。
