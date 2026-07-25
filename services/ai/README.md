# AI 服务

Python + FastAPI AI 编排服务。负责接收已脱敏的中间文档模型，执行 Schema 约束抽取、证据对齐、语义差异分析和评测。通过 LangChain/LangGraph provider adapter 兼容不同供应商和模型，供应商、模型名、API Key 由 API 服务的加密配置提供。

建议内部结构：`app/api`、`app/workflows`、`app/extractor`、`app/diff`、`app/llm`、`app/evaluation`、`tests`。每个工作流必须有严格输入输出 Schema、超时、重试和预算。

本服务不得直接访问业务数据库、原始 DOCX、敏感令牌映射或浏览器。模型响应中的未知敏感令牌或敏感明文必须被拒绝。
