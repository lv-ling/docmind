# Document AI 契约

> 导航：[仓库首页](../../../README.md) / [DocMind Document AI](../README.md) / Document AI 契约

`contracts/` 由 DocMind Document AI 自主维护，定义 Document AI 对 Server 暴露的内部 HTTP 边界与模型输出 Schema。Server 只通过版本化 HTTP 契约调用本应用，不引用 Python 源码。

在 `apps/docmind-document-ai` 中执行，契约校验会随 Python 测试一起运行：

```bash
# 运行 Document AI 测试，其中包含 Draft 2020-12 Schema 校验
uv run pytest
```
