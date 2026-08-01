# 本地基础设施

启动：

```bash
docker compose -f deploy/compose/docker-compose.yml up -d
```

日常操作也可以使用项目脚本：`./scripts/dev/infra.sh start`、`stop`、`restart`、`status`。

原生 DOCX 编辑 POC 使用可选的 ONLYOFFICE Docs 9.4.0，不随基础依赖自动启动：

```bash
./scripts/dev/infra.sh editor-start
```

首次启动需要下载约 1.2 GB 的官方镜像并等待编辑服务初始化。状态、日志和停止命令分别是 `editor-status`、`editor-logs`、`editor-stop`。浏览器访问地址默认为 `http://127.0.0.1:8082`；Document Server 通过 `host.docker.internal:8080` 读取 API 受控文件并回调保存。Apple Silicon 和 x86_64 使用同一个固定的多架构镜像摘要。

高保真分页依赖编辑器和渲染器使用同一组合法字体。仓库不提交字体二进制；请将获授权字体放入 `deploy/onlyoffice/fonts`，或在 `deploy/compose/.env` 通过 `DOCMIND_ONLYOFFICE_FONT_DIR` 指向本机合法字体目录。变更字体后执行 `./scripts/dev/infra.sh editor-fonts` 重建字体索引并重启编辑服务。缺少文档要求的字体时必须视为还原度告警，不能静默通过 G0。

每个容器的浏览器、命令行和应用连接方式见 [容器访问手册](ACCESS.md)。

需要应用 Compose 配置变更时使用 `./scripts/dev/infra.sh recreate`，它只重建容器，不删除数据卷。复制 `.env.example` 为 `.env` 可以覆盖本地端口或密码；PostgreSQL/RabbitMQ 的初始化密码在已有数据卷上不会自动修改。

服务地址：

| 服务 | 地址 | 开发账号 |
| --- | --- | --- |
| PostgreSQL | `localhost:5432` | `docmind / 12345678`，数据库 `docmind` |
| Redis | `localhost:6379` | 密码 `12345678` |
| RabbitMQ | `localhost:5672`，管理台 `http://localhost:15672` | `docmind / 12345678` |
| MinIO | API `http://localhost:9000`，控制台 `http://localhost:9001` | `minioadmin / 12345678` |
| ONLYOFFICE Docs（可选） | `http://127.0.0.1:8082` | JWT 服务间认证，无交互账号 |

所有本地服务统一使用开发密码 `12345678`。脚本启动时会通过一次性任务自动创建 `docmind-sources`、`docmind-previews`、`docmind-templates`、`docmind-exports` 四个 MinIO bucket；任务成功后自动删除，不会在 Docker Desktop 留下停止的初始化容器。这些凭据与仓库中的 ONLYOFFICE JWT 示例密钥仅限本地开发，禁止用于共享测试或生产环境。停止但保留数据：`docker compose -f deploy/compose/docker-compose.yml stop`；删除容器和本地卷前请确认数据不再需要。
