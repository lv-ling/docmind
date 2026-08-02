# DocMind Login Design QA

## Comparison Target

- Source visual truth: `C:\Users\260713201\Downloads\docmind_workspace (1).html`, `#login-layout`.
- Source screenshot: `D:\personal-projects\docmind\apps\docmind-web\login-prototype-1440x900.png`.
- Implementation screenshot: `D:\personal-projects\docmind\apps\docmind-web\login-implementation-final-1440x900.png`.
- Combined comparison: `D:\personal-projects\docmind\apps\docmind-web\login-comparison-1440x900.png` (source left, implementation right).
- Viewport: 1440 × 900 CSS px.
- Pixel dimensions: source 1440 × 900 px; implementation 1440 × 900 px. Browser captures were normalized to the CSS viewport despite the source tab initially reporting DPR 1.5; the implementation tab reported DPR 1.
- State: desktop, light theme, initial login form after reveal animations completed.

## Full-view Comparison Evidence

The source HTML and Vue implementation were served locally and captured in the in-app browser at the same viewport. They were then placed in one 2880 × 900 comparison image and inspected together.

- Root: 1440 × 900 px in both captures.
- Grid tracks: 720 px / 720 px in both captures.
- Brand and login panels: 720 × 900 px each.
- Brand panel inset: 64 px; logo, headline, tagline, workflow, and footer align to the source.
- Login form: 340 px wide at x = 910 px and y ≈ 381 px in both captures.
- Account input: 340 × 41.33 px in the implementation, matching the prototype control dimensions.
- Zinc/indigo palette, Lucide stroke style, typography hierarchy, border radii, divider, reveal sequence, and AI ping treatment match the source.

## Focused Region Evidence

- Brand area: logo mark, 36 px headline, uppercase product label, and footer placement match visibly.
- Workflow: 28 px circular nodes, 24 px icon-to-copy gap, 40 px step spacing, connecting gradient, indigo active node, and restrained ping animation match visibly.
- Login form: badge, 24 px title, labels, 340 px controls, 20 px vertical rhythm, 40 px submit button, and security divider match visibly.
- Password control: the added eye button occupies a 28 × 28 px transparent trailing control and preserves the source input height and width.
- No separate crop was required because every critical surface remains legible in the 2880 × 900 combined image; the form and workflow measurements provide the focused evidence.

## Intentional Product Differences

- `华东数据节点` was removed from the enterprise badge per the current product requirement.
- The prototype's prefilled `password` value was removed so production login starts empty and does not expose mock credentials.
- A Lucide eye/eye-off control was added per the current product requirement. It is page-private because the shared text field has no trailing-adornment contract and no other current screen requires this behavior.

## Interaction Verification

- The account and password controls expose accessible labels.
- The password toggle changes the input from `password` to `text` and back without changing its value; accessible names switch between `显示密码` and `隐藏密码`.
- The form calls the existing real `auth.login(email, password)` action, then the existing real `workspace.load()` action.
- Duplicate submission is ignored while authentication is pending.
- The submit button enters `验证中...`, disables repeated submission, then retains the 700 ms exit fade before navigation.
- The successful path navigates to `/workbench/source/list`.
- Browser console errors: none.

## Findings

No actionable P0, P1, or P2 mismatch remains. The only visible source differences are the three explicitly requested product changes listed above.

## Comparison History

1. The previous QA pass was blocked because the local source HTML had not been captured.
2. The source prototype was served locally and captured at 1440 × 900.
3. The implementation was recaptured at the same viewport after removing the stale data-node copy and mock password, and after adding the eye control.
4. The captures were combined and compared in one image. Panel proportions, major alignment, typography, spacing, colors, icons, and copy now pass; no further P0/P1/P2 visual correction was identified.

## Implementation Checklist

