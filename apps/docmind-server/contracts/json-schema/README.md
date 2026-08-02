# JSON Schema

> 导航：[仓库首页](../../../../README.md) / [Server 契约](../README.md) / JSON Schema

这里存放由 Server 持有的运行时 Schema：

- [`domain-event.schema.json`](domain-event.schema.json)：不允许携带敏感明文的 CloudEvents 领域事件信封。
- [`canonical-diff.schema.json`](canonical-diff.schema.json)：不可变文档版本之间的后端权威 Diff。

修改 Schema 时必须保持 `$id`、版本、必填字段、`additionalProperties` 和空值语义明确。在 `apps/docmind-server` 中执行验证：

```bash
# 解析并检查所有 Server 自持 JSON Schema 的版本和元数据
./mvnw verify
```
