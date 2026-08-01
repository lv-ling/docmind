# 工程脚本

> 导航：[仓库首页](../README.md) / 工程脚本

`scripts/` 存放本地开发和仓库维护入口。脚本应幂等、从仓库根目录定位资源、显式接收环境参数，并避免把文档原文、敏感映射或密钥写入日志。跨服务验收测试统一放在 [`tests/e2e`](../tests/e2e/README.md)。

```text
scripts/
├── dev/
│   ├── ai.sh             AI 环境、服务与质量命令
│   ├── infra.sh          Compose 基础设施和 ONLYOFFICE 操作
│   └── README.md         开发脚本完整命令
└── maintenance/
    └── README.md         维护脚本准入与安全边界
```

## 入口导航

- [开发脚本](dev/README.md)：AI 同步、开发服务、检查，以及基础设施启停、日志和编辑服务操作。
- [维护脚本](maintenance/README.md)：仓库清理和一致性检查脚本的放置规则；当前没有自动清理实现。

根目录 `package.json` 为常用操作提供 `ai:*`、`infra:*`、`api:*` 和 `e2e:*` 别名。README 应说明“何时使用”，脚本自身的 `usage` 输出负责列出可执行参数，两者需要保持同步。
