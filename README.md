# DocMind

DocMind 是面向 Word、PDF 文档理解、结构化抽取、模板化和版本协作的多语言 Monorepo。系统把 DOC、DOCX、PDF 原件转换为可追溯的结构化数据与受控文档模型，并通过权限、脱敏、人工复核和不可变版本链保护业务文档。

## 核心流程

1. 上传原件并固化不可变版本、摘要与只读预览。
2. 选择字段 Schema 和敏感规则，对文档执行解析、令牌化与结构化抽取。
3. 在权限过滤后的界面中复核候选值、证据和异常字段。
4. 将原件转换为受控模板，完成编辑、后端 Diff、发布和生成新版本式回滚。

## 仓库结构

```text
docmind/
├── apps/
│   └── web/                 Vue 3 用户端
├── services/
│   ├── api/                 Java/Spring Boot 业务 API 与任务编排
│   └── ai/                  Python/FastAPI 解析、脱敏、抽取与评测
├── packages/
│   ├── contracts/           TypeScript、OpenAPI、JSON Schema 与事件契约
│   ├── editor/              受控文档模型、模板绑定与安全 HTML
│   └── ui/                  共享 UI 组件与设计令牌
├── tests/
│   ├── e2e/                 跨服务端到端验收
│   └── tooling/             工作区工具链测试
├── deploy/
│   ├── compose/             本地基础设施与可选 ONLYOFFICE
│   └── onlyoffice/fonts/    本机授权字体挂载入口
├── scripts/
│   ├── dev/                 AI 与基础设施操作脚本
│   └── maintenance/         仓库维护入口
├── docs/                    产品、架构、设计、开发和交付文档
├── package.json             根命令与工具版本
└── pnpm-workspace.yaml      apps/* 与 packages/* 工作区范围
```

## 分层文档导航

- [应用层](apps/README.md)
  - [Web 应用](apps/web/README.md)
- [服务层](services/README.md)
  - [业务 API](services/api/README.md)
  - [AI 服务](services/ai/README.md)
- [共享包](packages/README.md)
  - [跨端契约](packages/contracts/README.md)
  - [编辑器内核](packages/editor/README.md)
  - [UI 组件](packages/ui/README.md)
- [测试](tests/README.md)
  - [跨服务验收测试](tests/e2e/README.md)
  - [工具链测试](tests/tooling/README.md)
- [部署与本地基础设施](deploy/README.md)
  - [Compose 使用说明](deploy/compose/README.md)
  - [容器访问手册](deploy/compose/ACCESS.md)
  - [ONLYOFFICE 资源](deploy/onlyoffice/README.md)
- [工程脚本](scripts/README.md)
  - [开发脚本](scripts/dev/README.md)
  - [维护脚本](scripts/maintenance/README.md)
- [产品与技术文档](docs/README.md)

模块 README 说明与当前代码直接相关的职责、结构、命令和边界；`docs/` 记录产品、架构、设计和交付决策。变更实现时应同步更新相邻模块 README，跨模块决策则在 `docs/` 中维护。

## 依赖边界

- `apps/web` 依赖共享包并只通过 HTTP 契约调用 `services/api`，不能直连数据库、对象存储或 AI 服务。
- `services/api` 负责身份、工作区、数据持久化、敏感映射、审计和异步任务，是业务数据访问边界。
- `services/ai` 只接收 API 明确转交的文件流或令牌化输入，不能自行访问业务数据库或 MinIO。
- `packages/contracts` 是跨端数据结构的事实源；`packages/editor` 和 `packages/ui` 不依赖具体页面或服务实现。

## 本地开发

前置条件：Node `24.14.1`、pnpm `10.13.1`、Java 17、Maven 3.6.3+、Python 3.12、`uv 0.11.29` 和 Docker Desktop。文档转换还需要 LibreOffice；原生 DOCX 编辑 POC 可选用 ONLYOFFICE。

首次准备：

```bash
pnpm install
pnpm ai:sync
pnpm infra:start
```

按需在独立终端启动服务：

```bash
pnpm ai:dev
pnpm api:dev
pnpm --filter @docmind/web dev
```

Web 默认监听 `http://127.0.0.1:5173`，并将 `/api` 代理到 `http://127.0.0.1:8080`；AI 默认监听 `http://127.0.0.1:8090`。本地基础设施账号与端口见[容器访问手册](deploy/compose/ACCESS.md)。

## 验证入口

```bash
# JavaScript/TypeScript、Python 和 Java 的完整质量门禁
pnpm check

# API、AI 和基础设施均运行后执行跨服务验收
pnpm e2e:mvp

# 单独验证 DOC、DOCX、PDF 上传和预览
pnpm e2e:source-formats --doc <fixture.doc> --docx <fixture.docx> --pdf <fixture.pdf>
```

`pnpm e2e:templates` 是 `pnpm e2e:mvp` 的兼容别名。更多命令按职责记录在各模块 README 和根目录 `package.json` 中。
