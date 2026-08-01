# ONLYOFFICE 字体挂载目录

> 导航：[仓库首页](../../../README.md) / [部署](../../README.md) / [ONLYOFFICE](../README.md) / 字体

本目录只承载开发或验收环境中已获合法授权的字体。`.gitignore` 会排除 `.ttf`、`.otf`、`.ttc` 等本机文件，只保留目录说明；字体二进制不得提交 Git。

## 使用方式

以下脚本命令均从仓库根目录执行：

1. 将授权字体放入本目录，或在 `deploy/compose/.env` 中设置 `DOCMIND_ONLYOFFICE_FONT_DIR` 指向本机字体目录。
2. 执行 `./scripts/dev/infra.sh editor-start` 启动编辑服务。
3. 字体发生变化后执行 `./scripts/dev/infra.sh editor-fonts` 重建字体索引并重启服务。
4. 使用 `docker exec docmind-onlyoffice fc-match "字体名"` 核验实际匹配结果。

生产环境应使用经法务确认、版本固定、编辑器与 PDF 渲染器一致的字体包。字体缺失或替代必须形成可阻断发布的还原度告警，不能静默通过验收。
