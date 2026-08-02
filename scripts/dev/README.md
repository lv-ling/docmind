# 开发脚本

> 导航：[仓库首页](../../README.md) / [工程脚本](../README.md) / 开发脚本

以下命令均从仓库根目录执行。脚本只调用应用自己持有的工具链，不在根目录安装依赖或生成锁文件。

## 一键启动

| 命令                          | 作用                                                                    |
| ----------------------------- | ----------------------------------------------------------------------- |
| `./scripts/dev/start.sh`      | 检查本机环境，启动并检查基础设施，再并行启动 Web、Server 和 Document AI |
| `Ctrl+C`                      | 结束本次脚本托管的三个应用进程；基础设施容器继续运行，避免中断本地数据  |
| `./scripts/dev/infra.sh stop` | 开发结束后显式停止基础设施容器，不删除数据卷                            |

`start.sh` 不会自动安装 Web 依赖或同步 AI 虚拟环境。首次使用前分别执行 `web.sh install` 和 `document-ai.sh sync`。任一应用意外退出时，脚本会停止另外两个应用并返回对应状态码。

## Web

| 命令                                 | 作用                                                             |
| ------------------------------------ | ---------------------------------------------------------------- |
| `./scripts/dev/web.sh install`       | 严格按 Web 的 `pnpm-lock.yaml` 安装依赖                          |
| `./scripts/dev/web.sh dev`           | 启动 Vite 开发服务器和热更新，默认地址为 `http://127.0.0.1:5173` |
| `./scripts/dev/web.sh build`         | 执行类型检查并生成生产构建到 Web 的 `dist/`                      |
| `./scripts/dev/web.sh lint`          | 使用 ESLint 检查 Web TypeScript、Vue 和配置文件                  |
| `./scripts/dev/web.sh format`        | 使用 Prettier 改写 Web 内不符合格式规范的文件                    |
| `./scripts/dev/web.sh format-check`  | 仅检查 Prettier 格式，不改写文件                                 |
| `./scripts/dev/web.sh typecheck`     | 使用 `vue-tsc` 执行严格类型检查，不产出文件                      |
| `./scripts/dev/web.sh test`          | 单次运行全部 Vitest 测试                                         |
| `./scripts/dev/web.sh test-watch`    | 以监听模式运行 Vitest，适合本地持续开发                          |
| `./scripts/dev/web.sh test-coverage` | 运行测试并生成覆盖率报告                                         |
| `./scripts/dev/web.sh check`         | 顺序执行 lint、格式检查、类型检查、测试和生产构建                |

依赖、Node 版本、配置和构建产物全部位于 `apps/docmind-web`。

## Server

| 命令                             | 作用                                                                    |
| -------------------------------- | ----------------------------------------------------------------------- |
| `./scripts/dev/server.sh dev`    | 通过 Maven Wrapper 启动 Spring Boot，默认地址为 `http://127.0.0.1:8080` |
| `./scripts/dev/server.sh test`   | 运行 Server 测试，不执行打包阶段                                        |
| `./scripts/dev/server.sh verify` | 运行完整 Maven 验证并生成可执行 JAR                                     |
| `./scripts/dev/server.sh clean`  | 删除 Server 的 Maven `target/` 构建产物                                 |

`server.sh` 固定使用 `apps/docmind-server/mvnw`，本机只需 Java 17；首次运行 Wrapper 会下载固定的 Maven 版本。

## Document AI

| 命令                                        | 作用                                                                |
| ------------------------------------------- | ------------------------------------------------------------------- |
| `./scripts/dev/document-ai.sh sync`         | 严格按 `uv.lock` 创建或同步应用自己的 `.venv`                       |
| `./scripts/dev/document-ai.sh dev`          | 使用 Uvicorn 热重载启动内部服务，默认地址为 `http://127.0.0.1:8090` |
| `./scripts/dev/document-ai.sh lint`         | 使用 Ruff 检查 Python 代码                                          |
| `./scripts/dev/document-ai.sh format`       | 使用 Ruff 改写不符合格式规范的 Python 文件                          |
| `./scripts/dev/document-ai.sh format-check` | 仅检查 Ruff 格式，不改写文件                                        |
| `./scripts/dev/document-ai.sh typecheck`    | 使用 mypy 执行严格类型检查                                          |
| `./scripts/dev/document-ai.sh test`         | 运行全部 pytest 测试                                                |
| `./scripts/dev/document-ai.sh eval`         | 运行确定性抽取评测并输出指标                                        |
| `./scripts/dev/document-ai.sh check`        | 顺序执行 lint、格式检查、类型检查、测试和评测                       |

除 `sync` 外的命令要求 `apps/docmind-document-ai/.venv` 已创建。

## 基础设施

| 命令                                    | 作用                                                             |
| --------------------------------------- | ---------------------------------------------------------------- |
| `./scripts/dev/infra.sh start`          | 启动 PostgreSQL、Redis、RabbitMQ、MinIO，等待健康并初始化 bucket |
| `./scripts/dev/infra.sh stop`           | 停止基础设施容器但保留数据卷                                     |
| `./scripts/dev/infra.sh restart`        | 重启现有基础设施容器并等待恢复健康                               |
| `./scripts/dev/infra.sh recreate`       | 强制重建基础设施容器并重新初始化 bucket，不删除数据卷            |
| `./scripts/dev/infra.sh status`         | 查看基础设施容器状态                                             |
| `./scripts/dev/infra.sh logs`           | 持续显示全部基础设施日志                                         |
| `./scripts/dev/infra.sh logs <service>` | 持续显示指定 Compose 服务日志                                    |
| `./scripts/dev/infra.sh editor-start`   | 启动可选 ONLYOFFICE Docs 并等待健康                              |
| `./scripts/dev/infra.sh editor-stop`    | 停止 ONLYOFFICE Docs                                             |
| `./scripts/dev/infra.sh editor-status`  | 查看 ONLYOFFICE Docs 状态                                        |
| `./scripts/dev/infra.sh editor-fonts`   | 重建 ONLYOFFICE 字体索引并重启服务                               |
| `./scripts/dev/infra.sh editor-logs`    | 持续显示 ONLYOFFICE Docs 日志                                    |

所有停止命令默认保留数据卷。只有明确确认本地数据不再需要时，才应手动执行会删除数据卷的 Compose 操作。

## 黑盒验收与全仓验证

| 命令                                                                 | 作用                                                                         |
| -------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `./scripts/dev/e2e.sh mvp`                                           | 在运行中的真实服务上验收上传、抽取、复核、模板、发布和回滚完整流程           |
| `./scripts/dev/e2e.sh source-formats --doc ... --docx ... --pdf ...` | 使用指定样例验证 DOC、DOCX、PDF 上传、不可变下载和 PDF 预览                  |
| `./scripts/dev/check.sh`                                             | 顺序执行 Web、Document AI 和 Server 各自的完整质量门禁，不启动基础设施或 E2E |

E2E 脚本只使用 Python 3 标准库，但要求基础设施、Server 和 Document AI 已经运行。
