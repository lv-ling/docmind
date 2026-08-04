# DocMind v2.0 实施路线图

## 1. 当前基线

仓库当前已具备：登录/工作区、原件上传与预览、抽取方案、敏感规则、抽取与人工复核、模板转换/列表/受控编辑、ONLYOFFICE 原生编辑 POC、基础 UI/路由/契约/测试，以及 PostgreSQL、Redis、RabbitMQ、MinIO、ONLYOFFICE 本地部署。

仍需重点补齐：PRD v2.0 契约重构、业务文档单一当前内容、内部/外部互斥流程、顺序审批、传阅、外链轮次、权威 Diff、对比调整、完整智能审阅、通知与审计查询。

## 2. 工作包顺序

### Phase 0：契约与技术门禁

- T00-01：修订 OpenAPI，将旧 `instance/version` 用户语义改为业务文档当前内容与内部快照。
- T00-02：补齐业务文档、流程、待办、审批、传阅、外链、通知、审计查询契约。
- T00-03：完成 G0～G2 文档技术 Spike，确认 ONLYOFFICE 许可、Content Control 与 PDF 坐标方案。
- T00-04：通过 `@tailwindcss/vite` 接入 Tailwind CSS、映射 `--dm-*` 设计令牌，并建设 `DmDialog/DmDataTable/DmPdfViewer/DocumentPicker` 等共享组件。
- T00-05：定义统一错误码、状态字典、权限能力码和页面 feature guard。

完成门禁：契约评审通过；代表性 DOCX/PDF 可以预览、绑定、回填；不再以原型 mock 状态作为开发接口。

### Phase 1：基础体验与原件闭环

- P01～P05：登录、应用壳、工作台、资产列表/上传、原件详情。
- P06～P10：抽取方案、任务、创建与人工复核。
- 补齐服务端分页、筛选、SSE、失败重试和对象级权限测试。

完成门禁：原件到批准 JSON 闭环通过；敏感字段与证据权限正确。

### Phase 2：模板闭环

- P11～P12：模板中心和模板设计器。
- 完成 DOCX Content Control、PDF 坐标字段、质量检查、保存新版本、发布、恢复与许可验收。

完成门禁：只有有效 `fillable` 已发布版本可进入业务文档创建器；历史版本不可变。

### Phase 3：业务协作闭环

- P13～P20：业务文档列表、创建、内部填写、详情、待办、审批、外部管理与外部填写。
- 建立流程状态机、编辑租约、顺序审批、传阅、外链轮次、删除/回收站与审计快照。

完成门禁：内部和外部模式互斥；审批人只读；冲突不覆盖；完成文档锁定。

### Phase 4：对比闭环

- 完成 G3 权威 Diff 引擎与结构化差异契约。
- P21～P23：对比列表、新建对比、结果与调整。

完成门禁：三种来源组合、仅对比、唯一调整目标、保存后重算与可重放通过。

### Phase 5：智能审阅与治理

- 完成 G4 AI 安全与结构化消息协议。
- P24～P30：智能审阅、AI 助手、安全策略、成员、模型、审计与异常页。

完成门禁：证据、敏感令牌化、人工确认、建议过期、图表白名单和审计通过。

## 3. 并行边界

- Web 页面可以在 OpenAPI mock server 上并行，但契约必须先冻结。
- Diff 引擎、流程引擎和 UI 设计系统可以并行，它们通过版本化契约集成。
- 模板字段绑定未通过前，不开发依赖真实回填的业务填写保存。
- 业务文档快照未完成前，不开发对比调整和智能审阅写回。

## 4. 每个页面的开发步骤

1. 更新或新增 Server OpenAPI、事件与 JSON Schema。
2. 完成 Server 领域状态机、权限、幂等、审计和测试。
3. 更新 Web `contracts` 与 API 适配器边界测试。
4. 实现页面 model/composable/区域组件。
5. 覆盖加载、空、失败、权限、冲突和成功状态。
6. 做键盘、焦点、读屏文本和敏感信息检查。
7. 运行应用级检查；跨服务功能运行 E2E。

## 5. 验证矩阵

| 改动 | 最低验证 |
| --- | --- |
| Markdown 任务文档 | 链接、格式、`git diff --check` |
| Web 页面 | `pnpm format:check`、`pnpm typecheck`、`pnpm test`、`pnpm build` |
| Server | `./mvnw test`；交付前 `./mvnw verify` |
| Document AI | Ruff、format、mypy、pytest、确定性 eval |
| 跨应用流程 | `./scripts/dev/e2e.sh` 对应阶段场景 |
| ONLYOFFICE/文件链路 | 代表样本矩阵、恶意文件矩阵、回调/断线/超时矩阵 |

## 6. 首个推荐迭代

第一个正式迭代只做 Phase 0，不直接铺开全部页面。其交付物应是：v2.0 OpenAPI、状态/权限字典、Content Control Spike、PDF.js 查看器、基础 DataTable/Dialog，以及登录/应用壳/资产列表三个页面。这样后续页面可以复用稳定边界，不会在旧 `instance version` 语义上继续堆代码。
