# 部署目录

> 导航：[仓库首页](../README.md) / 部署

`deploy/` 存放运行环境配置，不包含业务代码。当前仓库已实现本地 Docker Compose 基础设施和可选 ONLYOFFICE Document Server；Kubernetes、网关和可观测性配置尚未加入，后续应在实际实现时再建立对应目录。

```text
deploy/
├── compose/
│   ├── docker-compose.yml    PostgreSQL、Redis、RabbitMQ、MinIO、ONLYOFFICE
│   ├── .env.example          可覆盖的本地默认值
│   ├── README.md             启停、配置和数据安全说明
│   └── ACCESS.md             端口、账号与连接命令
└── onlyoffice/
    └── fonts/                本机合法授权字体挂载入口
```

## 使用入口

- [本地基础设施](compose/README.md)：日常启动、停止、重建和可选编辑服务。
- [容器访问手册](compose/ACCESS.md)：数据库、缓存、队列和对象存储连接方式。
- [ONLYOFFICE 本地资源](onlyoffice/README.md)：Document Server 与字体目录关系。

环境差异通过配置和 Secret 注入。真实密钥、共享测试凭据、生产凭据和授权字体二进制不得提交仓库；部署目录也不得复制应用或服务源码。