- [x] 50/50 desktop split.
- [x] Brand area hierarchy and spacing.
- [x] Three-stage workflow and restrained AI animation.
- [x] 340 px login form and prototype spacing rhythm.
- [x] Real auth and workspace store integration.
- [x] Empty production credentials with no mock branch.
- [x] Accessible password visibility toggle.
- [x] Loading, duplicate-submit prevention, exit fade, and navigation tests.
- [x] Reduced-motion handling.
- [x] Browser-rendered source, implementation, and combined comparison evidence.
- [x] Browser console check.

## Follow-up Polish

No additional visual polish is recommended; changing the layout or styling further would move away from the supplied prototype.

Login QA result: passed

---

# DocMind Workspace Design System Alignment QA

## Comparison target

- Source visual truth: `C:\Users\260713201\Downloads\docmind_premium_workspace (3).html`, Workspace view.
- Source screenshot: `docs/visual-alignment/prototype-workspace.png`.
- Pre-alignment screenshot: `docs/visual-alignment/vue-workspace-after.png`.
- Implementation screenshot: `docs/visual-alignment/vue-workspace-design-system.png`.
- Viewport: 1440 x 900 CSS pixels.
- Source and implementation images: 1440 x 900 pixels, normalized at 1x density.
- State: authenticated Workspace overview, light theme, default card state after reveal animation.

## Full-view comparison evidence

- The previously accepted 220 px sidebar, 48 px header, 24 px page padding, 8/4 content grid, card geometry, information hierarchy, content order, and Zinc/Indigo color system remain unchanged.
- The primary and AI columns retain the same origin, width, vertical rhythm, and card dimensions after the icon and control pass.
- Standard Workspace controls now share the prototype's 30 px height, 12 px label, 6 px radius, 12 px horizontal padding, 14 px icon, and 6 px icon-label gap.

## Focused region comparison evidence

- Sidebar: Lucide `brain-circuit`, `chevrons-up-down`, `layout-grid`, `files`, `cpu`, `check-square`, `layers`, `sliders-horizontal`, and `settings` match the prototype icon choices. Icons remain 12/14/16 px by context with 1.8 stroke width.
- Workspace actions: `list-checks` and `play` replace semantic approximations. Their optical baselines now align with the 12 px labels through a shared inline-flex label wrapper.
- Review cards: `file-text`, `file`, `check-circle-2`, `bot`, `arrow-right`, and `shield-alert` match the source. Compact action controls use the same control height, radius, padding, gap, and icon scale without changing the card layout.
- Pipeline and AI insights: `loader-2`, `activity`, `lightbulb`, `trending-up`, and `chevron-right` match the prototype; the loader retains the restrained spin state.

## Findings

- No actionable P0, P1, or P2 mismatch remains.
- P3: Windows/browser font antialiasing may produce subpixel differences in small Chinese labels; it does not change geometry or hierarchy.

## Required fidelity surfaces

- Fonts and typography: existing Inter/system Chinese family and the accepted size scale remain unchanged. Button labels use 12 px, medium weight, and a centered one-line baseline; navigation headings and active labels now match the prototype weights and tracking.
- Spacing and layout rhythm: no page or card structure changed. Icon-label gaps and button internals were normalized without shifting the accepted grid.
- Colors and visual tokens: Zinc, Brand Indigo, Amber, and Emerald usage remains unchanged; navigation icon colors match active/rest prototype states.
- Image and icon fidelity: the screen has no raster content. Hand-authored icon paths were removed from `AppIcon`; all visible Workspace icons now render from `lucide-vue-next` with a 1.8 stroke width.
- Copy and content: unchanged.

## Comparison history

1. Before: semantic custom SVG approximations were used for several icons; the review-queue and continue-review actions did not use the prototype's `list-checks` and `play` icons, and the shared button label wrapper did not guarantee a consistent icon-text baseline.
2. Final: exact Lucide mappings, context-specific 12/14/16 px sizes, 1.8 stroke width, standardized 30 px controls, and aligned navigation weights were applied. Same-state 1440 x 900 comparison shows no layout regression or actionable visual mismatch.

