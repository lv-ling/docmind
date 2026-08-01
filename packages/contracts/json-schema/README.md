# JSON Schema

> 导航：[仓库首页](../../../README.md) / [跨端契约](../README.md) / JSON Schema

这里存放需要由 TypeScript、Java 和 Python 共同解释的运行时 Schema：

- [`model-extraction-output.schema.json`](model-extraction-output.schema.json)：LLM 令牌化抽取输出；通过后仍必须执行二次 PII 扫描和用户 Schema 校验。
- [`domain-event.schema.json`](domain-event.schema.json)：不允许携带敏感明文的 CloudEvents 领域事件信封。
- [`canonical-diff.schema.json`](canonical-diff.schema.json)：不可变文档版本之间的后端权威 Diff。

修改 Schema 时必须保持 `$id`、版本、必填字段、`additionalProperties` 和空值语义明确，并同步检查 [`src/`](../src) 中的 TypeScript 类型及 Java/Python 消费者。验证入口：

```bash
pnpm --filter @docmind/contracts test
```
