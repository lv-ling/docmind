# DocMind Web 前端工程规范

## 1. 作用域与执行原则

本文件适用于 `apps/docmind-web/**`。所有新建、修改、迁移和审查前端代码的人员或 Agent 都必须遵守。

- 直接用户要求优先于本文件；如果两者冲突，应先说明冲突和影响。
- 本规范描述目标架构。现有扁平 `views`、大型单文件页面和全局样式属于待迁移代码，不代表新代码可以继续复制同类结构。
- 不得为了让无关文件“顺便符合规范”扩大任务范围。新代码必须合规；修改旧代码时，只在当前职责范围内渐进改善。
- 目录迁移与业务功能修改应尽量拆开，保证每一步都可审查、可验证、可回退。
- 样式实现统一采用 Tailwind CSS 作为原子 CSS 框架，并通过 Vite 构建集成，禁止在生产代码中使用 Tailwind Play CDN。除 Tailwind CSS 外，未经明确同意不新增其他样式框架或更换构建方案。

## 2. 技术基线

- 使用 Vue 3、TypeScript、Vite、Vue Router、Pinia 和 Vitest。
- Vue 组件使用 `<script setup lang="ts">` 和 Composition API。
- TypeScript 保持严格模式，不使用 `any` 逃避类型建模；确实未知的数据使用 `unknown` 并在边界处收窄。
- 使用 `pnpm` 和仓库锁文件，不混用 npm 或 yarn。
- 复用 `@/contracts`、`@/editor`、`@/ui` 等已有边界，不从 Server、Document AI 或仓库根目录直接导入源码。
- 相对导入 TypeScript 模块时延续项目现有 ESM 约定，保留 `.js` 后缀；Vue SFC 使用 `.vue` 后缀。

## 3. 目标目录与职责

```text
src/
├── api/                         HTTP 客户端与按领域划分的请求函数
├── components/                  全应用共享的业务无关或应用级组件
├── contracts/                   HTTP 请求、响应、事件与共享契约
├── editor/                      受控文档模型、序列化、校验和模板绑定
├── layouts/                     页面布局与应用外壳
├── router/
│   └── modules/                 按布局、权限和运行边界组织路由模块
├── stores/                      跨页面、跨路由的全局状态
├── styles/                      基础样式、设计令牌和 Tailwind CSS 入口
├── ui/                          无业务语义的设计系统基础组件
├── utils/                       与业务无关的纯工具
└── views/
    └── <domain>/
        ├── components/          领域内多个页面共享的组件
        └── <page>/
            ├── index.vue        路由页面入口
            ├── components/      仅当前页面使用的组件
            ├── composables/     当前页面的状态与副作用编排
            └── model/           页面表单模型、纯转换和校验
tests/
├── api/                         API 边界与错误处理测试
├── contracts/                   契约兼容性测试
├── editor/                      编辑器模型、序列化与校验测试
├── ui/                          设计系统组件测试
├── utils/                       通用工具测试
└── views/                       页面逻辑和关键交互测试，目录镜像 src/views
```

目标页面示例：

```text
views/
├── system/
│   ├── login/index.vue
│   └── not-found/index.vue
├── source/
│   ├── list/index.vue
│   └── detail/index.vue
├── schema/
│   └── list/index.vue
├── extraction/
│   ├── create/index.vue
│   └── review/index.vue
└── template/
    ├── list/index.vue
    └── editor/index.vue
```

### 3.1 依赖方向

- 路由页面可以依赖页面私有实现、领域共享组件、全局组件、Store、API、契约、编辑器、UI 和工具层。
- 页面私有目录不得被其他页面跨目录深层导入。出现复用时，将代码提升到最近的共同领域目录或真正的全局层。
- `ui`、`utils`、`contracts`、`api`、`stores` 和 `editor` 不得反向导入 `views`。
- 展示组件不得直接读取路由、调用 API 或修改全局 Store；由页面容器或 composable 注入 props，并通过 emits 报告用户意图。
- API 请求只能通过 `src/api` 发起。组件内不得直接使用 `fetch`，不得绕过 Server 访问私有对象或 Document AI。
- Pinia 仅保存跨页面、跨路由或需要集中管理的状态；单页表单、弹窗、分页、选中项和加载状态优先保留在页面 composable 中。