## Interaction verification

- The existing authentication flow reached `/workbench/overview` without auth or API changes.
- The hidden review action remains available and opened the existing review dialog.
- Closing the dialog restored the verified overview state.
- No application runtime error surfaced during the verified route and interaction path.

Workspace QA result: passed

---

# DocMind Full Product Pages Design QA

## Comparison target

- Source visual truth: `C:\Users\260713201\Downloads\docmind_premium_workspace (3).html`, Document Center, AI Processing Center, Review Center, Review Workspace, Template Center, and Config Center views.
- Source screenshots: `docs/visual-full-implementation/prototype-*.png`.
- Implementation screenshots: `docs/visual-full-implementation/source-list.png`, `source-detail.png`, `ai-processing.png`, `review-queue.png`, `review-workspace.png`, `template-list.png`, and `schema-list.png`.
- Viewport: 1280 x 720 CSS pixels.
- Pixel dimensions: every source and implementation capture is 1280 x 720 pixels at the in-app browser's normalized 1x capture density.
- State: authenticated product workspace, light theme, animations settled. The Config Center implementation was captured on the Sensitive Rules tab so its business state matches the prototype's global-rules view.

## Full-view comparison evidence

Each prototype capture and its implementation capture were opened together in the same comparison input at the same viewport and state.

- All standard pages retain the accepted 220 px sidebar, 48 px global header, 24 px content padding, 1600 px maximum content width, Zinc surfaces, Indigo AI accents, compact radii, and 12/14/16 px Lucide hierarchy.
- Document Center preserves the prototype's header, category-tab strip, five-column document object register, AI status, confidence, and update-time alignment. Search, upload, additional realistic documents, and filters extend the requested core task without changing the row geometry.
- AI Processing retains the two-column running-task grid, status/progress block, live Indigo indicator, and four-stage pipeline timeline. Queued, completed, and failed states reuse the same geometry.
- Review Center retains the high-risk-first information hierarchy, amber intervention stripe, confidence/category/time metadata, risk reason, AI suggestion, and compact actions. The second high-risk item is realistic task data using the same card contract.
- Review Workspace matches the prototype's fixed full-viewport mode and 42% / 28% / 30% three-column tracks. Original evidence, structured fields, AI pipeline, semantic summary, risk rule comparison, evidence quotation, and AI question input remain aligned.
- Template Center retains the three-card grid, icon/status header, title/description hierarchy, field count and application statistics, and restrained card elevation.
- Config Center's Sensitive Rules state preserves the prototype's threshold cards, sliders, sensitive-rule rows, semantic colors, and 900 px focused work area. Schema management is exposed as the requested adjacent tab rather than merged into the rules surface.
- Document Detail has no dedicated visual section in the supplied prototype. It was checked against the accepted Workspace system baseline: document preview is the dominant pane, with document facts, AI history, template relation, and version history contained in a compact right rail.

## Focused region comparison evidence

- Document rows: 13 px document titles, 11 px secondary metadata, compact status pills, mono confidence, and right-aligned time preserve the source density.
- Pipeline cards: progress tracks, state icons, connector rules, active-stage Indigo surface, and success/warning colors match the prototype after the semantic token bridge correction.
- Review Workspace: header height, pane proportions, source-document typography, active evidence marks, field confidence labels, amber intervention card, and bottom AI input were compared at readable scale; no separate crop was required.
- Template cards: the 3-up layout, icon boxes, status placement, text wrapping, stat panel, and inter-card gaps are visibly equivalent.
- Config rules: both threshold controls and sensitive-rule rows remain readable in the full comparison. The implementation adds only the requested configuration boundary tabs.
- Image and asset fidelity: these product views contain no raster imagery. Every visible icon is rendered from `lucide-vue-next` through `AppIcon` at stroke width 1.8; no custom SVG, CSS illustration, emoji, or placeholder image is used.

## Findings

