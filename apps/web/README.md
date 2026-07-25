# Web 应用

Vue 3 + TypeScript + Vite 用户端。负责原件只读预览、字段 Schema 配置、抽取结果复核、模板编辑、模板实例填写、编辑锁状态和文档对比。

建议内部结构：`src/api` 契约客户端、`src/features` 业务模块、`src/views` 页面、`src/router` 路由、`src/stores` 状态、`src/components` 应用组件。通用编辑能力放到 `packages/editor`，通用组件放到 `packages/ui`。

禁止在浏览器保存模型密钥、敏感令牌映射或绕过 API 直接访问对象存储私有对象。

