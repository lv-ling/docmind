# 服务层

> 导航：[仓库首页](../README.md) / 服务层

`services/` 存放可独立部署的后端进程。服务之间通过受控 HTTP 或消息契约协作，不共享数据库实体，也不绕过服务边界读取彼此的数据。

```text
services/
├── api/    Java/Spring Boot 业务 API、持久化和任务编排
└── ai/     Python/FastAPI 文档解析、脱敏、抽取和评测
```

- [业务 API](api/README.md) 是身份、权限、工作区、文档数据和敏感映射的访问边界。
- [AI 服务](ai/README.md) 处理 API 明确转交的文件流或令牌化输入，不直接访问业务存储。

跨服务结构应先在 [`packages/contracts`](../packages/contracts/README.md) 或对应 OpenAPI/JSON Schema 中定义，再由服务实现。