- No actionable P0, P1, or P2 mismatch remains.
- P3: the supplied Document Center prototype includes only one example row, while the functional implementation includes four realistic rows to exercise search, filtering, and status variation. The row contract itself matches.
- P3: the Config Center adds a 35 px `DmTabs` boundary above the prototype rules content so Schema management and Sensitive Rules remain separate, as required. This intentionally shifts the first rule card downward without changing its internal design.
- P3: small Chinese text may show subpixel antialiasing differences between source and Vue captures; geometry and hierarchy are unaffected.

## Required fidelity surfaces

- Fonts and typography: Inter/system Chinese, the existing serif document font, mono measurements, weight hierarchy, line height, tracking, truncation, and compact label density were checked across all seven routes.
- Spacing and layout rhythm: 220/48 shell geometry, 64 px page headers, 24 px page padding, 20/24 px section gaps, card padding, 8 px radii, subtle shadows, and review-pane percentages were checked.
- Colors and visual tokens: Zinc and Brand Indigo continue to resolve through the existing token bridge. Emerald, Amber, and Red utility scales now derive from the existing success, warning, and danger tokens instead of introducing another palette.
- Image quality and asset fidelity: no source raster assets exist on these screens. Official Lucide icons remain sharp and consistent at 1x capture density.
- Copy and content: all prototype-specific labels, document names, pipeline states, risks, evidence, template descriptions, and configuration explanations are preserved or extended with realistic product data.

## Comparison history

1. Initial implementation captures established route coverage and interaction behavior at 1280 x 720.
2. The first visual comparison found a P2 semantic-state drift: Tailwind's reset removed Amber/Emerald/Red utility scales, making review risk borders and completed pipeline states too neutral.
3. The existing Design Token Bridge was extended so semantic utility scales derive from `--dm-color-success`, `--dm-color-warning`, and `--dm-color-danger`; no independent color system was added.
4. Standard page headers were corrected to a border-box 64 px height, removing the content-box padding expansion and restoring the prototype's top rhythm.
5. All implementation views were recaptured and compared with their source captures in the same inputs. The earlier semantic-state and header-height findings are resolved; no actionable P0/P1/P2 issue remains.

## Interaction verification

- Document Center category tabs, local search/filter surface, document navigation, and upload entry are functional.
- AI Processing status tabs reveal running, queued, completed, and failed task states.
- Review Center actions open the Review Workspace; quick archive updates the visible queue and emits a shared toast.
- Review Workspace field selection highlights the matching source evidence; AI correction updates the field to 20%; confirm/archive returns to the queue.
- Template creation opens a functional modal and creates a demo draft confirmation without changing the real API contract.
- Config Center tabs, field editing/add/remove actions, thresholds, sensitive-rule toggles, draft save, and publish feedback are functional.
- Browser console check: no application warnings or errors; only Vite connection and hot-update debug messages were present.

## Implementation checklist

- [x] Seven requested routes implemented.
- [x] Page-private ViewModels keep realistic demo data out of Vue templates.
- [x] Existing auth, API modules, and Pinia store boundaries unchanged.
- [x] Existing AppIcon, DmButton, DmStatus, DmProgress, DmTabs, and Toast system reused.
- [x] Tailwind-first implementation with no new scoped CSS.
- [x] Same-viewport source and implementation screenshots captured and compared.
- [x] Primary interactions exercised in the in-app browser.
- [x] Browser console checked.

## Follow-up polish

No blocking follow-up is required. A later product-data pass can replace the page-private demo ViewModels with API-backed adapters without changing the visual component contracts.

final result: passed

---

# DocMind Design System Enforcement QA

## Comparison target

