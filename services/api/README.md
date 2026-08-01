# 业务 API

Java 17 + Spring Boot 3 业务服务。负责工作区和权限、原件/模板/实例生命周期、上传会话、敏感信息令牌化与回填、异步任务编排、编辑租约、审计和对外 API。

## 本地运行

前置条件：Java 17、Maven 3.6.3+、Docker 和 pnpm。先从仓库根目录启动依赖及初始化 MinIO bucket：

```bash
pnpm infra:start
pnpm api:dev
```

本地 profile 默认连接以下服务：

- PostgreSQL：`localhost:5432/docmind`
- Redis：`localhost:6379`
- RabbitMQ：`localhost:5672`
- MinIO：`http://localhost:9000`

默认值只用于本地开发，可通过 `DOCMIND_DATABASE_*`、`DOCMIND_REDIS_*`、`DOCMIND_RABBITMQ_*`、`DOCMIND_MINIO_*` 环境变量覆盖。生产环境必须显式注入凭据，不得沿用默认密码。

本地 profile 默认创建演示账号 `admin@docmind.local` / `DocMind123!` 和 `docmind-demo` 工作区。可通过 `DOCMIND_BOOTSTRAP_*` 覆盖，或设置 `DOCMIND_BOOTSTRAP_ENABLED=false` 禁用。该初始化器只在显式开启时运行，已有账号的密码不会被重置。JWT 签名密钥由 `DOCMIND_AUTH_SECRET` 提供，生产值至少 32 字节且必须使用密钥管理系统注入。

## 身份与工作区 API

- `POST /api/v1/auth/login`：使用邮箱和密码换取 30 分钟短时效 Bearer JWT
- `GET /api/v1/me`：读取当前用户
- `GET /api/v1/workspaces`：列出当前用户的有效工作区
- `POST /api/v1/workspaces`：创建工作区；必须提供 `Idempotency-Key`，重放返回同一对象
- `GET /api/v1/workspaces/{id}/members`：Owner/Admin 查询成员；其他角色返回统一 403

密码使用 Spring Security 的委托式单向编码器保存；未知邮箱、错误密码和禁用账号返回相同安全错误。JWT 只保存用户 UUID、签发方、受众和有效期，不放邮箱或工作区角色。角色由每次对象访问时查询工作区成员关系，避免令牌中的旧权限继续生效。

## 原件与上传 API

- `GET/POST /api/v1/workspaces/{id}/sources`：分页查询原件或创建 15 分钟上传会话
- `GET /api/v1/sources/{id}`：查询原件与倒序不可变版本历史
- `POST /api/v1/sources/{id}/versions`：为已有原件创建下一版本上传会话
- `POST /api/v1/source-versions/{id}/complete`：验证对象大小、ETag、真实格式和服务端 SHA-256 后完成版本
- `GET /api/v1/source-versions/{id}/content`：同源鉴权读取不可变原件
- `GET /api/v1/source-versions/{id}/preview`：读取预览排队/处理/就绪状态及只读 URL

创建和完成接口均要求 `Idempotency-Key`。浏览器声明的 MIME、大小、SHA 和 ETag 都不是信任来源：服务会从 MinIO 重新读取对象，DOC 校验 OLE `WordDocument` 流，DOCX 校验 OOXML 必需条目并限制解压规模，PDF 校验文件头和 EOF。验证通过后，服务使用 staging ETag 作为复制前置条件，将内容复制到新的 immutable key，并在数据库提交后清理 staging；即使旧预签名 PUT 尚未过期，也不能改写已确认版本。上传 URL 进入完成/过期等终态后返回 `null`。后台任务每分钟以数据库锁分批过期遗留上传会话并清理 staging，可用 `docmind.storage.upload-cleanup-*` 调整或关闭。

派生预览由独立 `SOURCE_PREVIEW` 异步任务生成；DOC/DOCX 经 LibreOffice 转换，PDF 进行受控复制，三种格式都用 PDFBox 校验真实 PDF 并记录页数。预览和模板资源始终通过同源鉴权 API 读取，不向 Web 暴露 MinIO bucket 或对象键。

## Schema 与敏感规则模板 API

- `GET/POST /api/v1/workspaces/{id}/schemas`：查询 Schema 或创建 Schema 与首个已发布版本
- `GET /api/v1/schemas/{id}`：查询当前版本和倒序不可变版本历史
- `POST /api/v1/schemas/{id}/versions`：创建并发布下一版本，上一版本转为 `superseded`
- `GET/POST /api/v1/workspaces/{id}/schema-templates`：查询模板或把任一已发布过的 Schema 版本保存为可复用指针
- `GET/POST /api/v1/workspaces/{id}/sensitive-rule-templates`：查询或创建敏感规则模板
- `GET /api/v1/sensitive-rule-templates/{id}`：查询敏感规则模板及版本历史
- `POST /api/v1/sensitive-rule-templates/{id}/versions`：校验规则并发布不可变新版本

