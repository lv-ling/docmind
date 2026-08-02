# DocMind Web

> 导航：[仓库首页](../../README.md) / [应用层](../README.md) / Web 应用

`docmind-web` 是 Vue 3 + TypeScript + Vite 用户端。它独立持有全部 Node 工具链、依赖锁和前端共享代码，不依赖根目录 manifest 或其他应用源码。

## 目录结构

```text
apps/docmind-web/
├── public/             品牌与静态资源
├── src/
│   ├── api/            HTTP 客户端
│   ├── contracts/      Web 使用的请求、响应和视图类型
│   ├── editor/         受控文档模型、模板绑定与安全 HTML
│   ├── ui/             UI 基础组件、样式和设计令牌
│   ├── components/     应用壳与应用级组件
│   ├── router/         登录和工作区路由守卫
│   ├── stores/         身份与工作区状态
│   ├── utils/          文件和 JSON 等通用工具
│   └── views/          原件、Schema、抽取和模板页面
├── package.json        Web 命令与依赖
├── pnpm-lock.yaml      Web 唯一 Node 依赖锁
└── vite/vitest/tsconfig/eslint 配置
```

`contracts`、`editor` 和 `ui` 是 Web 内部模块，不发布为跨应用包。Server 的 OpenAPI 由 Server 自己维护；Web 在 HTTP 边界上通过测试验证本地类型与实际响应的兼容性。

## 当前页面

- 登录和工作区选择。
- 原件列表、上传、不可变版本详情与只读预览。
- Schema 与敏感规则配置。
- 抽取任务创建、候选值和证据人工复核。
- 模板列表与双栏工作台，支持预览/微调、转换告警、后端 Diff、发布和生成新版本式回滚。

分栏偏好按用户和工作区保存在浏览器本地；文档模型、敏感值、访问令牌映射、对象存储地址和模型密钥不得写入本地存储。

## 开发与验证

在本目录执行：

```bash
# 严格按 pnpm-lock.yaml 安装依赖
pnpm install --frozen-lockfile

# 启动 Vite 开发服务器和热更新
pnpm dev

# 仅执行 Vue/TypeScript 类型检查
pnpm typecheck

# 单次运行全部 Vitest 测试
pnpm test

# 执行类型检查并生成生产构建
pnpm build

# 执行 lint、格式、类型、测试和构建完整门禁
pnpm check
```

也可从仓库任意目录使用 `./scripts/dev/web.sh <命令>`。开发服务器默认监听 `http://127.0.0.1:5173`，并把 `/api` 代理到 `http://127.0.0.1:8080`。

## 边界约束

- 不能在浏览器保存模型密钥、敏感令牌映射或完整敏感文档数据。
- 不能绕过 Server 直接访问 MinIO 私有对象或 Document AI 内部接口。
- 不从 `apps/docmind-server`、`apps/docmind-document-ai` 或仓库根目录导入源码和依赖。
- API 变化先由提供方更新版本化契约，再更新本地类型和边界测试。