- Source visual truth: `C:\Users\260713201\Downloads\docmind_premium_workspace (3).html`, Workspace Golden Reference.
- Current-run source screenshot: `docs/design-system-enforcement/00-prototype-workspace.png`.
- Current-run implementation screenshots: `docs/design-system-enforcement/01-workspace.png` through `07-config-center.png`.
- Combined comparison: `docs/design-system-enforcement/workspace-comparison.png`.
- Seven-page contact sheet: `docs/design-system-enforcement/all-pages-contact-sheet.png`.
- Viewport: 1280 x 720 CSS pixels.
- Pixel dimensions: every accepted screenshot is 1280 x 720 pixels. The source rendered at DPR 1; the implementation rendered at DPR 1.5 and the in-app browser normalized its capture to the CSS viewport, so no additional density scaling was used.
- State: authenticated desktop workspace, light theme, default route tabs, async content settled. Review Workspace uses the requested three-pane document-review state.

## Full-view comparison evidence

- The current-run HTML source and Vue Workspace were captured at the same 1280 x 720 viewport, placed in one comparison image, and inspected together.
- The 220 px sidebar, 48 px global header, 24 px page inset, main/AI column weight, card dimensions, information order, Zinc surfaces, and Brand Indigo emphasis remain aligned with the Golden Reference.
- All seven implementation routes were captured in one clean browser session. Page headers, section spacing, card radii, compact shadows, control density, navigation states, and typography read as one product system.
- Review Workspace preserves the full-width 42% / 28% / 30% three-pane layout and does not inherit the standard page padding, matching its specialized product boundary.

## Focused region comparison evidence

- Buttons: standard actions render through `DmButton` with 30 px height, 12 px medium label, 6 px radius, 12 px horizontal padding, 6 px icon gap, and 14 px `AppIcon` geometry. Composite document/schema/template/task objects render through `DmInteractiveSurface` so their multi-line business layout is preserved.
- Icons: business pages import no icon library directly. `AppIcon` is the only Lucide gateway and applies 1.8 stroke width with 12/14/16 px contextual sizing.
- Forms: visible text, search, password, number, textarea, range, file, select, and checkbox controls resolve through the shared UI primitives. Focus borders, radii, typography, disabled states, and compact heights are consistent.
- Tabs: the first Document Center capture exposed a browser-native scrollbar-arrow artifact at the right edge of the tab strip. `DmTabs` now hides its scrollbar through Tailwind utilities while retaining keyboard scrolling and the existing visual contract. The accepted recapture shows the artifact removed.
- No raster imagery exists in the audited product views. Lucide vectors remain sharp; no handwritten SVG, CSS icon drawing, emoji icon, or text-glyph navigation control appears in the business-page sources.

## Findings

- No actionable P0, P1, or P2 Design System inconsistency remains at the audited desktop viewport.
- P3: mobile/responsive parity cannot be claimed from the supplied desktop-only visual truth; a separate reference-backed responsive pass remains outside this scope.
- P3: template-editor legacy external CSS still styles its specialized document canvas and version/audit surfaces. Its interactive elements now render through the shared primitives, but a future editor-specific visual reference would be needed before removing that specialized stylesheet.

## Required fidelity surfaces

- Fonts and typography: Inter/system Chinese family, page/card hierarchy, compact metadata, button labels, line height, truncation, and mono data treatment were checked on all seven routes.
- Spacing and layout rhythm: shell geometry, 24 px page/section rhythm, 16-20 px card padding, 8 px card radii, 8 px control gaps, and review-pane proportions were checked.
- Colors and visual tokens: Zinc, Brand Indigo, Emerald, Amber, Red, focus rings, borders, and shadows continue through the existing token bridge. No new color family was added.
- Image quality and asset fidelity: there are no raster assets on these screens. All icons render from Lucide through `AppIcon` at stroke width 1.8.
- Copy and content: page structure, labels, business data, information order, and action order remain unchanged.

## Comparison history

