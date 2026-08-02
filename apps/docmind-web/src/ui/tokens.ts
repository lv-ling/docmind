export const DOCMIND_COLOR_TOKENS = {
  canvas: '#fafafa',
  paper: '#ffffff',
  paperMuted: '#f4f4f5',
  ink: '#18181b',
  inkMuted: '#71717a',
  border: '#e4e4e7',
  borderStrong: '#d4d4d8',
  brand: '#4f46e5',
  brandStrong: '#4338ca',
  accent: '#6366f1',
  focus: '#6366f1',
  success: '#059669',
  warning: '#d97706',
  danger: '#dc2626',
  diffInsert: '#059669',
  diffDelete: '#dc2626',
} as const;

export const DOCMIND_SPACE_TOKENS = {
  1: '0.25rem',
  2: '0.5rem',
  3: '0.75rem',
  4: '1rem',
  5: '1.5rem',
  6: '2rem',
  7: '3rem',
} as const;

export const DOCMIND_TYPE_TOKENS = {
  ui: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans SC", "Microsoft YaHei", sans-serif',
  document: '"Noto Serif SC", "Songti SC", "Times New Roman", serif',
  mono: '"SFMono-Regular", Menlo, Monaco, Consolas, monospace',
} as const;

export const DOCMIND_MOTION_TOKENS = {
  fast: '140ms',
  normal: '220ms',
  easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
} as const;

export const DOCMIND_DESIGN_TOKENS = {
  color: DOCMIND_COLOR_TOKENS,
  space: DOCMIND_SPACE_TOKENS,
  type: DOCMIND_TYPE_TOKENS,
  motion: DOCMIND_MOTION_TOKENS,
} as const;
