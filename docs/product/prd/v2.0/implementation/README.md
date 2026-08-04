# DocMind v2.0 技术方案与页面任务索引

本目录把 [PRD v2.0](../PRD.md) 和 [交互原型](../docmind-ui.html) 转换为可实施的技术方案与页面级任务。原型负责表达布局和主要入口，PRD 是业务规则、权限、状态与验收的最终依据；两者冲突时以 PRD 为准。

## 阅读顺序

1. [总体技术架构](00-technology-architecture.md)：技术栈、关键组件、PDF/Word/字段填写/文档对比方案。
2. [实施路线图](01-delivery-plan.md)：前置契约、分阶段依赖、质量门禁和任务顺序。
3. 按页面阅读下列任务文档。

## 页面任务

| 编号 | 页面 | 原型路由 | 文档 |
| --- | --- | --- | --- |
| P01 | 登录 | `#login` | [01-login](pages/01-login.md) |
| P02 | 全局应用壳与通知 | 全局 | [02-app-shell](pages/02-app-shell.md) |
| P03 | 工作台 | `#workbench` | [03-workbench](pages/03-workbench.md) |
| P04 | 文档资产列表与上传 | `#assets` | [04-asset-list](pages/04-asset-list.md) |
| P05 | 原件详情与预览 | `#asset-detail` | [05-asset-detail](pages/05-asset-detail.md) |
| P06 | 抽取方案列表 | `#schemas` | [06-schema-list](pages/06-schema-list.md) |
| P07 | 抽取方案编辑器 | `#schema-editor` | [07-schema-editor](pages/07-schema-editor.md) |
| P08 | 抽取任务/复核队列 | `#extraction`、`#extraction-review-queue` | [08-extraction-list](pages/08-extraction-list.md) |
| P09 | 新建抽取任务 | `#extraction-wizard` | [09-extraction-create](pages/09-extraction-create.md) |
| P10 | 抽取人工复核 | `#extraction-review` | [10-extraction-review](pages/10-extraction-review.md) |
| P11 | 模板中心 | `#templates` | [11-template-list](pages/11-template-list.md) |
| P12 | 模板设计器 | `#template-editor` | [12-template-editor](pages/12-template-editor.md) |
| P13 | 业务文档列表 | `#bizdocs` | [13-business-document-list](pages/13-business-document-list.md) |
| P14 | 新建业务文档 | `#bizdoc-wizard` | [14-business-document-create](pages/14-business-document-create.md) |
| P15 | 业务文档填写 | `#bizdoc-edit` | [15-business-document-edit](pages/15-business-document-edit.md) |
| P16 | 业务文档详情 | `#bizdoc-detail` | [16-business-document-detail](pages/16-business-document-detail.md) |
| P17 | 我的待办 | `#tasks` | [17-my-tasks](pages/17-my-tasks.md) |
| P18 | 审批详情 | `#approval-view` | [18-approval-detail](pages/18-approval-detail.md) |
| P19 | 外部提交管理 | `#external-mgmt` | [19-external-submission-list](pages/19-external-submission-list.md) |
| P20 | 外部填写 | `#external-fill` | [20-external-fill](pages/20-external-fill.md) |
| P21 | 文档对比列表 | `#compare` | [21-compare-list](pages/21-compare-list.md) |
| P22 | 新建文档对比 | `#compare-new` | [22-compare-create](pages/22-compare-create.md) |
| P23 | 对比结果与调整 | `#compare-result` | [23-compare-result](pages/23-compare-result.md) |
| P24 | 智能审阅列表 | `#review` | [24-review-list](pages/24-review-list.md) |
| P25 | 智能审阅与 AI 助手 | `#review-detail` | [25-review-detail](pages/25-review-detail.md) |
| P26 | 数据安全策略 | `#security` | [26-security-policy](pages/26-security-policy.md) |
| P27 | 成员与权限 | `#members` | [27-members](pages/27-members.md) |
| P28 | 模型配置 | `#model-config` | [28-model-config](pages/28-model-config.md) |
| P29 | 审计日志 | `#audit` | [29-audit-log](pages/29-audit-log.md) |
| P30 | 无权限与异常页 | `#no-permission`、未知路由 | [30-error-pages](pages/30-error-pages.md) |

## 统一完成标准

- 页面必须覆盖加载、空数据、无权限、失败、重试和正常状态。
- 写操作必须带幂等键；更新当前业务文档必须带内容校验标识与编辑租约。
- 原件、已发布模板版本、批准结果和审计快照不可原地修改。
- 页面不能直接访问 MinIO 或 Document AI，只调用 Spring Boot API。
- 文档正文、敏感明文、模型密钥、对象存储地址不得写入日志或持久化浏览器存储。
- 关键交互需要键盘、焦点、可访问名称和非颜色状态提示。