所有创建接口都要求 `Idempotency-Key`，幂等作用域按具体资源操作隔离；同一键和同一请求重放返回 `200`，同一键复用于不同请求返回 `409 IDEMPOTENCY_CONFLICT`。写操作要求 Owner、Admin 或 Editor，Viewer/Reviewer 可读取但不能修改。

Schema 字段强制校验唯一 `key/json_path/position`、连续排序、类型、数组项类型、约束适用范围、示例、默认值和显示配置，并生成 Draft 2020-12 JSON Schema。默认值只接受 `none` 或不执行的 `literal`：空字符串是合法字符串字面量；显式 `null` 仅在字段允许 `nullable` 时合法；`none` 表示后续抽取找不到值时返回 `null`。字段 `pattern` 与自定义敏感正则都使用 RE2/J 编译，避免回溯型 ReDoS。

敏感规则支持 Presidio、RE2 正则、受限词典和内置校验器引用。词典最多 10,000 项、总计 1,000,000 字符；校验器名称来自服务端 allowlist，不能由用户输入动态加载代码。审计只记录版本号和字段/规则数量，不记录正则、词典内容或默认值。

## 抽取任务 API

- `POST /api/v1/source-versions/{id}/extractions`：选择已发布过的 Schema 版本和可选敏感规则版本，幂等创建异步抽取任务
- `GET /api/v1/extractions/{id}`：按对象级权限查询任务状态和权限过滤后的结果

创建响应同时返回 `job_id`、`extraction_id` 和 `request_id`，调用方使用 `extraction_id` 查询业务结果、使用 `job_id` 关联异步任务。RabbitMQ Worker 已注册抽取处理器，按固定顺序读取并复核不可变原件摘要、调用 Python 解析/令牌化/抽取接口、独立校验输出，再事务性保存结果。AI 调用具备连接/读取超时、连续失败熔断及 408/429/5xx/网络错误重试；4xx 契约错误直接终止，任务回调与抽取运行状态保持一致。

任务、运行、字段结果、候选值和证据分别持久化。敏感令牌映射按抽取任务隔离保存；结果数据、映射、字段值、候选值和证据原文都使用“随机数据密钥 AES-256-GCM + 主密钥封装数据密钥”的加密信封，数据库只额外保存不含完整明文的掩码预览。模型输出会再次校验任务身份、字段集合、`data/fields` 一致性、证据节点与页码、未知/损坏令牌、已知原文泄漏及 Draft 2020-12 JSON Schema；回填只接受任务映射中的完整令牌。多个候选值、最终 Schema 错误或同一规范化敏感值存在不同原文格式时，字段自动进入人工复核。Web 契约只返回权限过滤后的 `visible` 或 `masked` 展示值，不能直接序列化内部实体。创建任务要求 Owner、Admin 或 Editor，Viewer/Reviewer 可读取同工作区任务但不能创建。

本地 AI 服务默认地址为 `http://127.0.0.1:8090`，使用 `DOCMIND_AI_BASE_URL`、`DOCMIND_AI_INTERNAL_TOKEN`、`DOCMIND_AI_CONNECT_TIMEOUT`、`DOCMIND_AI_READ_TIMEOUT` 和 `DOCMIND_AI_CIRCUIT_*` 覆盖。`DOCMIND_CRYPTO_MASTER_KEY_BASE64` 必须是 Base64 编码的 32 字节主密钥；仓库默认值仅用于本地开发，生产环境必须由 KMS/Secret 注入并管理 `DOCMIND_CRYPTO_KEY_ID` 的轮换。

内部 AI 客户端固定使用 HTTP/1.1，避免明文 HTTP 下的 h2c 升级破坏 Uvicorn 对 multipart 文档请求的读取。不要移除该限制，除非 AI 服务入口已经通过受测的 HTTP/2 反向代理提供服务。

## 文档模板 API

- `GET /api/v1/workspaces/{id}/templates`：查询工作区模板和转换状态
- `POST /api/v1/source-versions/{id}/templates`：幂等创建模板转换任务
- `GET /api/v1/templates/{id}`：读取当前版本、不可变版本历史、资源、告警和后端 Diff
- `POST /api/v1/templates/{id}/versions`：提交受控文档模型并保存下一版本
- `POST /api/v1/templates/{id}/versions/{versionId}/publish`：发布当前已校验版本
- `POST /api/v1/templates/{id}/rollback`：基于旧版本创建并发布一个新的恢复版本
- `GET /api/v1/template-resources/{id}/content`：鉴权读取模板资源

