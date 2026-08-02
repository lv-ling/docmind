# 《DocMind 产品功能与页面规划分析报告》

> 生成日期：2026-08-02
> 分析范围：`apps/web`、`services/api`、`services/ai`、`packages/contracts`、`packages/editor`、数据库迁移、自动化测试及产品 PRD
> 分析目标：为下一阶段产品页面结构重构提供功能与信息架构依据
> 不包含：颜色、组件样式、CSS、视觉规范等 UI 视觉设计

## 一、结论摘要

DocMind 当前不是一个通用“文档管理系统”，而是围绕不可变原件形成两条主要产品链路：

```text
不可变原件
├── 结构化数据链路：Schema → 安全抽取 → 人工复核 → 批准结果
└── 文档模板链路：文档解析 → 模板转换 → 人工微调 → 发布/恢复
```

当前完成度判断：

| 层级 | 结论 |
| --- | --- |
| v0.1 核心闭环 | 基本实现，可完成上传、预览、配置、Mock 抽取、复核、模板转换与版本发布 |
| 页面完整性 | 功能存在，但入口不完整，缺少任务中心、配置详情、版本管理、工作区管理等关键页面 |
| 后端隐藏能力 | 工作区创建、成员查询、原件新增版本、Schema/敏感规则版本、Schema 模板等已有 API，但前端没有使用 |
| AI 能力 | 文档解析、敏感信息令牌化和抽取工作流已实现；生产运行时仍固定使用 Mock Provider |
| 原生 DOCX 编辑 | 已有 ONLYOFFICE 隔离 POC，但没有进入正式模板版本状态机 |
| v0.2 能力 | 实例、编辑锁、批注、纠错只有 TypeScript 契约和 PRD，没有数据库、API、页面 |
| v1.0 能力 | 通用文档 Diff 只有契约和 PRD；目前仅有模板版本内部结构 Diff |
| 产品架构主要问题 | 页面按技术对象排列，但完整任务的查找、恢复、管理和治理入口不足 |

---

## 二、产品能力地图

### 1. 身份认证与会话

模块名称：身份认证

功能描述：

- 邮箱、密码登录。
- Bearer Token 身份认证。
- 查询当前用户。
- Token 保存在当前浏览器标签页的 `sessionStorage`。
- 会话失效后跳转登录页，并尝试恢复原工作区路径。
- 退出登录仅清除浏览器会话。

用户目标：

- 安全进入 DocMind。
- 会话过期后回到之前的工作位置。

当前实现状态：已实现。

当前实现位置：

- Login 页面。
- 前端 Auth Store、路由守卫。
- 后端 AuthController、JWT 安全配置。

涉及代码位置：

- `apps/web/src/views/LoginView.vue`
- `apps/web/src/stores/auth.ts`
- `apps/web/src/router/index.ts`
- `services/api/src/main/java/com/docmind/api/identity/api/AuthController.java`
- `services/api/src/main/java/com/docmind/api/identity/security/SecurityConfiguration.java`

存在缺口：

- 没有注册、找回密码、修改密码、主动失效服务端会话等能力。
- 本地开发账号在页面中预填，不属于正式产品流程。
- 没有账号管理页面。

### 2. 工作区与权限

模块名称：Workspace

功能描述：

- 用户可以属于多个工作区。
- 在全局应用壳中切换工作区。
- 后端支持创建工作区、查询工作区成员。
- 支持 Owner、Admin、Editor、Reviewer、Viewer 五种角色。
- 后端按角色控制查看、编辑、复核、成员管理和工作区管理权限。

用户目标：

- 隔离不同团队或业务空间的数据。
- 根据成员职责控制操作权限。

当前实现状态：

- 工作区列表、切换、角色鉴权：已实现。
- 创建工作区、成员列表：后端已实现，前端无入口。
- 邀请、修改角色、停用成员、删除工作区：未实现。

权限关系：

| 角色 | 查看 | 编辑内容 | 复核 | 成员管理 | 工作区管理 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Owner | 是 | 是 | 是 | 是 | 是 |
| Admin | 是 | 是 | 是 | 是 | 否 |
| Editor | 是 | 是 | 否 | 否 | 否 |
| Reviewer | 是 | 否 | 是 | 否 | 否 |
| Viewer | 是 | 否 | 否 | 否 | 否 |

当前实现位置：

- AppShell 工作区选择器。
- Workspace Store。
- Workspace API 和后端权限服务。

涉及代码位置：

- `apps/web/src/components/AppShell.vue`
- `apps/web/src/stores/workspace.ts`
- `services/api/src/main/java/com/docmind/api/identity/api/WorkspaceController.java`
- `services/api/src/main/java/com/docmind/api/identity/application/WorkspaceAccessService.java`
- `services/api/src/main/java/com/docmind/api/identity/application/WorkspacePermission.java`

存在问题：

