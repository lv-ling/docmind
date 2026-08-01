# AI 服务

Python 3.12 + FastAPI AI 编排服务。负责确定性文档解析、敏感检测与令牌化、Schema 约束抽取、证据对齐、模板转换辅助和评测。通过 Provider Adapter 兼容不同模型供应商，供应商、模型名和 API Key 由 Java API 的受控配置提供。

## 安全边界

- 只能通过带内部服务令牌的接口接收 Java API 明确转交的受控文件流或令牌化文档，不能自行访问业务数据库、MinIO 或浏览器。
- 原始文件只进入 Parser/Tokenizer 工作流；LLM Provider 只能接收令牌化文本、字段 Schema 和必要版面信息。
- 敏感令牌到原文的映射始终由 Java 应用层加密持有，不进入本服务或模型调用。
- 日志、错误和指标不得记录请求体、文档文本、字段值、模型原始输出、内部令牌或密钥。

## 开发

前置条件：Python 3.12 和 `uv 0.11.29`。推荐从仓库根目录使用统一脚本：

```bash
pnpm run ai:sync
pnpm run ai:check
pnpm run ai:dev
```

`ai:sync` 严格按 `uv.lock` 创建 `services/ai/.venv`；`ai:check` 依次执行 Ruff、格式检查、严格 mypy 和 pytest。需要更新依赖时，先在 `services/ai` 执行 `uv lock`，再提交更新后的锁文件。

公开探针为 `GET /health/live` 和 `GET /health/ready`。除探针外的 `/internal/v1/**` 接口必须提供 `X-DocMind-Internal-Token`。调用方传入合法 UUID `X-Request-ID` 时原样透传，否则服务生成新的 UUID。
