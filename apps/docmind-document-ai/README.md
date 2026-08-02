# DocMind Document AI

> 导航：[仓库首页](../../README.md) / [应用层](../README.md) / DocMind Document AI

`docmind-document-ai` 是 Python 3.12 + FastAPI 内部服务，负责确定性文档解析、敏感检测与令牌化、Schema 约束抽取、证据对齐、受控文档模型生成辅助和评测。

当前运行时固定装配确定性的 `MockExtractionProvider`，用于本地开发和端到端验收；代码已提供 LangChain Chat Model 适配器，但尚未在应用配置中接入真实供应商。接入生产模型时必须补充显式 Provider 配置、密钥注入、超时、审计和安全评测，不能仅替换 Mock 实例。

## 目录结构

```text
apps/docmind-document-ai/
├── contracts/       Document AI 对外模型输出 JSON Schema
├── src/docmind_ai/
│   ├── api/          健康检查、能力、解析、脱敏和抽取接口
│   ├── contracts/    Python 请求与响应模型
│   ├── parsing/      DOC、DOCX、PDF 解析与 OOXML 安全检查
│   ├── pii/          敏感检测、规范化、校验和令牌化
│   ├── extraction/   工作流、输出校验与 Provider Adapter
│   └── evaluation/   确定性评测入口
├── tests/            单元和服务契约测试
├── evals/            评测样例
├── pyproject.toml    依赖与质量配置
└── uv.lock           锁定依赖
```

## 安全边界

- 只能通过带内部服务令牌的接口接收 Java API 明确转交的受控文件流或令牌化文档，不能自行访问业务数据库、MinIO 或浏览器。
- 原始文件只进入解析与令牌化工作流；模型 Provider 只能接收令牌化文本、字段 Schema 和必要版面信息。
- 敏感令牌到原文的映射始终由 Java 应用层加密持有，不进入本服务或模型调用。
- 日志、错误和指标不得记录请求体、文档文本、字段值、模型原始输出、内部令牌或密钥。

公开探针为 `GET /health/live` 和 `GET /health/ready`。`/internal/v1/**` 接口覆盖能力查询、文档解析、敏感令牌化和抽取，必须提供 `X-DocMind-Internal-Token`。合法 UUID `X-Request-ID` 会原样透传，否则服务生成新的 UUID。

## 开发与配置

前置条件：Python 3.12、`uv 0.11.29`，文档格式转换还需要 LibreOffice。在本目录执行：

```bash
# 严格按 uv.lock 创建或同步 .venv
uv sync --frozen

# 启动 Uvicorn 开发服务和热重载
uv run uvicorn docmind_ai.app:create_app --factory --reload --host 127.0.0.1 --port 8090

# 检查 Python 代码规范
uv run ruff check src tests

# 执行严格类型检查
uv run mypy

# 运行全部 pytest 测试
uv run pytest
```

也可从仓库任意目录使用 `./scripts/dev/document-ai.sh sync|dev|check`。`sync` 严格按本应用的 `uv.lock` 创建 `.venv`；`check` 依次执行 Ruff、格式检查、严格 mypy、pytest 和确定性评测。需要更新依赖时在本目录执行 `uv lock` 并提交锁文件。

本地环境变量示例见 [`.env.example`](.env.example)。生产环境必须覆盖内部服务令牌、限制 API 文档暴露，并按运行环境设置文档大小、页数、超时和 Token 预算。
