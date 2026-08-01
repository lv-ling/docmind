# UI 包

共享 UI 组件、设计令牌、图标封装和无障碍基础能力。视觉方向采用“编辑出版工作台”：暖纸色画布、深蓝墨色和克制的朱红提示，兼顾文档阅读的沉静感与业务状态辨识度。

仅包含无业务状态的展示组件；原件预览、模板编辑、抽取复核等业务组件留在 `apps/web` 或 `packages/editor`。

## 当前能力

- `DmButton`：四种语义外观、三种尺寸、加载和禁用状态。
- `DmTextField`：标签、说明、错误、必填提示和完整 ARIA 关联。
- `DmStatus`：中性、信息、成功、警告、错误状态，可选择礼貌播报。
- `DmSplitPane`：鼠标拖动、键盘调宽、左右收起、移动端纵向回退。
- `DOCMIND_DESIGN_TOKENS`：颜色、字体、间距和动效令牌。

应用入口需引入一次样式：

```ts
import '@docmind/ui/styles.css';
```

分栏组件使用受控属性；`ArrowLeft`/`ArrowRight` 每次调整 2%，按住 Shift 调整 10%，`Home`/`End` 跳至最小或最大宽度。
