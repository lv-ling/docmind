# 工程脚本

> 导航：[仓库首页](../README.md) / 工程脚本

`scripts/` 存放无第三方运行时依赖的 Shell 编排入口。它们只定位应用目录并调用应用自己的 pnpm、Maven Wrapper 或 uv 环境，不在根目录安装依赖或形成统一构建系统。

```text
scripts/
├── dev/
│   ├── start.sh         一键启动基础设施与三个应用
│   ├── web.sh           Web 安装、开发与质量命令
│   ├── server.sh        Server 开发与 Maven 门禁
│   ├── document-ai.sh   Document AI 环境、服务与质量命令
│   ├── infra.sh         Compose 与 ONLYOFFICE 操作
│   ├── e2e.sh           Python 标准库黑盒验收
│   └── check.sh         顺序调用三个应用各自的完整门禁
└── maintenance/
    └── README.md        维护脚本准入与安全边界
```

脚本可从任意工作目录调用，不生成根目录锁文件，也不允许应用通过脚本产生源码级依赖。

- [开发脚本](dev/README.md)
- [维护脚本](maintenance/README.md)
