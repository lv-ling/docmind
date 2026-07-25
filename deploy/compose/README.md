# 本地基础设施

启动：

```bash
docker compose -f deploy/compose/docker-compose.yml up -d
```

日常操作也可以使用项目脚本：`./scripts/dev/infra.sh start`、`stop`、`restart`、`status`。

每个容器的浏览器、命令行和应用连接方式见 [容器访问手册](ACCESS.md)。

需要应用 Compose 配置变更时使用 `./scripts/dev/infra.sh recreate`，它只重建容器，不删除数据卷。复制 `.env.example` 为 `.env` 可以覆盖本地端口或密码；PostgreSQL/RabbitMQ 的初始化密码在已有数据卷上不会自动修改。

服务地址：

| 服务 | 地址 | 开发账号 |
| --- | --- | --- |
| PostgreSQL | `localhost:5432` | `docmind / 12345678`，数据库 `docmind` |
| Redis | `localhost:6379` | 密码 `12345678` |
| RabbitMQ | `localhost:5672`，管理台 `http://localhost:15672` | `docmind / 12345678` |
| MinIO | API `http://localhost:9000`，控制台 `http://localhost:9001` | `minioadmin / 12345678` |

所有本地服务统一使用开发密码 `12345678`。脚本启动时会通过一次性任务自动创建 `docmind-sources`、`docmind-previews`、`docmind-templates`、`docmind-exports` 四个 MinIO bucket；任务成功后自动删除，不会在 Docker Desktop 留下停止的初始化容器。这些凭据仅限本地开发，禁止用于测试/生产环境。停止但保留数据：`docker compose -f deploy/compose/docker-compose.yml stop`；删除容器和本地卷前请确认数据不再需要。
