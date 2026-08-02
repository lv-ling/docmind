# 应用层

> 导航：[仓库首页](../README.md) / 应用层

`apps/` 存放三个可独立安装、构建、测试和运行的应用。

```text
apps/
├── docmind-web/          Vue 3 + TypeScript + Vite 用户端
├── docmind-server/       Java 17 + Spring Boot 3 业务后端
└── docmind-document-ai/  Python 3.12 + FastAPI 文档智能服务
```

- [DocMind Web](docmind-web/README.md) 自己持有 `package.json`、`pnpm-lock.yaml`、Node 版本和所有前端源码。
- [DocMind Server](docmind-server/README.md) 自己持有 `pom.xml`、Maven Wrapper 和它对外提供的业务契约。
- [DocMind Document AI](docmind-document-ai/README.md) 自己持有 `pyproject.toml`、`uv.lock` 和模型输出契约。

应用之间只通过 HTTP、消息和版本化数据契约协作，不共享源码包、依赖锁、数据库实体或构建过程。根目录脚本只负责编排这些应用自己的命令，不形成新的包管理层。
