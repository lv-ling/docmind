# DocMind

DocMind 是面向 Word/PDF 文档理解、模板化、填写和对比的 Monorepo。目标是把 DOCX/PDF 原件转换为安全的结构化 JSON、可审核的 HTML 模板和可协作填写的模板实例。

## 仓库分层

```text
apps/web                 Vue 3 Web 应用
services/api             Java/Spring Boot 业务 API、鉴权、任务编排
services/ai              Python/FastAPI 文档解析、抽取与 AI 能力
packages/editor          Web 编辑器与文档模型
packages/contracts       OpenAPI、JSON Schema、事件契约
packages/ui              共享 UI 组件与设计令牌
docs/                    产品、架构、设计和交付文档
deploy/                  Docker、Compose、Kubernetes 和环境配置
scripts/                 本地开发、数据迁移、质量检查脚本
```

当前阶段以 `docs/` 为事实源，各代码目录的 `README.md` 说明本模块的职责与禁止依赖。详细文档入口见 [docs/README.md](docs/README.md)。

v0.1 的开发顺序、分端任务和完成状态见 [开发任务](docs/delivery/02-v0.1开发任务.md)。

## 依赖方向

`apps/web` 只通过契约调用 `services/api`；`services/api` 负责任务编排、数据、权限和敏感信息；`services/ai` 只处理应用层提供的脱敏输入。`packages/*` 可被应用或服务复用，但不能依赖具体应用。

## 工作区命令

JavaScript/TypeScript 工作区由 pnpm 管理，范围为 `apps/*` 和 `packages/*`。根目录统一提供 `dev`、`build`、`lint`、`format`、`typecheck`、`test`、`test:coverage`、`check` 以及 `infra:*` 脚本。质量基线使用 TypeScript 严格模式、ESLint Flat Config、Prettier 和 Vitest。

开发环境固定使用 Node `24.14.1` 和 pnpm `10.13.1`，安装与切换方式见 [Node 与 pnpm 环境](docs/development/02-Node与pnpm环境.md)。