## 4. 页面与目录命名规则

### 4.1 目录命名

- 所有目录使用小写 `kebab-case`；禁止拼音、空格、下划线和含义不清的缩写。
- `views` 第一层是稳定业务领域，第二层是路由页面。使用 `views/schema/detail/index.vue`，不要使用 `views/schema-detail/index.vue`。
- `system` 只放登录、异常页、无权限页等系统级页面。Schema、抽取、模板等业务页面不得因“像配置”而放入 `system`。
- 页面内部只在确有职责时创建 `components`、`composables`、`model` 等目录，禁止为了形式创建空目录。
- 常规页面层级控制在 `views/<domain>/<page>/<kind>`。除复杂组件包外，避免继续嵌套多层 `components`。

### 4.2 页面命名

- 所有路由页面入口统一命名为 `index.vue`。
- 禁止继续在 `src/views` 根目录新增 `SomethingView.vue`。
- 页面目录名称表达页面职责，例如 `list`、`detail`、`create`、`edit`、`review`；同一领域内保持一致的 CRUD 词汇。
- URL、路由模块、`RouteName`、页面目录和业务术语应尽量一致。兼容旧 URL 时可以只调整代码目录，不得擅自破坏已有地址。
- 路由文件按布局、权限、守卫和运行边界组织，不要求与 `views` 的领域目录一一对应。
- 当前规模下，公共页面集中在 `system.ts`，共享工作台布局与权限的业务页面集中在 `workbench.ts`；可在同一文件中使用 `sourceRoutes`、`schemaRoutes` 等数组进行逻辑分组。
- 禁止为了目录对称或单个页面创建独立路由文件。仅当一个领域具有独立布局、独立权限或守卫、复杂嵌套路由、动态注册需求，或稳定增长到约 4～5 个路由时，才拆为独立模块。
- 拆分后的模块只导出稳定路由配置，由上层模块统一组合；页面业务逻辑不得写入路由配置文件。

### 4.3 组件与其他文件命名

- Vue 组件使用描述明确的 `PascalCase.vue`，例如 `SourceUploadDialog.vue`、`TemplateVersionStrip.vue`。
- 页面私有组件不使用 `View` 后缀；`Page` 或 `View` 只用于真正的路由容器，但本项目路由容器统一为 `index.vue`。
- 禁止 `Data.vue`、`Content.vue`、`Box.vue`、`Item.vue` 等缺少领域语义的名称。
- 简单组件使用单文件形式：`components/SourceTaskPanel.vue`。
- 只有组件拥有专属子组件、类型、测试或资源时才创建组件目录：`components/SourceUploadDialog/index.vue`。
- `components/index.ts` 是可选公共出口，不是组件成立的条件。只有存在多个稳定导出并能隐藏内部路径时才创建。
- composable 使用 `useXxx.ts`；纯转换文件使用明确名词或动词，例如 `schema-form.ts`、`collect-editable-blocks.ts`。
- 测试使用 `*.test.ts` 或 `*.test.tsx`，统一放在应用根目录 `tests/` 下，并按 `src/` 的领域和职责镜像组织。
- 禁止继续在 `src/` 中新增测试文件，也不得把不相关领域的测试合并到一个巨型测试文件。
- 测试文件名应能对应被测模块，例如 `src/utils/file.ts` 对应 `tests/utils/file.test.ts`。

## 5. 页面和组件拆分规则

### 5.1 路由页面职责

`views/<domain>/<page>/index.vue` 应是薄路由容器，只负责：

- 读取并校验路由参数。
- 连接页面级 composable。
- 组合主要区域组件。
- 处理页面级导航、权限和顶层加载/错误状态。

页面不应长期同时承担大型表单转换、轮询、SSE、对象 URL、第三方 SDK、复杂渲染算法和多个独立视觉区域。

### 5.2 必须评估拆分的信号

出现任一情况时必须评估拆分，并优先按职责拆分：

