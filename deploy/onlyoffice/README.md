# ONLYOFFICE 本地资源

> 导航：[仓库首页](../../README.md) / [部署](../README.md) / ONLYOFFICE

本目录存放 ONLYOFFICE 本地开发与验收所需的仓库侧资源。Document Server 本身由 [`deploy/compose/docker-compose.yml`](../compose/docker-compose.yml) 中的可选 `editor` profile 管理。

```text
onlyoffice/
└── fonts/    合法授权字体的本机挂载入口，不提交字体二进制
```

从仓库根目录启动、停止和查看编辑服务：

```bash
./scripts/dev/infra.sh editor-start
./scripts/dev/infra.sh editor-status
./scripts/dev/infra.sh editor-stop
```

字体准备与索引重建见[字体目录说明](fonts/README.md)，完整 Compose 配置见[本地基础设施](../compose/README.md)。
