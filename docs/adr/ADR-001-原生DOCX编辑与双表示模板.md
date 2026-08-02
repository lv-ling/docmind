# ADR-001：原生 DOCX 编辑与双表示模板

状态：Proposed  
日期：2026-08-01  
决策范围：T0.1.1 / PRD v0.1.1

## 背景

T0.1 将 DOCX 解析为受控文档 JSON，再序列化为白名单 HTML。该方案适合安全抽取、结构化字段和节点 Diff，但浏览器流式布局不能复现 Word 的自动分页；当前解析器也只保留直接格式和显式/缓存分页提示。页眉页脚被渲染为单次区域，前端编辑器只支持按段落选择后整体修改，无法提供字符选择级的 Word 编辑体验。

继续扩展现有渲染器意味着自行实现样式继承、字体度量、换行、分页、分节、浮动对象、表格跨页和页眉页脚布局，其复杂度接近重新实现文字处理排版引擎。

## 决策

1. DOC/DOCX 模板以可编辑 DOCX 和字段绑定清单作为事实源；受控文档模型和安全 HTML 降为派生产物。
2. 使用自托管 ONLYOFFICE Docs 作为首选原生编辑引擎，通过内部 `DocumentEditorProvider` 接口隔离具体供应商，保留替换为 Syncfusion、Apryse 或其他引擎的能力。
3. 使用 Word Content Control 承载系统字段。控件 Tag 只保存受控标识，完整 Schema 信息保存在数据库清单中。
4. 每个模板版本必须固化：编辑文件、PDF 快照、字段清单、派生模型、资源、转换告警、摘要和引擎版本。
5. 左侧不可变原件继续使用 PDF；右侧 DOC/DOCX 使用原生分页编辑器。PDF 模板采用页面坐标字段层，不走 DOCX 富文本编辑器。
6. v0.1 的安全 HTML 版本保持不可变和可读，不批量原地改写；用户从原件按需创建 v0.1.1 原生编辑版本。

## 目标架构

```text
不可变 DOC/DOCX 原件
  ├─> 服务端布局引擎 ─> 原件 PDF（左侧对照）
  ├─> 工作 DOCX ─> 自托管编辑器（右侧编辑）
  │                    └─> 签名保存回调 ─> 新模板版本 DOCX
  └─> Python 解析 ─> 抽取/搜索/证据/派生文档模型

新模板版本 DOCX
  ├─> Content Control 校验 ─> 字段绑定清单
  ├─> 服务端布局引擎 ─> 版本 PDF 快照
  ├─> Python 解析 ─> 派生模型/文本/结构 Diff
  └─> 对象存储 + PostgreSQL 元数据 + 审计
```

## 服务职责调整

| 模块 | 保留 | 新增或调整 |
| --- | --- | --- |
| `apps/docmind-server` | 权限、模板版本、对象存储、任务、审计 | 编辑会话、编辑器配置/JWT、保存回调、DOCX 安全校验、Content Control 校验、版本提交编排 |
| `apps/docmind-document-ai` | PII、抽取、PDF/DOCX 文本解析、证据 | 输出派生模型和版式告警；不负责浏览器分页，不持有编辑会话 |
| `apps/docmind-web` | 左右分栏、原件 PDF、版本和告警 | 嵌入原生编辑器、字段面板、保存状态、会话恢复；移除段落下拉式主编辑路径 |
| `apps/docmind-web/src/editor` | 受控模型、字段绑定、结构 Diff、旧 HTML 兼容 | 作为 Web 内部模块增加字段清单和派生产物适配；不再承担 Word 页面布局事实源 |
| `deploy` | PostgreSQL、Redis、RabbitMQ、MinIO | 文档编辑服务、内部网络、JWT Secret、健康检查、资源限额和字体包 |

## 编辑会话与提交协议