- 模板中存在三个及以上可独立命名的主要区域。
- 某个区域拥有独立的 props、事件、加载态、错误态或空状态。
- 同一文件包含两个及以上独立异步工作流或生命周期资源。
- 存在计时器、事件监听、SSE、对象 URL、iframe 或第三方 SDK 初始化与销毁。
- 一段业务转换或校验可以脱离 Vue 单独测试。
- 相同结构出现两次以上，或同一组合在多个页面复用。
- 修改局部 UI 必须理解整页大部分状态才能保证安全。

以下行数是维护性预警，不是机械拆分目标：

- 路由页面超过约 250 行时应说明未拆分原因。
- 普通展示组件超过约 300 行时应重新检查职责。
- composable 超过约 250 行或返回大量无分组状态时应按工作流拆分。

不得通过把整段逻辑原样移动到一个巨大 composable 来假装完成组件化。

### 5.3 Props 与事件

- props 使用类型化 `defineProps`，emits 使用类型化 `defineEmits`。
- props 表达数据和能力，emits 表达用户意图；禁止子组件直接修改父组件对象内部字段。
- 事件名称使用 `kebab-case`，例如 `select-version`、`submit-review`。
- 双向绑定只用于语义明确的受控值，并使用标准 `update:modelValue` 或具名 `v-model`。
- 插槽用于结构组合，不用于隐藏难以追踪的数据流。

## 6. TypeScript 与变量命名规则

### 6.1 基础命名

- 变量、函数、参数和 composable 返回字段使用 `camelCase`。
- 类型、接口、类、枚举和 Vue 组件使用 `PascalCase`；类型和接口不添加 `I`、`T` 等无意义前缀。
- 模块级不可变常量使用 `UPPER_SNAKE_CASE`，例如 `SOURCE_PAGE_SIZE`、`TERMINAL_STATUSES`。
- 数组使用复数名词：`sources`、`selectedFields`；集合使用 `xxxSet`；按 ID 索引的映射使用 `xxxById`。
- DOM 模板引用以 `Ref` 结尾，例如 `fileInputRef`、`editorHostRef`。
- 禁止 `data`、`info2`、`temp`、`obj`、`arr`、`flag`、`res` 等缺少上下文的名称。

### 6.2 布尔值与状态

- 布尔变量使用 `is`、`has`、`can`、`should` 前缀，例如 `isLoading`、`hasWarnings`、`canPublish`。
- 异步动作状态使用 `isLoadingSources`、`isSavingSchema` 等具体名称；一个页面存在多个工作流时禁止共用含义模糊的 `loading` 或 `saving`。
- 状态联合使用明确类型，例如 `type UploadStage = 'idle' | 'hashing' | 'uploading'`，不要用多个互相矛盾的布尔值模拟状态机。

### 6.3 函数命名

- 查询或派生使用 `get`、`find`、`format`、`parse`、`to`、`create` 等准确动词。
- 加载数据使用 `loadSources`、`loadTemplateDetail`，保存操作使用 `saveSchema`、`publishTemplateVersion`。
- DOM 或组件事件处理器使用 `handleXxx`，例如 `handleFileDrop`、`handleSubmit`。
- 订阅和资源函数成对命名，例如 `startPolling` / `stopPolling`、`createPreviewUrl` / `revokePreviewUrl`。
- 有副作用的函数名必须体现动作，不得伪装成普通 getter。
- 函数保持单一职责。超过三个布尔参数时改用具名对象参数。

### 6.4 类型边界

- 后端契约字段保持服务端定义的 `snake_case`，不要在请求边界随意改名。
- 页面 ViewModel、表单模型和组件 props 使用前端 `camelCase`，转换集中在 `model`、API 适配器或明确的映射函数中。
- 可辨识联合优先于松散对象；跨层公开类型不得依赖页面私有类型。
- 不使用非空断言掩盖缺失状态。路由查询、DOM 引用和 API 可空字段必须显式校验。

## 7. 导入与模块出口规则