- 用户只能切换工作区，不能在产品中创建或管理工作区。
- 没有成员管理页面。
- 前端操作入口没有充分按角色隐藏，部分低权限用户会先看到按钮，再由后端拒绝。
- 没有工作区级设置入口。

### 3. 原始文档管理

模块名称：原始文档 / Sources

功能描述：

- 上传 DOC、DOCX、PDF。
- 最大文件大小 10MB。
- 浏览器计算 SHA-256。
- 通过预签名地址直接上传对象存储。
- 服务端验证文件签名、MIME、大小、摘要和对象 ETag。
- 每个逻辑文档可以拥有多个不可变版本。
- 生成安全 PDF 预览。
- 查看、下载不可变原件。
- 从特定原件版本发起抽取或模板转换。
- 后台清理过期上传会话和 staging 文件。

用户目标：

- 安全登记进入 AI 流程的业务文档。
- 保留可追溯、不可覆盖的文件版本。
- 确认后续任务使用的是哪个原件版本。

当前实现状态：

- 首次上传、列表、详情、版本查看、预览、下载：已实现。
- 新增原件版本：后端已实现，前端无入口。
- 删除、归档、重命名、批量上传：未实现。

当前实现位置：

- Sources 页面。
- Source Detail 页面。
- SourceController、SourceService。
- MinIO 对象存储与异步预览任务。

涉及代码位置：

- `apps/web/src/views/SourcesView.vue`
- `apps/web/src/views/SourceDetailView.vue`
- `apps/web/src/api/sources.ts`
- `services/api/src/main/java/com/docmind/api/source/api/SourceController.java`
- `services/api/src/main/java/com/docmind/api/source/application/SourceService.java`
- `services/api/src/main/java/com/docmind/api/source/application/SourcePreviewJobHandler.java`
- `services/api/src/main/resources/db/migration/V3__source_documents.sql`

隐藏能力：

- `POST /sources/{sourceId}/versions` 已支持为现有文档创建新版本。
- 后端列表支持游标分页，但前端只读取前 50 条后在浏览器中分页。
- 上传会话具备过期状态与自动清理机制。
- 原件和预览采用带认证的同源下载接口，不直接暴露对象存储地址。

存在问题：

- “处理进度”是页面内静态流程说明，不是基于任务数据计算的真实进度。
- 文档详情看不到该版本产生的抽取任务和模板。
- 没有删除、归档和生命周期管理。
- 列表搜索、筛选、分页仅作用于已加载的前 50 条。
- 文档列表状态只反映是否有当前版本，无法反映预览、抽取、复核等状态。

### 4. 字段 Schema

模块名称：字段配置 / Extraction Schema

功能描述：

- 创建结构化抽取字段。
- 支持 string、number、integer、boolean、date、datetime、object、array。
- 配置必填、nullable、字面量默认值、敏感等级和抽取提示。
- 服务端生成严格 JSON Schema。
- Schema 按不可变版本管理。
- 后端支持创建 Schema 新版本。
- 后端支持把已发布 Schema 版本保存为可复用模板。

用户目标：

- 明确定义 AI 应从文档中返回什么数据。
- 固化某次抽取使用的字段版本。
- 在相似业务中复用字段结构。

当前实现状态：

- 新建 Schema 并发布 V1、列表查看：已实现。
- Schema 详情、新版本、Schema 模板创建：后端已实现，前端无入口。
- 高级约束完整编辑：契约和后端支持较多，当前页面只开放一部分。

当前实现位置：

- 配置中心的“字段配置”标签。
- SchemaController、SchemaService。
- Schema 契约及数据库表。

涉及代码位置：

- `apps/web/src/views/SchemasView.vue`
- `apps/web/src/api/schemas.ts`
- `services/api/src/main/java/com/docmind/api/schema/api/SchemaController.java`
- `services/api/src/main/java/com/docmind/api/schema/application/SchemaService.java`
- `packages/contracts/src/schema/schema.ts`
- `services/api/src/main/resources/db/migration/V4__schemas_and_sensitive_rule_templates.sql`

隐藏能力：

- 获取 Schema 详情与完整版本历史。
- 基于现有 Schema 创建新版本。
- 创建 Schema Template。
- 字段契约支持格式和正则、枚举、长度和数值边界、示例、嵌套 JSON Path、数组项类型、字段显示掩码和字段级可见角色。

存在问题：

- 当前页面只能新建，不能打开、编辑、复制、创建新版本。
- Schema 模板只显示数量，不能创建或应用。
- JSON Path 被固定生成为 `$.key`，无法配置嵌套结构。
- 数组项类型固定为 string。
- 枚举、格式、边界、示例、字段权限等未开放。
- 配置中心同时承担 Schema 和敏感规则，两个对象的生命周期被压缩在一个页面中。

### 5. 敏感信息规则

模块名称：敏感信息保护

功能描述：

