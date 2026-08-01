# Web 应用

> 导航：[仓库首页](../../README.md) / [应用层](../README.md) / Web 应用

`@docmind/web` 是 Vue 3 + TypeScript + Vite 用户端，负责身份会话、工作区导航、原件管理、Schema 配置、抽取复核和模板工作台。浏览器只处理权限过滤后的视图数据，业务事实与敏感映射由 API 持有。

## 目录结构

```text
apps/web/
├── public/          品牌与静态资源
└── src/
    ├── api/         基于 @docmind/contracts 的 HTTP 客户端
    ├── components/  应用壳与应用级组件
    ├── router/      登录和工作区路由守卫
    ├── stores/      身份与工作区状态
    ├── utils/       文件和 JSON 等通用工具
    └── views/       原件、Schema、抽取和模板页面
```

可跨页面复用的受控文档能力放在 [`packages/editor`](../../packages/editor/README.md)，无业务状态的组件放在 [`packages/ui`](../../packages/ui/README.md)，请求与响应类型来自 [`packages/contracts`](../../packages/contracts/README.md)。

## 当前页面

- 登录和工作区选择。
- 原件列表、上传、不可变版本详情与只读预览。
- Schema 与敏感规则配置。
- 抽取任务创建、候选值和证据人工复核。
- 模板列表与双栏工作台，支持预览/微调、转换告警、后端 Diff、发布和生成新版本式回滚。

分栏偏好按用户和工作区保存在浏览器本地；文档模型、敏感值、访问令牌映射、对象存储地址和模型密钥不得写入本地存储。会话失效时清除访问令牌并返回登录页，登录后只恢复当前工作区内路径。

## 开发与验证

从仓库根目录执行：

```bash
pnpm --filter @docmind/web dev
pnpm --filter @docmind/web typecheck
pnpm --filter @docmind/web test
pnpm --filter @docmind/web build
```

开发服务器默认监听 `http://127.0.0.1:5173`，`/api` 请求代理到 `http://127.0.0.1:8080`。联调前先启动本地基础设施和[业务 API](../../services/api/README.md)；涉及抽取和文档解析时还需启动 [AI 服务](../../services/ai/README.md)。

## 边界约束

- 不能在浏览器保存模型密钥、敏感令牌映射或完整敏感文档数据。
- 不能绕过 API 直接访问 MinIO 私有对象或 AI 内部接口。
- 页面不得复制共享包中的契约、受控文档模型或通用组件实现。
