# 跨服务验收测试

> 导航：[仓库首页](../../README.md) / [测试](../README.md) / 跨服务验收

这里存放依赖 Server、Document AI 和本地基础设施协作的黑盒端到端验收脚本。脚本只使用 Python 3 标准库，应用内部测试不放入本目录。

```text
tests/e2e/
├── template_flow.py            上传、抽取、复核、模板和版本流转验收
├── source_formats.py           DOC、DOCX、PDF 上传与预览验收
└── fixtures/
    └── create_e2e_docx.py      确定性两页敏感 DOCX 生成器
```

## 前置条件

1. 完成 `./scripts/dev/web.sh install` 和 `./scripts/dev/document-ai.sh sync` 的首次环境准备。
2. 在一个终端执行 `./scripts/dev/start.sh`，启动基础设施、Web、Server 和 Document AI。
3. 确认本地演示账号和工作区可用；默认值为 `admin@docmind.local`、`docmind-demo`。
4. 文档预览转换需要本机可调用 LibreOffice。

## 执行命令

```bash
# 自动生成 DOCX，验收完整 MVP 流程
./scripts/dev/e2e.sh mvp

# 使用已有样例逐一验证三种首期格式
./scripts/dev/e2e.sh source-formats --doc <fixture.doc> --docx <fixture.docx> --pdf <fixture.pdf>
```

`template_flow.py` 使用确定性 Mock Provider，验证 MinIO 直传与摘要、RabbitMQ 调度、Schema 与敏感规则、令牌化、二次泄漏检查、人工复核、PDF/安全 HTML 模板、后端 Diff、发布和生成新版本式回滚。脚本会把创建的业务记录留在本地开发数据卷中，并打印可追踪的资源 ID；生成的临时 DOCX 默认在运行结束后删除。

格式验收脚本会校验原件摘要、真实 MIME、不可变下载、PDF 预览和正整数页数。失败时先根据请求 ID 查看 Server、Document AI 和 RabbitMQ 日志，再确认 LibreOffice 与对象存储状态。