- 支持九国规则：CN、US、JP、KR、DE、FR、GB、AU、NL。
- 支持 Presidio、RE2 正则、词典和受控 Validator。
- 支持电话、邮箱、身份证件、护照、银行账户、姓名、位置等数据类型。
- 抽取前将敏感明文替换为稳定令牌。
- 敏感令牌映射由 Java API 加密存储。
- 模型只能看到令牌化文档。
- 模型输出经过令牌、证据、Schema 和 PII 二次校验。
- 字段结果按角色返回明文或掩码。

用户目标：

- 避免业务敏感数据直接发送给外部模型。
- 按工作区配置适用的敏感识别规则。
- 让复核人员在权限范围内查看必要信息。

当前实现状态：

- 敏感规则版本模型、令牌化、加密映射和结果权限过滤：已实现。
- 页面创建九国固定预设：已实现。
- 自定义规则维护和版本管理：后端支持，前端未实现。

当前实现位置：

- 配置中心“敏感规则”标签。
- Python PII 模块。
- Java 敏感映射与结果权限过滤。

涉及代码位置：

- `apps/web/src/views/SchemasView.vue`
- `services/api/src/main/java/com/docmind/api/sensitive/api/SensitiveRuleTemplateController.java`
- `services/ai/src/docmind_ai/pii/tokenizer.py`
- `services/ai/src/docmind_ai/pii/detector.py`
- `services/api/src/main/java/com/docmind/api/extraction/application/SensitiveTokenMappingService.java`
- `services/api/src/main/java/com/docmind/api/extraction/application/ExtractionResultViewAssembler.java`

存在问题：

- 用户不能编辑单条规则。
- 不能创建正则、词典或 Validator 自定义规则。
- 不能查看版本详情或发布新版本。
- 当前页面会重复创建相同预设，缺少复制、更新和去重流程。
- 没有独立的规则测试工具。

### 6. AI 抽取处理

模块名称：AI Processing / Extraction

功能描述：

- 选择一个不可变原件版本。
- 选择已发布 Schema 版本。
- 可选敏感规则模板版本。
- 创建异步抽取任务。
- 复核原件摘要。
- 解析 DOC、DOCX、PDF。
- 执行敏感检测与稳定令牌化。
- 执行 Schema 约束抽取。
- 校验输出预算、结构、证据、令牌和 PII。
- 受控恢复敏感值并加密保存结果。
- 完成后进入人工复核。

用户目标：

- 将非结构化文档转换为符合业务定义的可靠数据。
- 知道使用了哪个原件、Schema 和敏感规则版本。
- 在模型不确定时获得候选值和证据。

当前实现状态：

- 异步工作流、重试、失败终态、结果验证和持久化：已实现。
- 真实第三方模型：未接入。
- 运行时固定使用确定性 Mock Provider。
- OCR：接口已预留，但默认使用 DisabledOcrAdapter。

当前实现位置：

- Extraction Create 页面。
- ExtractionJobHandler。
- Python ExtractionWorkflow。
- RabbitMQ 持久任务系统。

涉及代码位置：

- `apps/web/src/views/ExtractionCreateView.vue`
- `apps/web/src/api/extractions.ts`
- `services/api/src/main/java/com/docmind/api/extraction/api/ExtractionController.java`
- `services/api/src/main/java/com/docmind/api/extraction/application/ExtractionJobHandler.java`
- `services/ai/src/docmind_ai/extraction/workflow.py`
- `services/ai/src/docmind_ai/app.py`
- `services/ai/src/docmind_ai/parsing/ocr.py`

隐藏能力：

- Provider 抽象已经存在，并实现了 LangChain Chat Model Adapter。
- 工作流具备 Provider 超时、总超时、指数退避、输入/输出 Token 预算。
- 异步任务具备数据库 Outbox、RabbitMQ confirm、工作租约、恢复调度、最大三次执行和 DLQ。
- SSE 不可用时前端自动降级到轮询。

存在问题：

- 没有抽取任务列表。
- 用户离开复核地址后，很难从产品中重新找到任务。
- 没有任务取消、手动重试或重新运行入口。
- 没有结果导出能力。
- 没有真实模型供应商配置、密钥管理或模型切换页面。
- 扫描 PDF 没有实际 OCR。
- 尚未使用真实黄金数据集衡量准确率和证据命中率。

### 7. 抽取人工复核

模块名称：Extraction Review

功能描述：

- 实时显示抽取状态。
- 查看原件 PDF。
- 查看字段值、置信度、来源、候选值和证据。
- 支持接受、修改、拒绝字段。
- 所有字段完成复核后批准结果。
- 敏感字段按角色掩码。
- 修改后再次执行 Schema 校验。
- 复核和批准请求具备幂等保护和审计记录。

用户目标：

- 验证 AI 结果是否可靠。
- 根据原文证据修正错误。
- 形成可以正式使用的批准数据。

当前实现状态：核心闭环已实现。

当前实现位置：

- Extraction Review 页面。
- ExtractionReviewService。
- ExtractionResultViewAssembler。

