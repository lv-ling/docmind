# 测试目录

> 导航：[仓库首页](../README.md) / 测试

根目录 `tests/` 只存放跨应用黑盒验收。单元测试、契约测试和服务集成测试必须与被测应用相邻，并由各应用自己的工具链执行。

```text
tests/
└── e2e/    依赖 Server、Document AI 和本地基础设施的端到端验收
```

E2E 入口只使用 Python 3 标准库，不需要根目录 `package.json`、Python 虚拟环境或第三方测试框架。执行说明见[跨服务验收](e2e/README.md)。