- 跨领域或跨顶层目录导入使用 `@/` 别名，例如 `@/contracts`、`@/ui`。
- 同一页面目录内部可以使用相对导入，但避免超过两级的 `../../../`。出现深层回退通常说明代码位置不正确。
- 不得从其他领域页面的 `components`、`composables` 或 `model` 深层导入私有实现。
- `index.ts` 只暴露稳定公共 API。禁止为了缩短路径把目录中所有实现全部导出。
- 禁止形成循环依赖；底层模块不得通过 barrel 文件间接反向依赖页面层。
- import 顺序保持为：类型或核心项目边界、第三方依赖、项目内部绝对导入、当前模块相对导入；组间空一行。

## 8. 原子 CSS 与样式规则

### 8.1 总体原则

- 新页面和新组件应优先使用 Tailwind CSS 组合布局、间距、尺寸、排版、颜色、响应式和常见状态，减少重复编写局部 CSS。
- 复用 Tailwind CSS 标准工具类、项目主题配置和设计令牌；框架已有能力不得重复建设一套项目私有原子类。
- 原子类应保持职责单一、组合清晰。禁止为了追求“全原子化”而堆叠难以理解的类名或复制大段相同组合。
- 应用尚未接入 Tailwind CSS 时，应先完成统一的 Vite 构建配置、`--dm-*` 主题映射和验证，不得只在局部模板中使用无法生效的类名。

### 8.2 原子类使用和范围

- 直接使用 Tailwind CSS 提供的标准类名和变体规则，不再规定项目私有原子类前缀。
- 间距、字号、圆角、颜色和阴影必须优先映射到框架主题中的统一令牌或刻度；任意值语法只用于确无可复用令牌的特殊场景。
- 动态类名应使用可被框架构建工具静态识别的完整字符串或明确映射，禁止通过字符串拼接生成可能被构建阶段遗漏的类名。
- `dm-` 前缀保留给设计系统组件及其内部类；业务语义类使用领域前缀，例如 `source-task-panel`。

### 8.3 何时抽取语义类

以下任一情况可以并且应当使用自定义语义类，而不是继续堆叠原子类：

- 单个元素需要超过约 8 个原子类，已明显降低模板可读性。
- 同一组原子类组合重复三次以上。
- 需要复杂伪元素、后代选择器、关键帧、容器查询或多状态联动。
- 样式本身表达稳定业务语义，例如“当前文档任务面板”或“模板告警抽屉”。
- 需要作为完整组件边界统一维护响应式和可访问状态。

语义类采用领域化 BEM 风格：

```text
source-task-panel
source-task-panel__header
source-task-panel__actions
source-task-panel--empty
```

不要把整套原子声明复制进多个语义类。重复组合应优先变成组件或共享样式抽象。

### 8.4 样式存放

- 全局样式只允许放原子 CSS 框架入口、重置、基础元素、设计令牌和真正的全应用规则。
- 页面专属选择器不得写入全局入口 `src/styles/index.css`，应放在对应页面或组件目录。
- 页面或组件语义样式与实现就近存放，优先使用 SFC 的 `<style scoped>`；确需独立文件时放在对应页面或组件目录。
- `src/ui` 的样式属于设计系统层，不得包含 source、schema、template 等业务选择器。
- 修改旧全局样式时，应删除已经被替代的规则，禁止通过不断在文件尾追加更高优先级覆盖来完成设计调整。
- 禁止使用 `!important` 解决层叠冲突，除非覆盖不可控第三方样式，并在旁边说明原因。
- 内联 `style` 只用于运行时计算值或 CSS 自定义属性，例如动态进度和分栏比例；静态样式必须使用原子类或语义类。
- 颜色、间距、字号、圆角和阴影优先使用现有 `--dm-*` 令牌。新增视觉值前先判断是否应扩展令牌。

## 9. Vue 实现规则

- `<script setup>` 内部按“类型与导入 → props/emits → Store/路由 → state → computed → 纯函数 → handlers → watch/lifecycle”组织。
- computed 必须保持纯净，不发请求、不写 Store、不操作 DOM。
- 模板表达式保持简单；复杂过滤、格式化和条件判断移入 computed 或纯函数。
- `v-for` 必须使用稳定业务键，不使用数组索引作为可重排列表的 key。
- 所有计时器、事件监听、SSE、对象 URL、第三方编辑器和订阅都必须在卸载时清理。
- 不直接修改 props，不在展示组件中隐藏全局副作用。
- 禁止未经净化的 `v-html`。受控文档 HTML 只能经过现有 `editor` 安全策略处理。
- 加载、空、错误、无权限和禁用状态必须明确，异步按钮应防止重复提交。

