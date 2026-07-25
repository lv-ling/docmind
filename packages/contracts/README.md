# 契约包

存放 OpenAPI、JSON Schema、事件 Schema 和由契约生成的类型，是 Web、API、AI 服务之间数据结构的唯一事实源。

建议内部结构：`openapi`、`json-schema`、`events`、`generated`。生成目录禁止手工修改；不兼容契约变更需要版本升级和迁移说明。

