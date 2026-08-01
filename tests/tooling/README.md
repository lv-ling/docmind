# 工具链测试

> 导航：[仓库首页](../../README.md) / [测试](../README.md) / 工具链测试

这里验证 Monorepo 的根配置和工程约束，当前包含 `workspace.test.ts`。业务行为测试不放在此处：模块级测试与源码相邻，跨服务流程放在 [`tests/e2e`](../e2e/README.md)。

从仓库根目录执行：

```bash
pnpm test
```