涉及代码位置：

- `apps/web/src/views/ExtractionReviewView.vue`
- `services/api/src/main/java/com/docmind/api/extraction/application/ExtractionReviewService.java`
- `services/api/src/main/java/com/docmind/api/extraction/application/ExtractionResultPersistenceService.java`
- `services/api/src/main/java/com/docmind/api/extraction/application/ExtractionResultViewAssembler.java`

存在问题：

- 没有待复核任务队列。
- 没有按文档、状态、创建人等维度搜索任务。
- 拒绝原因由页面写死，用户不能输入真实业务原因。
- 没有批量接受或仅处理异常字段能力。
- 批准后的结果没有明确下游用途或导出入口。
- 页面只支持通过 Extraction ID 进入，缺少业务上下文导航。

### 8. 文档模板

模块名称：Document Templates

功能描述：

- 从原件版本创建模板转换任务。
- 生成 PDF 对照预览、受控文档模型、白名单 HTML/CSS、文档资源和转换告警。
- 模板版本不可变。
- 支持受控段落和标题文字微调。
- 支持字号、粗体、对齐和页边距调整。
- 保存产生新版本。
- 服务端计算模板版本结构 Diff。
- 有阻断告警时禁止发布。
- 支持发布和“生成新版本式恢复”。

用户目标：

- 将不可编辑原件转换为可复用模板。
- 人工确认转换质量。
- 保留所有修改和发布历史。
- 在不改写历史版本的情况下恢复旧内容。

当前实现状态：v0.1 受控模板链路已实现。

当前实现位置：

- Templates 列表。
- Template Editor。
- 模板转换异步任务和版本服务。

涉及代码位置：

- `apps/web/src/views/TemplatesView.vue`
- `apps/web/src/views/TemplateEditorView.vue`
- `apps/web/src/api/templates.ts`
- `services/api/src/main/java/com/docmind/api/template/api/DocumentTemplateController.java`
- `services/api/src/main/java/com/docmind/api/template/application/TemplateConversionJobHandler.java`
- `services/api/src/main/java/com/docmind/api/template/application/DocumentTemplateService.java`
- `services/api/src/main/resources/db/migration/V9__document_templates.sql`

存在问题：

- 创建模板只能从原件详情进入，Templates 页面自身没有原件选择器。
- 模板列表没有搜索、筛选、删除、归档和负责人信息。
- 当前微调以“选择段落后编辑”为主，不是完整富文本编辑体验。
- 表格、图片、页眉页脚等虽存在于文档模型中，但页面不能全面编辑。
- 当前 Diff 主要是文档模型路径列表，不是通用文档对比产品。
- 发布后的模板目前没有可消费它的“模板实例”功能。

### 9. 原生 DOCX 编辑 POC

模块名称：Native DOCX Editor POC

功能描述：

- 创建短期 ONLYOFFICE 编辑会话。
- 使用受控下载 URL 和 JWT 回调。
- 对保存结果进行文件大小、ZIP、OOXML、路径穿越和 VBA 安全校验。
- 保存到独立 POC 对象路径。
- 查询回调和保存状态。

用户目标：

- 验证是否能够保留 Word 分页和字符级富文本编辑能力。

当前实现状态：隔离 POC，未进入正式产品版本链。

涉及代码位置：

- `apps/web/src/views/TemplateEditorView.vue`
- `services/api/src/main/java/com/docmind/api/template/editor/NativeEditorController.java`
- `services/api/src/main/java/com/docmind/api/template/editor/NativeEditorSessionService.java`
- `docs/adr/ADR-001-原生DOCX编辑与双表示模板.md`
- `docs/delivery/04-v0.1.1高保真文档改造计划.md`

未完成内容：

- G0 还原度门禁未通过。
- 保存文件不会创建正式模板版本。
- 没有字段 Content Control 绑定。
- 没有模板版本 PDF 快照状态机。
- 没有正式数据库编辑会话。
- 当前仅接受 DOCX。
- 授权、代表样本、故障矩阵尚未完成。

因此，它不应与正式模板编辑能力并列成为稳定主流程。

### 10. 审计、安全与平台任务

模块名称：平台治理

功能描述：

- 追加式审计事件。
- 请求 ID。
- 业务幂等键和请求哈希。
- 工作区级权限验证。
- 敏感 JSON AES-GCM 信封加密。
- 私有对象存储。
- 安全文件校验。
- 可恢复异步任务、重试和死信队列。
- AI 服务内部 Token 鉴权。
- AI 熔断和超时。

用户目标：

- 追踪关键业务操作。
- 防止重复提交、越权和数据泄露。
- 在基础设施异常后自动恢复任务。

当前实现状态：底层已实现，产品管理面缺失。

涉及代码位置：

