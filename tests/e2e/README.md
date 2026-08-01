# 跨服务验收测试

> 导航：[仓库首页](../../README.md) / [测试](../README.md) / 跨服务验收

这里存放依赖 API、AI 和本地基础设施协作的端到端验收脚本。模块单元测试和服务集成测试仍与被测代码相邻，不放入本目录。

```text
tests/e2e/
├── template_flow.py            上传、抽取、复核、模板和版本流转验收
├── source_formats.py           DOC、DOCX、PDF 上传与预览验收
└── fixtures/
    └── create_e2e_docx.py      确定性两页敏感 DOCX 生成器
```

## 前置条件

1. 执行 `pnpm infra:start` 并确认 PostgreSQL、Redis、RabbitMQ 和 MinIO 健康。
2. 执行 `pnpm ai:dev` 和 `pnpm api:dev`。
3. 确认 API 使用的本地演示账号和工作区可用；默认值为 `admin@docmind.local`、`docmind-demo`。
4. 文档预览转换需要本机可调用 LibreOffice。

## 执行命令

```bash
# 自动生成 DOCX，验收完整 MVP 流程
pnpm e2e:mvp

# 使用已有样例逐一验证三种首期格式
pnpm e2e:source-formats --doc <fixture.doc> --docx <fixture.docx> --pdf <fixture.pdf>
```

`template_flow.py` 使用确定性 Mock Provider，验证 MinIO 直传与摘要、RabbitMQ 调度、Schema 与敏感规则、令牌化、二次泄漏检查、人工复核、PDF/安全 HTML 模板、后端 Diff、发布和生成新版本式回滚。脚本会把创建的业务记录留在本地开发数据卷中，并在标准输出打印可追踪的资源 ID；生成的临时 DOCX 默认在运行结束后删除。

格式验收脚本会校验原件摘要、真实 MIME、不可变下载、PDF 预览和正整数页数。失败时先根据请求 ID 查看 API、AI 和 RabbitMQ 日志，再确认 LibreOffice 与对象存储状态。
