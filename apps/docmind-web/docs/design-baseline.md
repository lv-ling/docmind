# DocMind Premium Workspace Design Baseline

## Reference

- Source prototype: `docmind_premium_workspace.html`
- Product surface: authenticated desktop workspace
- Typeface: Inter with Apple and system Chinese sans-serif fallbacks
- Visual direction: dense Zinc workspace with restrained Indigo AI emphasis

This baseline records source measurements. It is not permission to redesign pages that are only placeholders in the prototype.

## Layout geometry

| Area | Baseline |
| --- | --- |
| Workspace sidebar | `220px` fixed desktop width |
| Global header | `48px` fixed height |
| Main content | Fluid width with `1600px` maximum inner boundary |
| Standard page padding | `24px` |
| Compact control radius | `5px` to `6px` |
| Card radius | `8px` |
| Large surface radius | `12px` |

The sidebar and global header remain visible for standard workspace routes. Immersive routes may hide the sidebar while preserving their own 48px command header.

## Color tokens

### Zinc

| Scale | Value |
| --- | --- |
| 50 | `#fafafa` |
| 100 | `#f4f4f5` |
| 200 | `#e4e4e7` |
| 300 | `#d4d4d8` |
| 400 | `#a1a1aa` |
| 500 | `#71717a` |
| 600 | `#52525b` |
| 700 | `#3f3f46` |
| 800 | `#27272a` |
| 900 | `#18181b` |
| 950 | `#09090b` |

### Brand Indigo

| Scale | Value |
| --- | --- |
| 50 | `#eef2ff` |
| 100 | `#e0e7ff` |
| 200 | `#c7d2fe` |
| 300 | `#a5b4fc` |
| 400 | `#818cf8` |
| 500 | `#6366f1` |
| 600 | `#4f46e5` |
| 700 | `#4338ca` |
| 800 | `#3730a3` |
| 900 | `#312e81` |

Semantic aliases use Zinc 50 for canvas, white for paper, Zinc 900 for primary text, Zinc 500 for muted text, Zinc 200 for borders, Brand 600 for primary AI actions, and Brand 500 for focus.

## Motion

| Motion | Baseline |
| --- | --- |
| Page fade | `300ms`, `translateY(2px)` |
| Stagger reveal | `500ms`, `translateY(6px)` |
| Reveal delays | `100ms`, `150ms`, `200ms`, `300ms` |
| Shimmer | `1500ms linear infinite` |
| AI breathe | `3000ms ease-in-out infinite` |
| AI ping | `2500ms cubic-bezier(0, 0, 0.2, 1) infinite` |
| Shared easing | `cubic-bezier(0.16, 1, 0.3, 1)` |

Every motion treatment must stop or reduce under `prefers-reduced-motion: reduce`.

## Review workspace

The immersive review workspace uses three simultaneous business panes:

| Pane | Width |
| --- | --- |
| Original document and evidence | `42%` |
| Structured extraction fields | `28%` |
| AI analysis and copilot | `30%` |

The review queue and review workspace remain separate route boundaries. The queue selects work; the immersive workspace reviews one extraction run identified by `extractionId`.

## Engineering guardrails

- Tailwind classes in the HTML are measurement references only; Tailwind is not introduced into the Vue application.
- `theme.css` remains the theme-variable entry point.
- UI primitives consume semantic variables and do not define a competing palette.
- Existing auth, workspace, API, contract, and editor boundaries remain unchanged.
- Pages without a complete prototype remain route boundaries until a final visual reference is supplied.
