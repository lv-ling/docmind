# DocMind

DocMind 是面向 Word、PDF 文档理解、结构化抽取、模板化和版本协作的多语言仓库。三个应用独立管理依赖和构建工具，根目录只保留文档、部署、黑盒 E2E 与无第三方依赖的编排脚本。

## 核心流程

1. 上传原件并固化不可变版本、摘要与只读预览。
2. 选择字段 Schema 和敏感规则，对文档执行解析、令牌化与结构化抽取。
3. 在权限过滤后的界面中复核候选值、证据和异常字段。
4. 将原件转换为受控模板，完成编辑、后端 Diff、发布和生成新版本式回滚。

## 仓库结构

```text
docmind/
├── apps/
│   ├── docmind-web/          Vue 3 用户端；独立 package.json 与 pnpm-lock.yaml
│   ├── docmind-server/       Java/Spring Boot 后端；独立 Maven Wrapper
│   └── docmind-document-ai/  Python/FastAPI 文档智能服务；独立 uv.lock
├── tests/
│   └── e2e/                  仅使用 Python 标准库的跨服务黑盒验收
├── deploy/
│   ├── compose/              本地基础设施与可选 ONLYOFFICE
│   └── onlyoffice/fonts/     本机授权字体挂载入口
├── scripts/
│   ├── dev/                  无第三方运行时依赖的开发编排脚本
│   └── maintenance/          仓库维护入口
└── docs/                     产品、架构、设计、开发和交付文档
```

根目录没有 `package.json`、Node workspace 或统一依赖锁。进入任一应用即可独立安装、构建和测试，也可以通过根目录 Shell 脚本从任意工作目录调用常用命令。

## 文档导航

- [应用层](apps/README.md)
  - [DocMind Web](apps/docmind-web/README.md)
  - [DocMind Server](apps/docmind-server/README.md)
  - [DocMind Document AI](apps/docmind-document-ai/README.md)
- [测试](tests/README.md)与[跨服务验收](tests/e2e/README.md)
- [部署与本地基础设施](deploy/README.md)
- [工程脚本](scripts/README.md)
- [产品与技术文档](docs/README.md)

## 应用边界

- `docmind-web` 持有自己的 TypeScript 契约类型、受控编辑器代码和 UI 基础组件，只通过 HTTP 调用 Server。
- `docmind-server` 持有业务 API、任务编排、数据访问以及对外 OpenAPI、事件和业务 JSON Schema。
- `docmind-document-ai` 持有解析、脱敏、抽取、评测以及模型输出 JSON Schema，不能自行访问业务数据库或对象存储。
- 应用之间不通过源码包、workspace 链接或跨目录构建引用共享实现；跨应用兼容性由版本化接口契约和黑盒 E2E 验证。

## 本地开发

前置条件：Web 使用 Node `24.14.1` 与 pnpm `10.13.1`；Server 使用 Java 17；Document AI 使用 Python 3.12 与 `uv 0.11.29`；本地基础设施需要 Docker Desktop。文档转换还需要 LibreOffice，可选原生 DOCX 编辑需要 ONLYOFFICE。

首次准备：

```bash
# 严格按 Web 锁文件安装 Node 依赖
./scripts/dev/web.sh install

# 严格按 uv.lock 创建 Document AI 虚拟环境
./scripts/dev/document-ai.sh sync
```

准备完成后可一键启动基础设施和三个应用：

```bash
# 启动基础设施、Web、Server 和 Document AI；按 Ctrl+C 停止三个应用
./scripts/dev/start.sh
```

`Ctrl+C` 不会停止基础设施容器。开发结束后可执行 `./scripts/dev/infra.sh stop`，它会保留数据库和对象存储数据。需要分别观察或调试进程时，也可以在独立终端启动：

```bash
# 启动 Web 开发服务器和热更新
./scripts/dev/web.sh dev

# 启动 Spring Boot Server
./scripts/dev/server.sh dev

# 启动 Uvicorn Document AI 服务和热重载
./scripts/dev/document-ai.sh dev
```

Web 默认监听 `http://127.0.0.1:5173` 并把 `/api` 代理到 `http://127.0.0.1:8080`；Document AI 默认监听 `http://127.0.0.1:8090`。基础设施账号与端口见[容器访问手册](deploy/compose/ACCESS.md)。

## 验证入口

```bash
# 依次执行 Web、Document AI 和 Server 的独立质量门禁
./scripts/dev/check.sh

# API、AI 和基础设施均运行后执行完整黑盒验收
./scripts/dev/e2e.sh mvp

# 单独验证 DOC、DOCX、PDF 上传和预览
./scripts/dev/e2e.sh source-formats --doc <fixture.doc> --docx <fixture.docx> --pdf <fixture.pdf>
```

更细的安装与命令说明由各应用自己的 README 维护。