## 10. 可访问性与交互规则

- 优先使用语义 HTML；能用 `button`、`a`、`input` 时不要用带点击事件的 `div`。
- 图标按钮必须有可访问名称；表单控件必须有关联 label。
- 自定义交互必须支持键盘和清晰焦点态。
- 状态更新根据重要程度使用恰当的 `aria-live`、`role="status"` 或 `role="alert"`，避免重复播报。
- 弹窗需要明确标题、`aria-modal`、关闭方式和合理焦点管理。
- 动画遵守 `prefers-reduced-motion`，颜色不是传达状态的唯一方式。

## 11. 数据、安全与本地存储

- 浏览器不得持久化模型密钥、敏感令牌映射、完整敏感文档、私有对象地址或未脱敏模型结果。
- access token 延续现有会话存储策略，不擅自改为长期本地存储。
- `localStorage` 只保存无敏感性的用户偏好，并使用包含用户和工作区作用域的稳定 key。
- 对象 URL 使用完必须回收；日志和错误提示不得泄露令牌、签名 URL 或敏感正文。
- API 变化先更新提供方的版本化契约，再更新 Web 契约、适配代码和边界测试。

## 12. 测试与验证

- 不要求每个路由页面或组件都创建测试文件。简单展示、静态组合和无独立行为的页面可以不写页面测试。
- 是否新增测试由风险和可观察行为决定，不能按页面数量机械创建，也不能为了目录对称创建空测试。
- 纯转换、解析、校验、状态机和复杂 computed 逻辑应提取成可独立测试的函数。
- composable 中的轮询、回退、资源释放和错误路径应有针对性测试。
- 组件测试关注用户可观察行为、props/emits、可访问状态和关键条件分支，不测试 Vue 内部实现。
- 修复缺陷必须补充能复现问题的测试；纯文件移动不得顺带改变行为。
- 所有测试统一存放在 `tests/`，并镜像被测源码的领域路径；允许同一职责下紧密相关的小型模块共享一个测试文件。
- 修改 TypeScript 或 Vue 代码后，至少在 `apps/docmind-web` 下运行：

```bash
pnpm format:check
pnpm typecheck
pnpm test
pnpm build
```

- 影响范围较大、准备交付或修改工程配置时运行：

```bash
pnpm check
```

- 仅修改 Markdown 或 Agent 规则时，不要求运行前端构建；必须检查 Markdown 格式、引用路径和最终 diff。
- 不得声称未实际执行的检查已经通过；失败时说明命令、错误和未完成风险。

## 13. 渐进迁移规则

- 禁止一次性重写整个前端。按领域和页面逐步迁移，优先处理职责最复杂的页面。
- 仅移动目录时保持 UI、路由名称、URL、接口调用和交互行为不变。
- 拆分组件时先识别输入、输出和副作用，再移动代码；不得通过复制产生新旧两套实现。
- 样式迁移应同时删除对应旧全局规则，防止新旧样式叠加。
- 新页面不得落回 `views` 根目录；新页面样式不得落回巨型全局文件。
- 当前任务没有要求架构迁移时，不得仅因发现旧文件超长而擅自实施大规模重构；应记录并在交付说明中提出后续建议。

## 14. Agent 开工与交付清单

开始前：

- 明确目标业务领域和页面。
- 阅读相关路由、API、契约、Store、页面和测试。
- 检查工作区已有改动并保护用户文件。
- 判断变更属于页面容器、页面私有、领域共享还是全局能力。

交付前：

- 确认没有新增扁平页面、跨领域私有导入或页面级全局样式。
- 确认命名、组件归属、资源清理、敏感数据和可访问状态符合本规范。
- 运行与改动相称的验证命令。
- 检查 `git diff`，只报告实际修改和实际验证结果。