- `services/api/src/main/java/com/docmind/api/audit/application/AuditRecorder.java`
- `services/api/src/main/resources/db/migration/V2__identity_workspace_audit.sql`
- `services/api/src/main/java/com/docmind/api/extraction/messaging/AsyncJobRecoveryScheduler.java`
- `services/api/src/main/java/com/docmind/api/infrastructure/crypto/JsonEnvelopeEncryption.java`

存在问题：

- 没有审计查询 API。
- 没有审计页面。
- 没有异步任务运维页面。
- 没有模型配置或系统能力状态页面。

---

## 三、未完成与仅存在于契约中的能力

以下对象虽然已定义 TypeScript 契约和领域事件，但没有对应数据库迁移、Java API、业务服务或页面，不应计入当前可用能力：

| 规划能力 | 当前代码状态 |
| --- | --- |
| DocumentInstance | 仅契约 |
| InstanceVersion | 仅契约 |
| EditLock | 仅契约 |
| 实例填写、保存、提交、恢复 | 未实现 |
| CommentThread / Comment | 仅契约 |
| ProofreadingRun / Suggestion | 仅契约 |
| 术语词典 | 未实现 |
| 通用 DiffRun / DiffChange | 仅契约 |
| 上传文档与系统版本混合比较 | 未实现 |
| 浏览器实时草稿 Diff | 未实现 |
| Diff 批注与 AI 风险解释 | 未实现 |
| 实例或比较结果导出 | 未实现 |

证据位置：

- `packages/contracts/src/instance/instance.ts`
- `packages/contracts/src/review/review.ts`
- `packages/contracts/src/diff/diff.ts`
- `docs/product/prd/v0.2/PRD.md`
- `docs/product/prd/v1.0/PRD.md`

---

## 四、当前页面结构分析

实际路由定义了 8 个页面：

- Login
- Sources
- Source Detail
- Schemas
- Extraction Create
- Extraction Review
- Templates
- Template Editor

路由位置：`apps/web/src/router/index.ts`

### 1. Login

页面目的：身份验证并恢复工作区上下文。

用户进入原因：

- 首次访问。
- 主动退出。
- Token 过期。

当前包含功能：

- 邮箱密码登录。
- 会话过期提示。
- 安全重定向。
- 本地账号预填。

存在问题：

- 同时承担产品介绍和认证，但没有账号生命周期能力。
- 无密码管理和组织加入流程。

### 2. Sources

页面目的：作为原件登记簿和当前主要首页。

用户进入原因：

- 登录后的默认落点。
- 上传文档。
- 查找原件。
- 发起后续处理。

当前包含功能：

- 上传。
- 搜索和本地筛选。
- 本地分页。
- 选择文档。
- 查看摘要。
- 进入详情或发起抽取。

存在问题：

- 既是文档列表，又承担任务起点，职责过重。
- 右侧“当前任务”并不是真实任务。
- 无抽取任务和模板产物关联。
- 无新增版本入口。
- 仅加载前 50 条。

### 3. Source Detail

页面目的：查看一个逻辑文档的不可变版本和预览。

用户进入原因：

- 检查原件。
- 切换历史版本。
- 下载原件。
- 发起抽取或模板转换。

当前包含功能：

- 版本选择。
- 状态和文件元数据。
- PDF 预览轮询。
- 原件下载。
- 两条业务链路入口。

存在问题：

- 没有显示这个版本已经创建过哪些抽取或模板。
- 没有新增版本入口。
- 没有版本失败后的恢复操作。
- 文档和下游任务之间缺少可追溯导航。

### 4. Schemas / 配置中心

页面目的：管理字段 Schema 和敏感规则。

用户进入原因：

- 创建抽取配置。
- 创建敏感规则预设。
- 查看已发布配置。

当前包含功能：

- 创建并发布 Schema V1。
- 字段排序、类型、必填、默认值、敏感级别。
- 创建固定九国敏感规则模板。
- 展示 Schema 和规则列表。

存在问题：

- 两类独立对象被压缩到一个页面。
- 无详情、编辑、新版本、复制、归档。
- Schema Template 只有数量，没有操作。
- 契约支持的大量字段约束无法配置。
- 用户无法查看“哪些任务正在使用这个版本”。

### 5. Extraction Create

页面目的：创建绑定三个不可变版本的抽取任务。

用户进入原因：从原件列表或详情选择版本后发起抽取。

当前包含功能：

- 显示原件版本 ID。
- 选择 Schema 当前发布版。
- 选择敏感规则当前发布版。
- 创建任务并跳转复核页。

存在问题：

- 不能独立进入，缺少原件版本即报错。
- 只显示原件 UUID，不显示文档名称和文件信息。
- 不能查看配置详情。
- 没有任务名称、业务标签等组织属性。

### 6. Extraction Review

页面目的：等待处理并逐字段复核结果。

用户进入原因：任务创建后跳转，或掌握 Extraction ID 后直接访问。

当前包含功能：

- SSE 和轮询。
- 处理状态。
- 原件预览。
- 字段结果、证据、候选值、置信度。
- 接受、修改、拒绝、批准。
- 权限掩码。

