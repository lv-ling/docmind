# 测试目录

> 导航：[仓库首页](../README.md) / 测试

`tests/` 只存放跨模块验收和工作区级工具测试。模块单元测试、服务集成测试应与被测代码相邻，分别保留在 `apps/*`、`packages/*` 或 `services/*` 内。

```text
tests/
├── e2e/        依赖 API、AI 和本地基础设施的端到端验收
└── tooling/    根工作区配置与工具链约束测试
```

- [跨服务验收测试](e2e/README.md)
- [工具链测试](tooling/README.md)

完整质量门禁使用 `pnpm check`；真实依赖验收需先启动基础设施、AI 和 API。
