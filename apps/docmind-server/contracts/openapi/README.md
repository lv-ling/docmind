# OpenAPI

> 导航：[仓库首页](../../../../README.md) / [Server 契约](../README.md) / OpenAPI

[`v1.yaml`](v1.yaml) 是 DocMind HTTP API v1 的目标契约，描述上传、Schema、敏感规则、抽取复核、模板、实例、批注、纠错和权威 Diff。契约覆盖范围不等同于当前代码已全部实现；具体上线能力以对应服务实现、测试和产品版本为准。

在 `apps/docmind-server` 中验证：

```bash
# 验证 OpenAPI 版本、安全配置、本地引用和 operationId 唯一性
./mvnw verify
```

契约测试检查版本、安全配置、本地引用和 `operationId` 唯一性。修改接口时应优先更新本文件中的契约，并同步 Java API、已发布的契约版本和 Web 客户端兼容性说明。
