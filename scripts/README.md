# 脚本目录

存放本地开发、数据库迁移辅助、契约生成、质量检查、脱敏测试集和评测任务入口。脚本应幂等、显式接收环境参数，并避免把文档原文或敏感映射写入日志。

建议内部结构：`dev`、`migration`、`quality`、`evaluation`。

## 本地基础设施

电脑重启后，先启动 Docker Desktop；等待 Docker 可用，然后在项目根目录执行：

```bash
./scripts/dev/infra.sh start
```

脚本会启动 PostgreSQL、Redis、RabbitMQ、MinIO，等待健康检查通过，并通过一次性任务确保 MinIO bucket 已创建。

### 常用命令

```bash
# 启动并等待服务健康
./scripts/dev/infra.sh start

# 查看容器状态
./scripts/dev/infra.sh status

# 停止容器但保留数据卷
./scripts/dev/infra.sh stop

# 重启已有容器
./scripts/dev/infra.sh restart

# Compose 配置变更后强制重建容器，不删除数据卷
./scripts/dev/infra.sh recreate

# 查看全部日志
./scripts/dev/infra.sh logs

# 查看单个服务日志，例如 MinIO
./scripts/dev/infra.sh logs minio
```

### 操作顺序

1. 启动 Docker Desktop。
2. 进入项目目录：`cd /Users/lvling/learn/docmind`。
3. 执行 `./scripts/dev/infra.sh start`。
4. 执行 `./scripts/dev/infra.sh status`，确认四个服务均为 `healthy`。
5. 开发结束后可执行 `./scripts/dev/infra.sh stop`。

不要执行 `docker compose down -v`，除非明确需要删除 PostgreSQL、Redis、RabbitMQ 和 MinIO 的本地数据。完整账号、端口和启动流程见 [本地基础设施文档](../docs/development/01-本地基础设施.md)，具体连接命令见 [容器访问手册](../deploy/compose/ACCESS.md)。
