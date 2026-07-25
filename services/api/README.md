# 业务 API

Java 17 + Spring Boot 3 业务服务。负责工作区和权限、原件/模板/实例生命周期、上传会话、敏感信息令牌化与回填、异步任务编排、编辑租约、审计和对外 API。

建议采用按领域组织的模块：`source`、`extraction`、`template`、`instance`、`collaboration`、`diff`、`identity`、`audit`，而不是只按 controller/service/mapper 技术层平铺。

数据库和对象存储只由本服务或其受控 Worker 访问；AI 服务不能绕过本服务读取租户数据。

