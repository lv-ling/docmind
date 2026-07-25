# 部署目录

存放本地 Compose、Kubernetes、网关、可观测性和环境配置样例。真实密钥不得提交仓库；环境差异通过配置和 Secret 注入，不复制业务代码。API、AI 和 Web 的 Dockerfile 将在对应服务有可运行代码时添加，当前 Compose 只负责基础设施。

建议内部结构：`compose`、`k8s`、`observability`、`env`。本地基础设施的启动方式见 [compose/README.md](compose/README.md)。
