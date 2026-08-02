# Server 契约

> 导航：[仓库首页](../../../README.md) / [DocMind Server](../README.md) / Server 契约

`contracts/` 由 DocMind Server 自主维护，定义 Server 对 Web 暴露的 HTTP 边界以及 Server 持有的事件与派生模型 Schema。其他应用不引用本目录源码，只消费明确发布的版本化契约。

```text
contracts/
├── openapi/      公开 HTTP API v1
├── json-schema/ Server 持有的 JSON Schema
└── events/       领域事件说明
```

在 `apps/docmind-server` 中执行，契约结构由 Server 的 Maven 测试验证：

```bash
# 运行 Server 全部门禁，同时验证 OpenAPI 和 JSON Schema 工件
./mvnw verify
```
