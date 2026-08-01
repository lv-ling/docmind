# JSON Schema

跨服务持久化、任务消息和模型结构化输出使用的 JSON Schema 存放在此目录。

- `model-extraction-output.schema.json`：LLM 令牌化抽取输出；通过后仍必须执行二次 PII 扫描和用户 Schema 校验。
- `domain-event.schema.json`：不允许携带敏感明文的 CloudEvents 领域事件信封。
- `canonical-diff.schema.json`：不可变文档版本之间的后端权威 Diff。