1. The enforcement scan found raw business-page buttons, native form controls, direct Lucide imports in the login components, and composite interactive cards without a shared semantic surface.
2. Standard actions were moved to `DmButton`; composite object interactions were moved to `DmInteractiveSurface`; form controls were moved to `DmInput`, `DmTextarea`, `DmRange`, `DmFileInput`, `DmSelect`, `DmCheckbox`, and `DmTextField`.
3. Login icons were routed through `AppIcon`, preserving the accepted login layout and its password visibility control.
4. A source-level Vitest guard now fails if views/components/layouts introduce raw `button`, `a`, `input`, `select`, `textarea`, `svg`, or direct `lucide-vue-next` usage outside `AppIcon`.
5. The first Document Center screenshot found a P2 scrollbar-arrow artifact in `DmTabs`; the scrollbar was hidden with Tailwind utilities and the page was recaptured at the same state and viewport.
6. The post-fix seven-route screenshots and browser console were inspected. No P0/P1/P2 issue or runtime warning/error remained.

## Interaction verification

- Login completed through the existing local authentication path; auth, API contracts, and store boundaries were not changed.
- All seven authenticated routes loaded and exposed the expected semantic controls in DOM snapshots.
- Inputs, selects, checkboxes, ranges, file input, password visibility, tabs, standard actions, and composite object actions retain keyboard-operable native semantics through shared components.
- Browser console warnings/errors: none in the final seven-route session.

## Implementation checklist

- [x] Raw business-page interactive tag scan returns zero violations.
- [x] Direct Lucide imports outside `AppIcon` return zero violations.
- [x] Standard operations use `DmButton`.
- [x] Composite business-object interactions use `DmInteractiveSurface`.
- [x] Shared form primitives cover visible input types.
- [x] Automated Design System enforcement test added.
- [x] Same-viewport source and implementation comparison captured.
- [x] Seven authenticated page screenshots captured and inspected.
- [x] P2 tab scrollbar artifact fixed and recaptured.
- [x] Typecheck, ESLint, unit tests, production build, and clean browser console verified.

## Follow-up polish

No desktop Design System blocker remains. Responsive coverage should be handled only when a mobile/tablet reference is supplied.

final result: passed

---

# DocMind Global Design System Consistency Audit

## Comparison target

- Source visual truth: the accepted Workspace Golden Reference in `docs/design-system-audit/01-workspace-before.png`, plus the seven same-state pre-audit captures in `docs/design-system-audit/*-before.png`.
- Implementation screenshots: `docs/design-system-audit/01-workspace-after.png` through `07-config-after.png`.
- Viewport: 1280 x 720 CSS pixels.
- Pixel dimensions: every before/after capture is 1280 x 720 pixels at the in-app browser's normalized 1x capture density.
- State: authenticated product workspace, light theme, default tabs, animations settled. Review Workspace uses its fixed three-pane route state.

## Full-view comparison evidence

All seven before captures and all seven implementation captures were opened and inspected. Workspace, Review Workspace, Config Center, and Review Queue before/after pairs were additionally opened together in the same comparison inputs because they contain the highest-density controls and icon alignment surfaces.

- The accepted 220 px sidebar, 48 px global header, 24 px page padding, 24 px major section gap, 8 px card radius, Zinc surfaces, Brand Indigo emphasis, and compact shadow treatment remain unchanged.
- All standard action buttons now resolve through `DmButton`: 30 px height, 12 px label, 6 px radius, 12 px horizontal padding, 6 px icon gap, and 14 px button icon.
- Icon-only actions use the same component contract at 30 x 30 px. The review back control, modal close controls, notification/search/sidebar actions, schema add/remove actions, and AI submit action no longer carry page-private button geometry.
- Composite document rows, template cards, schema choices, version rows, and pipeline task cards remain semantic interactive cards. They intentionally retain their business-object layout instead of being forced into the action-button component.
- Config Center Select and Checkbox controls now reuse `DmSelect` and `DmCheckbox`; the Schema grid, Sensitive Rules cards, labels, and current business state remain unchanged.

## Focused region comparison evidence

