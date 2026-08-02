# Web 的 Node 与 pnpm 环境

Node 工具链只属于 `apps/docmind-web`。仓库根目录和另外两个应用不读取 Web 的 `package.json`、锁文件或配置。

## 固定版本

```text
Node.js: 24.14.1
pnpm: 10.13.1
```

版本记录在 `apps/docmind-web/.nvmrc`、`.node-version` 和 `package.json`。该目录启用 `engine-strict=true`，版本不匹配时安装会失败。

## 首次安装

```bash
# 进入唯一使用 Node 工具链的 Web 应用
cd apps/docmind-web

# 安装并切换到仓库固定的 Node 版本
nvm install 24.14.1
nvm use 24.14.1

# 启用 Corepack 并安装仓库固定的 pnpm 版本
corepack enable
corepack install --global pnpm@10.13.1

# 严格按 Web 锁文件安装依赖
pnpm install --frozen-lockfile
```

也可以从仓库根目录执行 `./scripts/dev/web.sh install`。依赖只安装到 Web 目录，锁文件只更新 `apps/docmind-web/pnpm-lock.yaml`。

## 日常命令

```bash
# 进入 Web 应用；以下 pnpm 命令不能从仓库根目录执行
cd apps/docmind-web

# 启动开发服务器
pnpm dev

# 生成生产构建
pnpm build

# 运行全部前端测试
pnpm test

# 仅执行类型检查
pnpm typecheck

# 执行 Web 完整质量门禁
pnpm check
```

根目录没有 pnpm workspace，不能在根目录直接运行 pnpm 命令。跨应用验证使用 `./scripts/dev/check.sh`，它只是顺序调用各应用自己的门禁。

## 版本升级

升级 Node 时同时修改 Web 目录中的 `.nvmrc`、`.node-version` 和 `package.json#engines.node`。升级 pnpm 时同时修改 `package.json#packageManager` 与 `package.json#engines.pnpm`，再在 Web 目录执行 `pnpm install` 更新独立锁文件，最后执行 `pnpm check`。
