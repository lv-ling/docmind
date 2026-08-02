# 领域事件契约

> 导航：[仓库首页](../../../../README.md) / [Server 契约](../README.md) / 领域事件

本目录是领域事件文档和后续版本化 Schema 产物的入口。当前实现与契约分布在：

- [`AsyncJobCommand`](../../src/main/java/com/docmind/api/extraction/messaging/AsyncJobCommand.java)：Server 内部持久任务命令。
- [`json-schema/domain-event.schema.json`](../json-schema/domain-event.schema.json)：跨语言运行时校验使用的事件信封 Schema。

事件只能携带任务、工作区、聚合对象和状态等标识信息，不能携带文档文本、抽取值、敏感令牌映射或密钥。新增事件时应同时更新 Java 类型、Schema、契约测试和生产者/消费者，并明确向后兼容策略。
