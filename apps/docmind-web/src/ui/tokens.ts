export const DOCMIND_ZINC_TOKENS = {
  50: '#fafafa',
  100: '#f4f4f5',
  200: '#e4e4e7',
  300: '#d4d4d8',
  400: '#a1a1aa',
  500: '#71717a',
  600: '#52525b',
  700: '#3f3f46',
  800: '#27272a',
  900: '#18181b',
  950: '#09090b',
} as const;

export const DOCMIND_BRAND_TOKENS = {
  50: '#eef2ff',
  100: '#e0e7ff',
  200: '#c7d2fe',
  300: '#a5b4fc',
  400: '#818cf8',
  500: '#6366f1',
  600: '#4f46e5',
  700: '#4338ca',
  800: '#3730a3',
  900: '#312e81',
} as const;

export const DOCMIND_COLOR_TOKENS = {
  zinc: DOCMIND_ZINC_TOKENS,
  brandScale: DOCMIND_BRAND_TOKENS,
  canvas: DOCMIND_ZINC_TOKENS[50],
  paper: '#ffffff',
  paperMuted: DOCMIND_ZINC_TOKENS[100],
  ink: DOCMIND_ZINC_TOKENS[900],
  inkMuted: DOCMIND_ZINC_TOKENS[500],
  border: DOCMIND_ZINC_TOKENS[200],
  borderStrong: DOCMIND_ZINC_TOKENS[300],
  brand: DOCMIND_BRAND_TOKENS[600],
  brandStrong: DOCMIND_BRAND_TOKENS[700],
  brandSoft: DOCMIND_BRAND_TOKENS[50],
  accent: DOCMIND_BRAND_TOKENS[500],
  focus: DOCMIND_BRAND_TOKENS[500],
  success: '#059669',
  warning: '#d97706',
  danger: '#dc2626',
  diffInsert: '#059669',
  diffDelete: '#dc2626',
} as const;

export const DOCMIND_SPACE_TOKENS = {
  0.5: '0.125rem',
  1: '0.25rem',
  1.5: '0.375rem',
  2: '0.5rem',
  2.5: '0.625rem',
  3: '0.75rem',
  4: '1rem',
  5: '1.25rem',
  6: '1.5rem',
  8: '2rem',
  10: '2.5rem',
  12: '3rem',
} as const;

export const DOCMIND_TYPE_TOKENS = {
  ui: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans SC", "Microsoft YaHei", sans-serif',
  document: '"Noto Serif SC", "Songti SC", "Times New Roman", serif',
  mono: '"SFMono-Regular", Menlo, Monaco, Consolas, monospace',
} as const;

export const DOCMIND_MOTION_TOKENS = {
  fast: '140ms',
  normal: '220ms',
  page: '300ms',
  reveal: '500ms',
  shimmer: '1500ms',
  breathe: '3000ms',
  ping: '2500ms',
  easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
} as const;

export const DOCMIND_LAYOUT_TOKENS = {
  sidebarWidth: '220px',
  headerHeight: '48px',
  contentMaxWidth: '1600px',
  reviewDocumentPane: '42%',
  reviewFieldsPane: '28%',
  reviewAiPane: '30%',
} as const;

export const DOCMIND_DESIGN_TOKENS = {
  color: DOCMIND_COLOR_TOKENS,
  space: DOCMIND_SPACE_TOKENS,
  type: DOCMIND_TYPE_TOKENS,
  motion: DOCMIND_MOTION_TOKENS,
  layout: DOCMIND_LAYOUT_TOKENS,
} as const;