- Review Workspace: the 42% / 28% / 30% pane ratio, source evidence, field hierarchy, and AI analysis cards are unchanged. Risk actions moved from 28 px / 11 px local controls to the shared 30 px / 12 px button system without changing the field card or evidence layout.
- Config Center: native selects and checkboxes were replaced by shared controls. The 110 px type column, 80 px required column, row height, field order, and footer actions remain aligned; icon-only add/delete controls now share the 30 px system.
- Review Queue and AI Processing: dark primary actions no longer depend on Tailwind `!important` overrides. Their visible size, placement, content, and hierarchy match the pre-audit captures through the shared `dark` variant.
- Global shell: exact Lucide icons remain at 1.8 stroke width. The sidebar footer now uses the semantically correct `log-out` icon instead of `settings`; mobile navigation uses Lucide `menu` instead of CSS line art.
- Image quality: these views contain no raster imagery. Every visible icon uses `AppIcon` backed by `lucide-vue-next`; no handwritten SVG, text glyph icon, emoji, or CSS-drawn icon remains in the audited action controls.

## Findings

- No actionable P0, P1, or P2 consistency issue remains at the audited desktop viewport.
- P3: the UI library still contains specialized splitter controls and page-private interactive-card surfaces. They are not action buttons and preserve their own accessible interaction geometry by design.
- P3: responsive/mobile visual parity was not part of the supplied desktop Golden Reference; desktop route coverage is complete, while a separate responsive QA pass would be needed before claiming mobile parity.

## Required fidelity surfaces

- Fonts and typography: Inter/system Chinese, page and card hierarchy, 12 px button labels, line height, label weight, truncation, and compact metadata density were checked across all routes.
- Spacing and layout rhythm: shell dimensions, 24 px page/section rhythm, card padding, 8 px radii, row density, button internals, and three-pane proportions were checked.
- Colors and tokens: Zinc, Brand Indigo, Amber, Emerald, Red, border, focus, and shadow states continue to resolve through the existing token bridge. No new color family was introduced.
- Image quality and asset fidelity: no raster assets are present; Lucide icons render consistently through `AppIcon` with stroke width 1.8.
- Copy and content: page structure, information hierarchy, business copy, data values, and action order are unchanged.

## Comparison history

1. The pre-audit screenshots identified page-private 28 px / 11 px review actions, native configuration controls, raw icon buttons, a text-glyph modal close control, and business-page `!important` button overrides.
2. `DmButton` gained system variants for dark, accent, accent ghost, danger ghost, and icon-only actions; the base 30/12/6/12/14/6 geometry remains the single source of truth.
3. Shared `DmSelect` and `DmCheckbox` controls replaced the visible Config Center and Document Center native variants. Exact Lucide mappings were extended for menu and toast states.
4. The seven routes were recaptured at 1280 x 720 and visually compared. No layout, content, hierarchy, or card-regression finding remained.

## Interaction verification

- Document Center filter expands and exposes the shared file-type select and reset action.
- Template Center opens and closes the create-template dialog through shared action and icon-only controls.
- Config Center switches to Sensitive Rules and exposes the shared checkbox cards.
- All seven authenticated routes render successfully in a clean browser session.
- Browser console check: no warning or error in the clean seven-route regression session.

## Implementation checklist

- [x] Standard actions use `DmButton` and shared variants.
- [x] Icon-only actions use the 30 x 30 shared button contract.
- [x] Button icons inherit 14 px size and 1.8 Lucide stroke width.
- [x] No business-page `!important` button overrides remain.
- [x] Visible Select and Checkbox variants use shared UI components.
- [x] Composite interactive cards preserve page structure and information hierarchy.
- [x] Seven same-viewport after screenshots captured and inspected.
- [x] Focused before/after comparisons completed for the densest control surfaces.
- [x] Clean browser console verified.

## Follow-up polish

No desktop visual blocker remains. Responsive/mobile alignment should be treated as a separate reference-backed QA scope.

final result: passed