1. Web 请求创建编辑会话，API 校验模板当前版本和用户权限。
2. API 生成唯一文档 key、短时文件 URL、回调 URL、权限和 JWT；编辑服务只能通过 API 读取工作副本。
3. 用户点击“保存为新版本”时，API 创建提交意图并请求强制保存。
4. 回调到达后，API 校验 JWT、会话、状态、nonce 和回调顺序，只从配置的编辑服务主机下载结果。
5. API 校验 MIME/ZIP/OOXML、摘要、宏和外部关系，再校验 Content Control 与字段清单。
6. 文件先进入 staging；数据库事务创建模板版本和异步渲染任务后再固化对象。失败时清理 staging，不更新当前版本。
7. PDF、派生模型和 Diff 完成后版本转为 `ready`；发布仍是独立操作。

## 接口方向

计划新增以下 API；详细请求响应在实现前进入 OpenAPI：

- `POST /api/v1/templates/{templateId}/editor-sessions`
- `GET /api/v1/template-editor-sessions/{sessionId}`
- `POST /api/v1/template-editor-sessions/{sessionId}/bindings/prepare`
- `POST /api/v1/template-editor-sessions/{sessionId}/commit`
- `DELETE /api/v1/template-editor-sessions/{sessionId}`
- `POST /api/v1/integrations/onlyoffice/callback`
- `GET /api/v1/template-versions/{versionId}/editable-content`
- `GET /api/v1/template-versions/{versionId}/rendered-preview`
- `GET /api/v1/template-versions/{versionId}/bindings`

回调接口是内部集成接口，不向普通浏览器暴露。公开创建/提交接口必须使用 `Idempotency-Key`，提交必须携带基础版本或 `If-Match`。

## 安全约束

- 编辑服务自托管，默认禁止访问公网；只允许访问 API 暴露的受控文件端点。
- 编辑配置和回调启用 JWT；Secret 只进入 Secret 管理，不进入仓库或日志。
- 回调下载 URL 做协议、主机、端口和 DNS/IP 校验，禁止任意重定向和私网探测。
- 禁止宏执行、外部模板、危险 OLE、远程关系和未批准插件；上传与保存后均重新扫描。
- 文档内容、字段值和签名 URL 不进入日志；审计只保存对象 ID、摘要、动作和结果。
- 字体包由部署清单控制，记录字体替代但不打包无授权字体。

## 备选方案及结论

| 方案 | 结论 |
| --- | --- |
| 继续增强自研 HTML + `contenteditable` | 不选为主路径；可保留旧版本兼容。自动分页和复杂 Word 布局成本过高 |
| Aspose.Words `HTML_FIXED` | 适合高保真只读预览和评测候选，不适合作为语义富文本编辑模型 |
| Tiptap Pages | 适合高度定制的受控编辑器，但分页/表格能力和版本成熟度暂不满足首选路径；保留后备评估 |
| CKEditor Pagination | 不用于导入 Word 原始分页还原 |
| ONLYOFFICE Docs | 首选 POC；原生分页编辑、内容控件、评论/审阅扩展路径与当前需求匹配 |
| Syncfusion/Apryse | 商业备选；如果 ONLYOFFICE 在样本文档、授权或集成约束上不通过 Gate G0，则进入同一 Provider 接口评估 |

## 影响

正面影响：用户获得字符级富文本编辑和原生分页；页眉页脚、表格和版式由成熟引擎处理；v0.2 可在稳定编辑底座上实现批注和纠错。

代价与风险：新增重型部署组件、授权审核、回调与编辑会话状态机；模板版本存储增加；只有编辑文件与字段清单是事实源，派生模型可能延迟；编辑引擎升级需要回归版式集。

## 退出与回滚

- 通过功能开关按工作区启用 `native_docx_editor`。
- v0.1 旧编辑器与数据保持只读兼容，原生编辑失败时可关闭开关，不回写历史版本。
- Provider 接口必须允许切换编辑引擎；数据库避免保存 ONLYOFFICE 专属响应体，只保存中立会话和版本字段。
- POC Gate G0 未通过时终止生产接入，转评 Syncfusion/Apryse；不在失败 POC 上继续扩展业务功能。
