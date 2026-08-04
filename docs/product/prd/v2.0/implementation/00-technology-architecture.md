# DocMind v2.0 总体技术架构

## 1. 结论

沿用现有三应用架构，不重写技术栈：

| 层 | 确定采用 | 主要职责 |
| --- | --- | --- |
| Web | Vue 3、TypeScript、Vite、Tailwind CSS、Vue Router、Pinia、Vitest | 页面、权限呈现、上传、预览、字段填写、差异和审阅交互 |
| 业务 Server | Java 17、Spring Boot 3.5、Spring Security、JPA、Flyway | 身份、工作区、对象权限、状态机、审计、幂等、编辑租约、对象存储访问 |
| Document AI | Python 3.12、FastAPI、Pydantic、LangGraph | 文档解析、PII 令牌化、结构抽取、统一文档模型、确定性 Diff、智能审阅 |
| 数据与任务 | PostgreSQL、Redis、RabbitMQ、MinIO | 业务事实、短租约/限流、异步任务、不可变文件与派生产物 |
| 文档服务 | 自托管 ONLYOFFICE Docs、LibreOffice、PDFBox、Apache POI | DOCX 原生编辑、格式转换、PDF 校验/生成、OOXML 检查与字段回填 |

现有仓库已经实现上述大部分基础设施和 MVP 链路。v2.0 的主要工作不是更换框架，而是补齐业务文档/流程、外部填写、权威 Diff、智能审阅以及统一的页面组件。

## 2. Web 组件策略

### 2.1 设计系统

保留现有 `src/ui`、`Dm*` 组件和 `--dm-*` 设计令牌，以此作为唯一业务 UI 门面。正式引入 Tailwind CSS 作为生产工程的原子样式方案，通过 Vite 构建集成；原型中的 Tailwind CDN 只用于设计稿展示，生产代码禁止使用 CDN 版本。

底层能力统一规划如下；Tailwind CSS 已确定引入，其余新增生产依赖按第 11 节完成评审后引入：

| 能力 | 建议组件 | 使用方式 |
| --- | --- | --- |
| 原子样式与主题 | Tailwind CSS、`@tailwindcss/vite` | 通过 Vite 插件接入；主题值映射 `--dm-*` 设计令牌，页面优先使用标准 utility class |
| Dialog、Drawer、Popover、Select、Tabs、Tooltip、Dropdown、Combobox | Reka UI | 仅作为无样式、可访问的 primitive，封装为 `DmDialog` 等，不让页面直接依赖 |
| 数据表格 | TanStack Table Vue | 封装 `DmDataTable`，处理服务端分页、排序、筛选、列可见性和行选择 |
| PDF 预览 | `pdfjs-dist` | 自建 `DmPdfViewer`，使用 Display Layer，不直接嵌入未改造的通用 viewer |
| 图表 | Apache ECharts | 封装 `DmChart`，仅按需注册折线、柱状、饼图模块 |
| 图标 | `lucide-vue-next` | 统一线性图标；所有纯图标按钮提供 `aria-label` |
| JSON Schema 校验 | Ajv 2020 | 前端即时校验；服务端仍是最终权威校验 |
| 大列表虚拟化 | `@tanstack/vue-virtual` | 只用于长字段树、长 Diff 和长审阅建议列表 |

