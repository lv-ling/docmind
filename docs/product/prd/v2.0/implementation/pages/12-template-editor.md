# P12 模板设计器

## 页面定义

- 路由：`/templates/:id/editor`，对应原型 `#template-editor`。
- 目标：设计 DOCX/PDF 模板字段、保存不可变新版本、质量检查并发布。

## 布局

- 顶栏：返回、模板名、草稿/版本、租约状态、自动保存、质量检查、保存为新版本、发布。
- 版本条：当前草稿、历史版本、published 指针、恢复入口。
- DOCX 模式：主区 ONLYOFFICE；右侧字段定义/绑定/告警 Inspector。
- PDF 模式：左/主区 PDF.js 页面与坐标字段层；右侧字段与属性 Inspector。
- 底部或 Drawer：转换告警、结构 Diff、保存任务进度、审计记录。

## 字段定义

binding ID、key、JSON Path、展示名、类型、required、nullable、默认值、enum/format/pattern/边界、敏感等级/权限、锁定策略、缺失策略、同步组、Content Control tag 或 PDF page/bbox。

字段可以从已发布抽取方案导入，也可以新建。导入是复制，后续方案变更不自动同步。

## DOCX 操作

- 打开短期 ONLYOFFICE 编辑会话，JWT、nonce、下载 URL 和回调均由 Server 签发。
- 选中文字/占位内容或光标位置后创建 Content Control，并绑定系统 binding ID。
- 同一字段可绑定多处，均加入同一同步组。
- `${name}` 只提供识别/转换建议，发布前必须转为正式绑定。
- 回调文件按不可信输入重新做大小、ZIP、OOXML、宏、OLE、外部关系和绑定校验。

## PDF 操作

- PDF 正文不可编辑；拖拽创建文本、日期、数字、单选、复选、图片、签名占位区域。
- 坐标保存为 PDF point，并校验越界、重叠、旋转和最小尺寸。
- 发布产物应生成 AcroForm 或等价可回填层，同时保留 bindings.json。

## 保存与发布

“保存为新版本”执行：强制保存 -> 下载结果 -> 安全校验 -> 绑定校验 -> 固化版本 -> PDF 快照 -> 统一模型 -> 结构 Diff -> 质量检查。任一步失败都不能替换当前可用版本。

发布要求：版本 ready、PDF 快照成功、类型明确、fillable 至少一个有效绑定、无阻断告警。恢复历史版本必须复制为新草稿后重新走完整门禁。

## 并发与异常

- 同一模板草稿只有一个编辑租约；丢失租约后停止回调写入并转只读。
- 编辑服务故障保留上一正式版本和最后安全工作副本。
- 关闭/刷新页面时提示未提交工作副本，但不能把自动保存说成正式版本。

## 实现拆分

- 延续现有 `views/template/editor` 组件和 `useTemplateNativeEditor`。
- 新增 `TemplateBindingInspector`、`PdfFieldDesigner`、`TemplateCommitProgress`。
- 当前 POC “独立对象保存”必须接入正式草稿/版本状态机后才算完成。

## 验收

- G0/G1/G2 Spike 全通过；DOCX 代表样本还原度和字体门禁达标。
- 删除、重复、改写或非法嵌套 Content Control 会阻断发布。
- reference 模板不能创建业务文档。
- 保存失败不会影响上一 published 版本。

## 子任务

- P12-T1：完成 ONLYOFFICE 许可/插件/回调正式化。
- P12-T2：实现 DOCX Content Control 和 PDF 坐标设计器。
- P12-T3：实现提交状态机、质量检查、发布和恢复。
- P12-T4：建立代表样本、安全文件和故障矩阵 E2E。

