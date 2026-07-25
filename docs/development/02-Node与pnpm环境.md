# Node 与 pnpm 环境

## 固定版本

```text
Node.js: 24.14.1
pnpm: 10.13.1
```

Node 版本同时记录在 `.nvmrc` 和 `.node-version`，兼容 nvm、fnm、mise、asdf 等常用版本管理工具。pnpm 版本记录在根 `package.json` 的 `packageManager` 和 `engines` 字段。

项目启用了 `engine-strict=true`。版本不匹配时安装会失败，避免不同开发环境生成不一致的依赖和锁文件。

## 首次安装

使用 nvm：

```bash
nvm install 24.14.1
nvm use 24.14.1
corepack enable
corepack install --global pnpm@10.13.1
pnpm install
```

如果本机已经安装正确版本，只需：

```bash
node --version
pnpm --version
pnpm install
```

预期输出：

```text
v24.14.1
10.13.1
```

## 日常使用

进入项目后先确认版本：

```bash
node --version
pnpm --version
```

常用根命令：

```bash
pnpm dev
pnpm build
pnpm test
pnpm typecheck
pnpm check
```

## 版本升级

升级 Node 时必须同时修改 `.nvmrc`、`.node-version` 和 `package.json#engines.node`。升级 pnpm 时必须同时修改 `package.json#packageManager` 和 `package.json#engines.pnpm`，再执行 `pnpm install` 更新 `pnpm-lock.yaml`。版本升级需要单独提交，并运行 `pnpm check`。