转换 Worker 会复核不可变原件摘要，为 DOC/DOCX 调用隔离的 LibreOffice headless 生成 PDF，为 PDF 保留只读副本，再调用 Python 解析统一受控文档模型。Java 端独立校验模型版本、节点白名单、稳定 ID、深度、节点数和体积，服务端重新生成并加密保存 HTML；Web 不能上传或保存任意 HTML。图片等资源按内容摘要复核后进入 templates bucket，转换不完整的版式以结构化告警保存，阻断级告警会禁止发布。

本地模板预览默认调用 `soffice`，可通过 `DOCMIND_LIBREOFFICE_COMMAND` 指定绝对路径，并通过 `DOCMIND_TEMPLATE_PREVIEW_TIMEOUT` 调整超时。运行环境必须安装覆盖目标语言的字体；中文部署至少提供可被 Fontconfig/LibreOffice 发现的 CJK 字体，否则 PDF 会出现缺字方框。

手工编辑只提交受控文档模型和基版本 ID。每次保存都会创建不可变版本，并由后端计算结构 Diff；发布不会改写原件。回滚也不覆盖历史版本，而是复制目标模型、生成新版本并发布，保证审计链连续。

## 异步任务编排

工作队列使用 `docmind.jobs.v1` Direct Exchange、`docmind.jobs.execute.v1` 持久队列和 `docmind.jobs.dead-letter.v1` 死信队列。任务命令只包含任务、工作区、聚合对象和请求关联 ID，不包含文档文本、字段值、敏感令牌或密钥。

`async_job` 同时作为可恢复发件箱：调度器先通过数据库短租约取得发布权，RabbitMQ publisher confirm 成功后才写入 `published_at`；发布进程在 confirm 与数据库提交之间崩溃时，消费者收到的真实 Broker 投递可以关闭该窗口。消费端使用数据库悲观锁和尝试编号幂等抢占，重复消息直接确认。可重试失败清除当前发布状态，按 `5s -> 10s -> 20s` 指数退避后重新发布；非重试错误或耗尽次数的消息 `reject(requeue=false)`，由 RabbitMQ 转入 DLQ。Worker 使用独立租约，进程退出后由恢复任务重新调度。

关键开关和参数使用 `DOCMIND_JOBS_ENABLED`、`DOCMIND_JOB_DISPATCHER_ENABLED`、`DOCMIND_JOB_CONSUMER_ENABLED`、`DOCMIND_JOB_*_INTERVAL`、`DOCMIND_JOB_WORKER_LEASE` 和 `DOCMIND_JOB_RETRY_*` 覆盖。生产环境必须启用 correlated publisher confirms 和 persistent delivery；默认配置已经开启。

## 健康检查

- `GET /actuator/health/liveness`：仅判断进程是否存活
- `GET /actuator/health/readiness`：检查 PostgreSQL、Redis、RabbitMQ、MinIO 及四个必需 bucket
- `GET /actuator/info`：返回服务基本信息

响应和结构化日志均携带 UUID 格式的 `X-Request-ID`；合法的调用方请求 ID 会原样透传，非法值会被替换。异常响应遵循 `packages/contracts` 的 snake_case 错误结构，未知异常不会暴露原始消息或文档内容。

## 验证

```bash
pnpm api:test
pnpm api:verify
pnpm check
pnpm e2e:mvp
```

`test` profile 使用 H2 隔离数据库并关闭 Redis、RabbitMQ、MinIO 健康依赖，验证请求关联、错误契约、存活探针、JWT、幂等、对象级权限、审计、文件真实性、staging/immutable 隔离、抽取任务边界、发布确认、重复投递、重试、死信决策、AI 熔断、加密信封以及含敏感多候选的完整抽取持久化链路。需要验证真实依赖、Flyway PostgreSQL 迁移和 RabbitMQ Broker 路由时先执行 `pnpm infra:start`，再分别启动 `pnpm ai:dev` 与 `pnpm api:dev`，最后执行 `pnpm e2e:mvp`。旧命令 `pnpm e2e:templates` 保留为兼容别名。

## 模块边界

后续代码按领域组织为 `source`、`extraction`、`template`、`instance`、`collaboration`、`diff`、`identity`、`audit`，避免只按 controller/service/repository 技术层平铺。跨领域通用的 Web 和错误处理放在 `shared`，数据库、消息队列和对象存储适配器放在 `infrastructure`。

数据库和对象存储只由本服务或其受控 Worker 访问；AI 服务不能绕过本服务读取租户数据。