存在问题：

- 没有任务列表入口。
- 无任务上下文和返回来源文档入口。
- 无批量复核。
- 拒绝原因固定。
- 无批准结果导出或下游操作。

### 7. Templates

页面目的：模板登记簿和模板转换入口。

用户进入原因：

- 查看工作区模板。
- 从原件详情携带参数进入并创建模板。

当前包含功能：

- 模板列表。
- 转换状态轮询。
- 从 URL 参数创建模板转换任务。
- 进入模板编辑器。

存在问题：

- 页面本身不能选择原件。
- 没有模板搜索、筛选、归档或删除。
- 没有发布状态和告警汇总。
- 用户难以判断哪些模板可以被业务使用。

### 8. Template Editor

页面目的：校验模板转换结果并管理版本。

用户进入原因：

- 打开模板。
- 查看转换状态。
- 微调、保存、发布或恢复版本。
- 尝试原生 DOCX POC。

当前包含功能：

- 转换状态。
- 版本历史。
- 原件对照。
- 受控文档预览和微调。
- 转换告警。
- 后端版本 Diff。
- 保存新版本。
- 发布和恢复。
- 原生编辑 POC。

存在问题：

- 一个页面混合了稳定模板版本管理与实验性 POC。
- 受控编辑范围有限。
- 版本历史、告警、Diff 和编辑操作集中在同一工作台，业务层级不清。
- POC 保存不进入正式版本链，用户容易误解操作结果。

---

## 五、业务流程梳理

### 1. 用户主流程：结构化数据抽取

```text
登录
↓
选择工作区
↓
进入原始文档
↓
上传 DOC / DOCX / PDF
↓
服务端校验并固化不可变版本
↓
后台生成 PDF 预览
↓
创建或选择字段 Schema
↓
创建或选择敏感规则版本
↓
从原件版本发起抽取
↓
异步解析、令牌化和抽取
↓
进入抽取复核
↓
逐字段接受 / 修改 / 拒绝
↓
Schema 二次校验
↓
批准抽取结果
```

当前断点：批准之后没有导出、推送、创建实例或其他下游流程。

### 2. 用户主流程：模板生成

```text
选择不可变原件版本
↓
发起模板转换
↓
后台生成 PDF、文档模型、安全 HTML、资源和告警
↓
进入模板编辑器
↓
原件与模板人工校验
↓
微调内容
↓
保存为新版本
↓
检查告警和版本 Diff
↓
发布当前版本
↓
必要时选择旧版并创建恢复版本
```

当前断点：发布后的模板还不能用于创建、填写和提交业务实例。

### 3. 异常流程

#### 上传异常

```text
扩展名/MIME/文件签名/大小不合法
→ 拒绝上传完成

上传会话过期
→ 标记 expired
→ 后台清理 staging 对象

摘要或 ETag 不一致
→ 不固化不可变版本
```

#### 预览异常

```text
LibreOffice 或存储暂时不可用
→ 异步任务重试

重试耗尽
→ 预览 failed
→ 原件仍可下载
```

#### 抽取异常

```text
原件或配置状态无效
→ 创建任务失败

AI 服务或模型临时不可用
→ retrying
→ 指数退避

原件摘要异常 / 令牌泄漏 / 未知令牌 / Schema 不匹配
→ 任务失败或字段进入重点复核

SSE 不可用
→ 前端自动使用轮询
```

#### 复核异常

```text
用户没有 Reviewer 权限
→ 后端拒绝

字段仍有 pending
→ 不能批准

人工修改值不满足字段类型或 Schema
→ 拒绝保存或批准
```

#### 模板异常

```text
解析、转换或资源保存失败
→ retrying
→ failed

存在阻断告警
→ 禁止发布

基于旧版本保存
→ 版本冲突

原生编辑器不可用
→ POC 退出
→ 不影响正式模板版本
```

### 4. 后台流程

```text
Web
→ Java API 鉴权与权限校验
→ PostgreSQL 创建业务对象及 async_job
→ Dispatcher 发布 RabbitMQ 命令
→ Consumer 获取工作租约
→ Job Handler 读取 MinIO 原件
→ 调用 Python AI / LibreOffice
→ 校验并持久化结果
→ 发布状态事件
→ Web SSE 或轮询更新
```

同时存在：

- 未发布任务扫描。
- Worker 租约过期恢复。
- 指数退避重试。
- 重试耗尽进入 DLQ。
- 上传会话过期清理。
- 审计事件追加写入。

---

## 六、领域模型分析

### 1. 已实现核心对象

