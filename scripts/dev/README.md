# 开发脚本

> 导航：[仓库首页](../../README.md) / [工程脚本](../README.md) / 开发脚本

本目录统一封装本地 AI 环境和 Docker Compose 操作。脚本可从任意工作目录调用，但以下示例均从仓库根目录执行。

## AI 服务脚本

`ai.sh` 使用 `uv.lock` 管理 `services/ai/.venv`，并确保所有质量命令使用同一虚拟环境。

```bash
pnpm ai:sync           # 按锁文件创建或同步环境
pnpm ai:dev            # 在 127.0.0.1:8090 启动开发服务
pnpm ai:lint
pnpm ai:format:check
pnpm ai:typecheck
pnpm ai:test
pnpm ai:eval
pnpm ai:check          # lint、格式、类型、测试和评测
```

需要修改 Python 格式时可使用 `pnpm ai:format`。除 `sync` 外的命令要求先完成 `pnpm ai:sync`。

## 基础设施脚本

电脑重启后先启动 Docker Desktop，再执行：

```bash
pnpm infra:start       # 启动基础服务、等待健康并创建 MinIO bucket
pnpm infra:status      # 查看容器状态
pnpm infra:restart     # 重启已有容器
pnpm infra:recreate    # 配置变化后重建容器，不删除数据卷
pnpm infra:stop        # 停止容器，保留数据卷
pnpm infra:logs        # 持续查看全部日志
```

需要指定服务或操作可选编辑器时直接调用脚本：

```bash
./scripts/dev/infra.sh logs minio
./scripts/dev/infra.sh editor-start
./scripts/dev/infra.sh editor-status
./scripts/dev/infra.sh editor-fonts
./scripts/dev/infra.sh editor-logs
./scripts/dev/infra.sh editor-stop
```

端口、默认账号和 `.env` 覆盖方式见[本地基础设施](../../deploy/compose/README.md)。不要使用 `docker compose down -v`，除非已明确确认可以删除 PostgreSQL、Redis、RabbitMQ、MinIO 和 ONLYOFFICE 的本地数据卷。
