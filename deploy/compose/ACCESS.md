# 容器访问手册

使用前确认 Docker Desktop 已运行，并在项目根目录启动服务：

```bash
./scripts/dev/infra.sh start
./scripts/dev/infra.sh status
```

## PostgreSQL

连接参数：

```text
Host: 127.0.0.1
Port: 5432
Database: docmind
Username: docmind
Password: 12345678
```

进入交互终端：

```bash
docker exec -it docmind-postgres psql -U docmind -d docmind
```

验证连接：

```sql
SELECT current_database(), current_user, version();
```

常用命令：`\d` 查看当前数据库，`\dt` 查看表，`\q` 退出。也可以使用 DBeaver、DataGrip、TablePlus 或 pgAdmin 连接。

## Redis

连接参数：

```text
Host: 127.0.0.1
Port: 6379
Password: 12345678
```

进入客户端并验证：

```bash
docker exec -it docmind-redis redis-cli -a 12345678
PING
```

返回 `PONG` 表示连接成功。应用连接地址为 `redis://:12345678@127.0.0.1:6379/0`。

## RabbitMQ

连接参数：

```text
Host: 127.0.0.1
AMQP Port: 5672
Management Port: 15672
Username: docmind
Password: 12345678
```

浏览器管理台：<http://localhost:15672>

命令行检查：

```bash
docker exec docmind-rabbitmq rabbitmq-diagnostics -q ping
docker exec docmind-rabbitmq rabbitmqctl list_queues
```

应用连接地址为 `amqp://docmind:12345678@127.0.0.1:5672/`。

## MinIO

连接参数：

```text
API: http://127.0.0.1:9000
Console: http://127.0.0.1:9001
Access Key: minioadmin
Secret Key: 12345678
```

浏览器控制台：<http://localhost:9001>

查看 bucket：

```bash
docker exec docmind-minio mc alias set local http://127.0.0.1:9000 minioadmin 12345678
docker exec docmind-minio mc ls local
```

应看到 `docmind-sources`、`docmind-previews`、`docmind-templates`、`docmind-exports`。

## 日志与故障检查

```bash
# 全部服务日志
./scripts/dev/infra.sh logs

# 单个服务日志
./scripts/dev/infra.sh logs postgres
./scripts/dev/infra.sh logs redis
./scripts/dev/infra.sh logs rabbitmq
./scripts/dev/infra.sh logs minio
```

这些账号密码仅供本地开发。端口只绑定 `127.0.0.1`，不能从其他电脑直接访问。不要使用 `docker compose down -v`，否则会删除本地数据卷。