| 领域 | 核心对象 |
| --- | --- |
| 身份 | UserAccount、Workspace、WorkspaceMember |
| 审计 | AuditEvent |
| 原件 | SourceDocument、SourceVersion、SourceUploadSession、SourcePreview |
| 配置 | ExtractionSchema、ExtractionSchemaVersion、ExtractionSchemaField、SchemaTemplate |
| 敏感规则 | SensitiveRuleTemplate、SensitiveRuleTemplateVersion、SensitiveRule、SensitiveToken |
| 任务 | AsyncJob |
| 抽取 | ExtractionRun、ExtractionFieldResult、ExtractionCandidate、ExtractionEvidence、ExtractionReviewOperation |
| 模板 | DocumentTemplate、ParsedContent、DocumentTemplateVersion、DocumentTemplateResource、DocumentConversionWarning、DocumentTemplateOperation |

### 2. 对象关系

```text
UserAccount
└── WorkspaceMember ── Workspace
                        ├── SourceDocument
                        │   └── SourceVersion
                        │       ├── SourcePreview
                        │       ├── ExtractionRun
                        │       └── DocumentTemplate
                        │
                        ├── ExtractionSchema
                        │   └── ExtractionSchemaVersion
                        │       └── ExtractionSchemaField
                        │
                        ├── SchemaTemplate ──→ 已发布 SchemaVersion
                        │
                        ├── SensitiveRuleTemplate
                        │   └── SensitiveRuleTemplateVersion
                        │       └── SensitiveRule
                        │
                        └── AuditEvent
```

抽取关系：

```text
SourceVersion
+ SchemaVersion
+ 可选 SensitiveRuleTemplateVersion
            ↓
      ExtractionRun
            ├── AsyncJob
            ├── SensitiveToken
            ├── ExtractionFieldResult
            │    ├── ExtractionCandidate
            │    └── ExtractionEvidence
            └── ExtractionReviewOperation
```

模板关系：

```text
SourceVersion
    ↓
DocumentTemplate
    ├── AsyncJob
    └── DocumentTemplateVersion
         ├── ParsedContent
         ├── DocumentTemplateResource
         ├── DocumentConversionWarning
         ├── Diff JSON
         └── DocumentTemplateOperation
```

关键业务约束：

- SourceVersion 永远不可修改。
- 抽取任务固定引用原件、Schema、敏感规则版本。
- 模板版本固定引用来源原件版本和解析结果。
- 回滚不会覆盖历史，而是创建新的模板版本。
- 敏感值、候选值、证据和人工修正值均加密保存。
- Web 获得的是按角色过滤后的结果视图，不是原始数据库对象。
- AsyncJob 是后台执行基础设施，不是用户任务本身。

### 3. 规划对象

以下关系尚未落地：

```text
Published TemplateVersion
└── DocumentInstance
    ├── InstanceVersion
    ├── EditLock
    ├── CommentThread
    │   └── Comment
    └── ProofreadingRun
        └── ProofreadingSuggestion

SourceVersion / TemplateVersion / InstanceVersion
└── DiffRun
    └── DiffChange
```

---

## 七、当前产品功能树

图例：

- `[已]` 已具备页面和业务链路
- `[后端]` 后端存在但前端无入口
- `[POC]` 实验性能力
- `[契约]` 只有契约或规划

```text
DocMind
├── Identity
│   ├── [已] 登录
│   ├── [已] 当前用户
│   ├── [已] 会话过期恢复
│   └── [已] 本地退出
│
├── Workspace
│   ├── [已] 工作区列表与切换
│   ├── [已] 五级角色权限
│   ├── [后端] 创建工作区
│   ├── [后端] 成员列表
│   └── [未实现] 邀请、角色修改、成员停用
│
├── Documents
│   ├── [已] DOC / DOCX / PDF 上传
│   ├── [已] 不可变版本
│   ├── [已] PDF 预览与原件下载
│   ├── [后端] 为文档上传新版本
│   └── [未实现] 删除、归档、批量管理
│
├── Data Configuration
│   ├── Extraction Schema
│   │   ├── [已] 创建并发布 V1
│   │   ├── [后端] 详情与版本历史
│   │   ├── [后端] 创建新版本
│   │   └── [后端] Schema Template
│   └── Sensitive Rules
│       ├── [已] 九国规则预设
│       ├── [已] 版本化规则模型
│       ├── [后端] 创建新版本
│       └── [未开放] 正则、词典、Validator 编辑
│
├── AI Processing
│   ├── [已] 文档解析
│   ├── [已] 敏感信息令牌化
│   ├── [已] Schema 约束抽取
│   ├── [已] 证据、候选值、置信度
│   ├── [已] 输出验证与受控回填
│   ├── [已] 异步重试与恢复
│   ├── [代码未接入] LangChain Provider
│   ├── [未实现] 真实模型配置
│   └── [未实现] OCR
│
├── Review
│   ├── [已] 字段接受
│   ├── [已] 字段修改
│   ├── [已] 字段拒绝
│   ├── [已] 整体批准
│   ├── [已] 字段级敏感掩码
│   └── [未实现] 任务队列与结果导出
│
├── Templates
│   ├── [已] 原件转受控模板
│   ├── [已] 转换告警
│   ├── [已] 受控文档微调
│   ├── [已] 不可变版本
│   ├── [已] 后端版本 Diff
│   ├── [已] 发布
│   ├── [已] 生成新版本式恢复
│   └── [POC] ONLYOFFICE 原生 DOCX 编辑
│
├── Governance
│   ├── [已] 审计写入
│   ├── [已] 请求幂等
│   ├── [已] 加密与对象私有访问
│   ├── [已] 异步任务恢复
│   └── [未实现] 审计和任务管理页面
│
├── Document Instances
│   ├── [契约] 实例
│   ├── [契约] 实例版本
│   ├── [契约] 编辑锁
│   ├── [契约] 批注
│   └── [契约] 纠错
│
└── Document Comparison
    ├── [契约] 权威 DiffRun
    ├── [契约] DiffChange
    ├── [未实现] 上传/系统版本混合比较
    └── [未实现] 浏览器实时 Diff
```

