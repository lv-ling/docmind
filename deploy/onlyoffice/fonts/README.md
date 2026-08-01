# ONLYOFFICE 本地字体挂载目录

这个目录只承载开发/验收环境中已获合法授权的字体，不向仓库提交 `.ttf`、`.otf`、`.ttc` 等字体二进制。

1. 将获授权字体放入此目录，或在 `deploy/compose/.env` 设置 `DOCMIND_ONLYOFFICE_FONT_DIR` 指向本机字体目录。
2. 启动编辑服务：`./scripts/dev/infra.sh editor-start`。
3. 字体发生变化后执行：`./scripts/dev/infra.sh editor-fonts`。
4. 使用 `docker exec docmind-onlyoffice fc-match "字体名"` 核验实际匹配结果。

生产环境应使用经法务确认、版本固定且编辑器与 PDF 渲染器一致的字体包；字体缺失或替代必须形成可阻断发布的告警。