Tailwind CSS 负责布局、间距、排版、颜色、响应式和常见视觉状态；Reka UI 负责 WAI-ARIA、键盘、焦点与浮层交互；TanStack Table 负责表格状态。三者都隐藏在 `Dm*` 设计系统和应用组件之后，避免业务页面形成多套使用方式。[Tailwind CSS Vite 集成](https://tailwindcss.com/docs/installation/using-vite) · [Reka UI](https://reka-ui.com/docs/overview/introduction) · [TanStack Vue Table](https://tanstack.com/table/latest/docs/framework/vue/vue-table)

### 2.2 Tailwind CSS 使用边界

- 生产工程安装 `tailwindcss` 和 `@tailwindcss/vite`，在 Vite 配置中启用插件，并从应用全局样式入口导入 Tailwind CSS；不使用 Play CDN 或运行时注入。
- 页面使用 Tailwind utility class 完成布局和页面级视觉组合；稳定、重复、具有交互语义的模式必须下沉为 `Dm*` 或领域组件，不能靠复制长串类名复用。
- `--dm-*` 仍是品牌色、语义色、字号、间距、圆角、阴影和层级的设计令牌来源；Tailwind 主题应引用这些令牌，不另建一套互相冲突的数值体系。
- `Dm*` 组件可以在内部使用 Tailwind 实现视觉变体，但必须通过类型化 props 暴露 `size`、`variant`、`state` 等稳定 API；业务页面不得通过深层选择器修改组件内部结构。
- 动态样式使用完整 class 映射，避免运行时字符串拼接导致构建扫描遗漏；任意值只用于无法归入设计令牌的一次性特殊布局。
- 现有 CSS 按页面改造范围渐进迁移，不为了接入 Tailwind 一次性重写无关页面。

### 2.3 明确不采用

- 不把 Element Plus、Ant Design Vue 或 Naive UI 作为全局视觉层；它们会与现有 `Dm*` 组件、页面结构和设计令牌形成两套体系。
- 不同时引入 UnoCSS 或另一套原子 CSS 框架，避免重复扫描、类名规则和主题配置分裂。
- 不用 TipTap、Quill 或普通 `contenteditable` 替代 Word 编辑器；它们不能保证 DOCX 的分页、分节、页眉页脚、复杂表格和样式保真。
- 不直接复用 React 版 Ant Design X。AI 对话使用 Vue 自有 `DmAiConversation`，消息按结构化块渲染。

### 2.4 页面组件清单

应优先建设以下共享组件，再批量实施页面：

```text
src/ui/components/
├── DmButton / DmTextField / DmStatus / DmSplitPane（已有）
├── DmDialog / DmAlertDialog / DmDrawer
├── DmSelect / DmCombobox / DmDateRange
├── DmDataTable / DmPagination / DmFilterBar
├── DmTabs / DmTooltip / DmMenu
├── DmEmptyState / DmErrorState / DmSkeleton
└── DmProgress / DmStepper / DmTimeline

src/components/document/
├── DocumentSourceBadge
├── DocumentStatusBadge
├── DocumentPicker
├── DocumentTaskProgress
├── DmPdfViewer
├── DocumentFieldOverlay
├── BoundFieldPanel
└── DocumentEvidenceAnchor

src/components/ai/
├── DmAiConversation
├── AiMessageRenderer
├── AiEvidenceLink
├── AiSuggestionCard
└── AiChartBlock
```

## 3. PDF 预览方案

### 3.1 处理链路

```text
DOC/DOCX/PDF 原件
  -> 隔离转换/安全校验
  -> 生成不可变 PDF 预览
  -> MinIO 私有对象
  -> Spring Boot 同源鉴权流式接口
  -> PDF.js Display Layer
  -> canvas + text layer + evidence/field overlay
```

PDF.js 的 Display Layer 提供页面获取、viewport、canvas 渲染和文本信息，适合构建自有查看器；Viewer Layer 只作为参考，不原样嵌入。[PDF.js 官方说明](https://mozilla.github.io/pdf.js/getting_started/)

### 3.2 `DmPdfViewer` 能力

- 连续页/单页模式、上一页/下一页、页码跳转、缩放、适宽、旋转。
- 文本搜索与结果跳转；证据高亮用 `node_id + page + bbox + quote` 定位。
- 按页懒加载和卸载不可见 canvas，避免大文档常驻内存。
- 预览 URL 只指向同源鉴权 API；禁止把 MinIO 预签名地址写进 DOM、日志或本地存储。
- 下载是独立权限动作，不能把 canvas 截图当作原件下载。
- 预览失败与原件验证失败分开显示，并允许仅重试预览。

### 3.3 坐标规范

服务端统一保存 PDF point 坐标：`page_number`、`x`、`y`、`width`、`height`、`rotation`。前端通过 PDF.js viewport 变换映射到 CSS 像素。证据、PDF 模板字段和对比锚点都复用该坐标协议。

## 4. Word 编辑与模板设计

### 4.1 选择 ONLYOFFICE Docs

仓库已有自托管 ONLYOFFICE Document Server、JWT 会话、短期下载 URL、签名回调和保存状态轮询，继续沿用。ONLYOFFICE Docs API 支持嵌入编辑器、权限配置、比较与表单填写；Office API 能读取和操作 Content Control。[ONLYOFFICE Docs API](https://api.onlyoffice.com/docs/) · [Content Control API](https://api.onlyoffice.com/docs/office-api/usage-api/document-api/ApiDocument/)

### 4.2 两类编辑会话

| 会话 | 能力 | 允许用户 |
| --- | --- | --- |
| 模板设计会话 | 完整 DOCX 编辑、创建/修改 Content Control、字段绑定、质量检查 | 模板设计者且持模板编辑租约 |
| 业务文档受控修订会话 | 在“对比并调整/智能审阅”场景修改唯一业务文档 | 当前内部编辑人且持业务编辑租约 |

两类会话使用不同 session scope、对象路径、回调 nonce 和状态机，不复用同一份工作副本。

### 4.3 模板设计器实现

- DOCX：左侧或主区域嵌入 ONLYOFFICE，右侧显示字段定义/绑定检查器。
- 插入绑定：通过受控 ONLYOFFICE 插件或 Document/Automation API，在选区或光标处创建 Content Control；Tag 写系统 binding ID，不直接写敏感业务值。
- 服务端保存回调后重新验证 ZIP、OOXML、宏、OLE、外部关系、路径穿越、Content Control 唯一性与嵌套规则。
- PDF 模板：使用 PDF.js + 自定义坐标字段设计层，不允许修改 PDF 正文。
- 模板“自动保存”只保存工作副本；“保存为新版本”才固化 DOCX/PDF、绑定清单、PDF 快照与统一模型。

### 4.4 商业许可门禁

生产上线前必须完成 ONLYOFFICE 版本与许可证评审。Community 版本存在并发连接限制，部分外部连接/Automation 能力仅在商业版本提供；官方文档显示 Community 版本免费并发连接数有限，而 `createConnector` 仅用于 Docs Developer。许可、白标、并发和插件 API 必须纳入 G0 Spike，不能等上线后再确认。[ONLYOFFICE 版本说明](https://helpcenter.onlyoffice.com/docs/faq/docs-enterprise.aspx) · [Docs API methods](https://api.onlyoffice.com/docs/docs-api/usage-api/methods/)

## 5. 业务文档只允许编辑指定字段

### 5.1 结论

日常内部填写和外部填写不开放完整 ONLYOFFICE 编辑器。页面使用受控字段层，服务端以模板版本中的绑定清单为白名单。即使用户篡改前端请求，服务端也只接受允许字段，并重新进行类型、权限和业务状态校验。

### 5.2 产物模型

每个已发布 `fillable` 模板版本必须包含：

```text
template.docx / template.pdf       原始编辑产物
preview.pdf                        只读快照
document-model.json                稳定 node_id 的统一文档模型
bindings.json                      字段定义、JSON Path、Content Control/坐标锚点
field-layout.json                  可选页码与 bbox；无可靠位置时为空
```

### 5.3 填写页面

- 有可靠 `bbox`：在 PDF/文档派生视图上覆盖可聚焦输入控件，点击原位置直接填写。
- 无可靠 `bbox`：点击锚点打开字段弹层，或使用右侧 `BoundFieldPanel`；页面明确提示“此字段在面板填写”。
- 同一 binding ID 多处出现：共享同一个表单状态，修改后同步高亮所有锚点。
- 不向无权限用户下发敏感明文；掩码值不能作为真实表单值回传。
- 保存提交 `{field_key: typed_value}` 与 `content_revision`，禁止上传任意 HTML 或任意 DOCX。

### 5.4 服务端回填

```text
校验流程状态/处理人/租约/If-Match
  -> 校验字段白名单与字段级权限
  -> JSON Schema + 业务规则校验
  -> 保存结构化 current_data
  -> 回填 DOCX Content Control 或 PDF AcroForm/坐标字段
  -> 生成新 current DOCX/PDF 派生产物
  -> 更新同一业务文档内容校验标识
```

ONLYOFFICE 的 `fillForms` 权限可把编辑器限制为只填写表单，但官方当前表单链路以 PDF 表单为主，因此它适合作为 PDF 模板增强路径，不应假设普通 DOCX Content Control 天然等价于安全字段白名单。[ONLYOFFICE permissions](https://api.onlyoffice.com/docs/docs-api/usage-api/config/document/permissions/) · [嵌入 PDF 表单](https://api.onlyoffice.com/docs/docs-api/get-started/how-it-works/embedding-forms-into-a-web-page/)

## 6. 文档对比方案

### 6.1 权威 Diff 与编辑器 Diff 分离

- ONLYOFFICE 内置 Compare 可以在编辑器内展示 tracked changes，适合作为人工调整辅助。[ONLYOFFICE Compare](https://api.onlyoffice.com/docs/docs-api/get-started/how-it-works/comparing-documents/)
- DocMind 的权威 Diff 必须由服务端基于固定输入快照与算法版本生成，持久化结构化差异；这样才能筛选、定位、批注、导出、重放和审计。

### 6.2 统一文档模型

```text
Document
├── metadata: format/page/section settings/reliability
├── blocks[]: paragraph/heading/list/table/image/header/footer/page-break
├── runs[]: text + style fingerprint
├── bindings[]: binding_id/json_path/value/anchor
└── assets[]: image digest/size/position
```

所有节点必须有稳定 `node_id`。DOCX 从 OOXML 解析；PDF 使用文本块、字体、坐标和页面对象解析；扫描件没有 OCR 时标记 `unsupported` 或低可靠度，不能伪造结构精度。

### 6.3 Diff 流水线

1. Spring Boot 校验 A/B 对象权限、状态与模式。
2. 上传文件先经过安全验证并固化为临时不可变版本。
3. 业务文档创建只读对比快照；模板/原件固定版本 ID。
4. RabbitMQ 调度 Document AI 解析双方统一模型。
5. 先按稳定 ID/标题/表格结构做块级匹配，再做文本序列、样式、表格单元格、图片摘要、字段值与页面设置差异。
6. 保存 `algorithm_version`、输入摘要、可靠度、告警和结构化 changes。
7. 前端按差异 ID 同步定位双方；草稿 Diff 明确标为非权威。

### 6.4 调整规则

- 仅当前内部编辑人持租约时可以指定唯一业务文档为调整目标。
- 原件、临时上传、模板、外部编辑中、审批中和已完成业务文档永远只读。
- “应用 A 到 B”只对可安全映射的文本、格式或字段差异开放；操作前展示方向和实际变更。
- 保存时重新检查租约和 `If-Match`，更新同一业务文档，再创建调整后快照并重算权威 Diff。

## 7. 智能审阅与 AI 助手

- 进入详情先固化输入快照，再创建默认审阅任务；模型只接收令牌化文本。
- 输出使用结构化协议：`text`、`markdown_table`、`chart`、`suggestion`、`evidence`，而不是让前端执行模型返回的 HTML/JS。
- Markdown 必须经过白名单净化；图表只接收预定义 ECharts option 子集。
- 建议锚点包含 `node_id + offsets + quote + prefix + suffix`，内容变化后尝试重定位；失败则标记“需重新定位”。
- 接受建议先显示 before/after，只有当前内部编辑人可以写入；批量接受必须二次确认。
- 对话、建议和图表都绑定固定快照；保存后旧建议进入 `outdated`。

## 8. API、状态与实时更新

### 8.1 契约先行

现有 OpenAPI 中的 `DocumentInstance` 仍暴露版本号并使用旧状态，和 PRD v2.0 的“单一当前内容 + 内部审计快照”不完全一致。开发页面前必须先升级契约：

- 用户可见对象统一使用 `business_document`，内部快照使用 `business_document_snapshot`。
- 业务文档响应不返回用户可理解的业务版本号；只返回内部 `content_revision`/ETag。
- 状态改为 `draft/editing_internal/editing_external/awaiting_approval/completed/cancelled/recycled`。
- 增加流程、待办、审批链、传阅、外链轮次、删除/回收站 API。
- Diff、智能审阅与抽取输入都支持业务文档快照。

### 8.2 异步状态

- 抽取、预览、模板转换、模板提交、Diff、审阅使用 RabbitMQ 后台任务。
- Web 优先使用 SSE 订阅状态；断线后指数退避重连，超时或不支持时退化为带抖动轮询。
- 页面刷新后从服务端恢复任务，不能依赖前端计时器作为事实源。

### 8.3 并发

- 编辑租约在 Redis/数据库中有明确 holder、过期时间与续租。
- 保存同时校验租约、对象权限、流程状态、`If-Match` 和幂等键。
- 冲突返回 409 与最新 revision，前端提供复制本地内容、刷新或进入对比，不自动覆盖。

## 9. 安全与部署

- Browser -> Spring Boot 使用同源 HTTPS；Spring Boot 是唯一外部业务 API。
- ONLYOFFICE、Document AI、MinIO、RabbitMQ、Redis 和 PostgreSQL 仅在受控网络暴露。
- ONLYOFFICE 配置、下载 URL、回调全部签名并绑定 session nonce；编辑结果按不可信上传重新验证。
- 外部填写令牌使用高熵随机值，数据库只保存哈希；访问码使用慢哈希并限流。
- 模型密钥使用 KMS/Secret，不回传原值；前端只显示“已配置/尾号/更新时间”。
- 审计追加写入，不记录正文、完整字段、签名 URL、密钥或提示词。

## 10. 技术 Spike 与决策门禁

| Spike | 通过条件 | 阻断范围 |
| --- | --- | --- |
| G0 ONLYOFFICE 许可与还原度 | 代表性 DOCX 样本、字体、分页、回调、安全、并发、API 许可均通过 | 模板编辑、业务受控修订 |
| G1 Content Control | 插入/读取/锁定/重复同步/服务端回填/破坏检测通过 | fillable 模板发布、业务填写 |
| G2 PDF 字段坐标 | 缩放/旋转/多页/移动端/打印映射稳定 | PDF 模板与原位置填写 |
| G3 权威 Diff | 文本、结构、表格、格式、字段、页面告警可重放 | 文档对比、对比调整 |
| G4 AI 安全 | PII 零泄露、证据定位、输出白名单、建议过期通过 | 智能审阅与 AI 对话 |

## 11. 已确认与候选依赖清单

本文档只做架构决策，没有修改 `package.json`。Tailwind CSS 已确定引入，编码任务应安装并锁定以下构建依赖：

- `tailwindcss`
- `@tailwindcss/vite`

其余候选生产依赖仍需在对应编码任务开始前完成评审：

- `reka-ui`
- `@tanstack/vue-table`
- `@tanstack/vue-virtual`
- `pdfjs-dist`
- `echarts`
- `lucide-vue-next`
- `ajv`

ONLYOFFICE 商业版本、服务器端 OOXML 辅助库或 OCR 引擎属于部署/后端专项采购与许可评审，不应通过前端依赖变更顺带引入。
