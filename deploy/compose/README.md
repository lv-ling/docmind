# 本地基础设施

> 导航：[仓库首页](../../README.md) / [部署](../README.md) / Compose

本目录为本地开发提供 PostgreSQL、Redis、RabbitMQ 和 MinIO，并通过可选 `editor` profile 提供 ONLYOFFICE Docs。Compose 镜像使用固定摘要，端口只绑定到 `127.0.0.1`。

## 服务清单

| 服务 | 默认地址 | 用途 |
| --- | --- | --- |
| PostgreSQL | `127.0.0.1:5432` | 业务数据与任务状态 |
| Redis | `127.0.0.1:6379` | 缓存和短期协调状态 |
| RabbitMQ | `127.0.0.1:5672` | 异步任务消息；管理台为 `15672` |
| MinIO | `127.0.0.1:9000` | 原件、预览、模板和导出对象；控制台为 `9001` |
| ONLYOFFICE Docs | `127.0.0.1:8082` | 可选原生 DOCX 编辑 POC |

默认账号、密码和连接命令见[容器访问手册](ACCESS.md)。

## 日常操作

推荐从仓库根目录使用统一脚本；`start` 会等待四项基础服务健康，并以一次性任务幂等创建 MinIO bucket。

```bash
# 启动服务、等待健康并初始化 MinIO bucket
./scripts/dev/infra.sh start

# 查看容器状态
./scripts/dev/infra.sh status

# 持续查看全部基础设施日志
./scripts/dev/infra.sh logs

# 停止容器但保留数据卷
./scripts/dev/infra.sh stop
```

Compose 配置发生变化后执行 `./scripts/dev/infra.sh recreate`，它重建容器并重新确认 bucket，但不会删除数据卷。查看单个服务日志可使用 `./scripts/dev/infra.sh logs <service>`。

## 本地配置

默认值定义在 `docker-compose.yml` 和 [`.env.example`](.env.example) 中。如需覆盖，复制为本目录下的 `.env`：

```bash
# 创建本机私有覆盖文件；该文件不会提交到 Git
cp deploy/compose/.env.example deploy/compose/.env
```

脚本会自动加载该文件。`.env` 已被 Git 忽略；其中的默认密码只适用于单机开发，不能用于共享测试或生产环境。PostgreSQL 和 RabbitMQ 的初始化凭据写入数据卷后，修改 `.env` 不会自动迁移已有账号。

## 可选 ONLYOFFICE

```bash
# 启动并等待 ONLYOFFICE 健康
./scripts/dev/infra.sh editor-start

# 查看 ONLYOFFICE 状态
./scripts/dev/infra.sh editor-status

# 持续查看 ONLYOFFICE 日志
./scripts/dev/infra.sh editor-logs

# 停止 ONLYOFFICE，保留相关数据
./scripts/dev/infra.sh editor-stop
```

首次启动需要下载较大的官方镜像并等待初始化。Document Server 通过 `host.docker.internal:8080` 读取 API 受控文件并回调保存；服务间使用本地示例 JWT，生产环境必须替换。

高保真分页要求编辑器与 PDF 渲染器使用同一组合法字体。将授权字体放入 [`deploy/onlyoffice/fonts`](../onlyoffice/fonts/README.md)，或用 `DOCMIND_ONLYOFFICE_FONT_DIR` 指向本机目录；字体变化后执行 `./scripts/dev/infra.sh editor-fonts` 重建索引。字体缺失或替代必须作为还原度告警处理。

## 数据安全

- `stop` 只停止容器并保留数据卷。
- `recreate` 只重建容器，不删除数据卷。
- `docker compose down -v` 会删除本地数据库、队列和对象数据，执行前必须确认不再需要。
- 本地凭据、ONLYOFFICE JWT 和端口配置不得复用于共享或生产环境。