---

## 八、下一阶段页面架构建议

以下只讨论信息架构，不涉及视觉方案。

### 1. 建议的 v0.1 页面结构

```text
DocMind
├── 工作台
│   ├── 我的待办
│   ├── 处理中任务
│   └── 最近文档与模板
│
├── 原始文档
│   ├── 文档列表
│   └── 文档详情
│       ├── 原件版本
│       ├── 预览
│       ├── 抽取记录
│       └── 模板记录
│
├── 抽取任务
│   ├── 任务列表
│   ├── 创建任务
│   ├── 处理状态
│   └── 结果复核
│
├── 配置
│   ├── 字段 Schema
│   │   ├── 列表
│   │   ├── 详情
│   │   ├── 版本
│   │   └── 复用模板
│   └── 敏感规则
│       ├── 列表
│       ├── 详情
│       └── 版本
│
├── 文档模板
│   ├── 模板列表
│   ├── 模板详情
│   ├── 转换与编辑工作台
│   └── 发布及版本历史
│
└── 工作区管理
    ├── 基本信息
    ├── 成员与角色
    ├── 审计记录
    └── 系统能力状态
```

其中“抽取任务列表”需要新增后端列表 API；其余部分有相当比例可以复用现有后端能力。

### 2. 暂不进入主导航的功能

在真正实现前，不建议将以下模块作为正式导航入口：

- 模板实例。
- 批注与纠错。
- 通用文档 Diff。
- 原生 DOCX 正式编辑。
- 模型供应商设置。

这些能力目前仍处于契约、PRD 或 POC 阶段。

### 3. 页面重构优先级

#### P0：补全核心闭环的“可找回性”

- 新增抽取任务列表。
- 文档详情增加抽取记录和模板记录。
- 复核页增加返回来源文档和任务列表入口。
- 批准结果增加查看最终 JSON 和导出入口。

#### P1：补全配置生命周期

- 将 Schema 和敏感规则拆成独立资源结构。
- 增加详情、版本历史、新版本、复制和使用情况。
- 开放现有后端 Schema Template 能力。
- 开放原件新增版本能力。

#### P2：补全治理能力

- 工作区创建。
- 成员与角色管理。
- 审计记录查询。
- 角色驱动的前端操作权限。

#### P3：处理模板编辑架构

- 将“模板详情/版本管理”和“编辑工作台”职责分开。
- 原生编辑 POC 在通过 G0 并接入正式版本链前保持实验入口。
- 明确受控 HTML 模板与原生 DOCX 模板的产品边界。

#### P4：再进入后续版本

- v0.2：模板实例、锁、批注、纠错。
- v1.0：通用文档 Diff。
- 避免仅根据共享契约提前建设空页面。

---

## 九、最终判断

DocMind 已经具备一个安全、版本化的文档抽取与模板转换技术底座，但当前页面结构没有完整呈现底层领域模型：

- “原件”已有版本能力，但页面主要按单文件使用。
- “抽取”已有完整任务生命周期，但没有任务管理页面。
- “Schema”和“敏感规则”已有版本模型，但页面只有一次性创建表单。
- “模板”已有较完整的版本闭环，但与实验性原生编辑混在同一页面。
- “工作区、权限、审计”已存在于后端，却尚未形成管理产品。
- “实例、批注、纠错、通用 Diff”目前仍是未来能力，不能按已实现功能规划正式页面。

下一阶段页面重构的核心不应是增加更多技术对象入口，而应先补齐三个产品闭环：

```text
文档 → 任务 → 结果
配置 → 版本 → 使用情况
模板 → 校验 → 发布 → 后续使用
```

## 十、验证说明

- 本报告以当前代码、数据库迁移和路由为主要事实来源，PRD 仅用于识别规划边界。
- 已执行 `pnpm test`。
- 测试结果：28 个测试文件、70 项测试全部通过。
- 报告没有把仅存在于 TypeScript 契约或 PRD 中的对象判断为已实现能力。
