# `@docmind/ui`

> 导航：[仓库首页](../../README.md) / [共享包](../README.md) / UI 组件

`@docmind/ui` 提供无业务状态的 Vue 组件、设计令牌和无障碍基础能力。视觉方向采用“编辑出版工作台”：暖纸色画布、深蓝墨色和克制的朱红提示，兼顾文档阅读与业务状态辨识。

## 目录结构

```text
packages/ui/src/
├── components/    Button、TextField、Status、SplitPane
├── tokens.ts      可在 TypeScript 中消费的设计令牌
├── styles.css     全局变量与组件样式
└── index.ts       公共导出
```

## 当前组件

- `DmButton`：四种语义外观、三种尺寸、加载和禁用状态。
- `DmTextField`：标签、说明、错误、必填提示和完整 ARIA 关联。
- `DmStatus`：中性、信息、成功、警告、错误状态及可选礼貌播报。
- `DmSplitPane`：鼠标拖动、键盘调宽、左右收起和移动端纵向回退。
- `DOCMIND_DESIGN_TOKENS`：颜色、字体、间距和动效令牌。

应用入口需引入一次样式：

```ts
import '@docmind/ui/styles.css';
```

分栏组件使用受控属性；`ArrowLeft`/`ArrowRight` 每次调整 2%，按住 Shift 调整 10%，`Home`/`End` 跳至最小或最大宽度。

## 开发与验证

```bash
pnpm --filter @docmind/ui typecheck
pnpm --filter @docmind/ui test
pnpm --filter @docmind/ui build
```

原件预览、模板编辑和抽取复核等业务组件应留在 [`apps/web`](../../apps/web/README.md) 或 [`packages/editor`](../editor/README.md)，不能进入本包。
