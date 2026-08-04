# P28 模型配置

## 页面定义

- 路由：`/settings/model`，对应原型 `#model-config`。
- 访问：Owner/Admin。
- 目标：配置允许的模型供应商、用途、密钥引用、超时与安全开关，不向浏览器回传真实密钥。

## 布局

- 配置列表：用途（抽取/审阅/对话）、供应商、模型、状态、最近测试、更新时间。
- 编辑表单：供应商、base URL、模型 ID、密钥输入、超时/重试、数据训练声明、启用状态。
- 操作：测试连接、保存、停用、轮换密钥；测试结果单独展示。

## 字段规则

- 供应商/协议从服务端 allowlist；不允许任意浏览器脚本或动态 Provider 类名。
- API Key 只允许写入，读取响应仅返回 `configured`、尾号、更新时间和 key ID。
- 自定义 endpoint 必须通过服务端 SSRF allowlist、HTTPS 和 DNS/IP 检查。
- Prompt 正文不在此页面直接自由编辑；Prompt 使用独立版本化配置和权限。
- 测试请求使用无敏感固定样例，不使用真实文档。

## 安全

- 密钥经 KMS/Secret 加密；日志、审计、错误、网络响应均不返回原值。
- 保存与测试都由 Spring Boot 发起，Web 不直接调用模型供应商。
- 页面离开/保存后清空输入框，不存 session/local storage。

## 验收

- 查看现有配置无法恢复 API Key。
- SSRF、明文 HTTP、内网地址和非法协议被拒绝。
- 测试成功不等于保存；保存失败不改变当前生效配置。

## 子任务

- P28-T1：定义 Provider allowlist、secret reference 和测试契约。
- P28-T2：实现配置列表/表单/测试状态。
- P28-T3：补密钥不回显、SSRF、轮换和审计测试。

