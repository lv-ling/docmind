# shadcn-vue integration baseline

## Layer boundary

DocMind uses shadcn-vue as a checked-in primitive source, not as a page-level component API.

```text
src/components/ui      shadcn-vue primitives and Reka UI behavior
        ↓
src/components/dm      stable DocMind component API and visual contract
        ↓
src/views              business components and pages
```

Business pages and layouts must continue importing from `@/ui`. They must not import from
`@/components/ui` or `reka-ui` directly. The architecture test enforces this boundary.

## Tailwind and token policy

- `components.json` points the CLI at `src/styles/tailwind.css`.
- The existing Tailwind v4 token bridge remains authoritative.
- shadcn primitives provide structure, accessibility behavior, and state attributes only.
- DocMind wrappers own Zinc/Brand colors, compact radii, typography, spacing, and interaction styles.
- Running `shadcn-vue init` over this repository is not part of the workflow because it can replace
  the established stylesheet. Add or inspect primitives with the CLI, then review generated changes.

## Migration matrix

| Dm component | Current state | Primitive layer | API policy |
| --- | --- | --- | --- |
| `DmButton` | Phase 1 migrated | shadcn `Button` / Reka `Primitive` | Preserve variants, sizes, loading, icon-only, and click events |
| `DmInput` | Phase 1 migrated | shadcn `Input` | Preserve model modifiers, input types, appearances, and DOM events |
| `DmTextarea` | Phase 1 migrated | shadcn `Textarea` | Preserve string model and textarea attributes |
| `DmCheckbox` | Phase 1 migrated | shadcn `Checkbox` / Reka checkbox behavior | Preserve boolean model, label, description, and disabled state |
| `DmSelect` | Phase 1 migrated | shadcn `NativeSelect` | Preserve native `option` slot and string model |
| `DmTabs` | Phase 2 migrated | shadcn `Tabs` / Reka roving focus | Preserves `v-model`, item arrays, change events, disabled tabs, and keyboard navigation |
| `DmDialog` | Phase 2 migrated | shadcn `Dialog` | Controlled `v-model`, title, description, trigger/body/footer slots, confirm/cancel, and loading guard |
| `DmDropdown` | Phase 2 migrated | shadcn `DropdownMenu` | Controlled open state, trigger/item slots, typed items, separators, disabled and destructive states |
| `DmPopover` | Phase 2 migrated | shadcn `Popover` | Controlled open state, trigger/content slots, placement, alignment, and modal behavior |
| `DmRange` | Remains self-owned | Native range today; Slider is optional later | Do not change native range semantics without a product need |

Complex document preview, extraction, review, and pipeline components remain business-owned in
Phase 3. They may compose Dm components but must not expose shadcn primitives as their public API.
