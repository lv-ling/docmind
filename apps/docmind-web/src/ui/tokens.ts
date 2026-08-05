export const DOCMIND_COLOR_TOKENS = {
  canvas: '#f5f5f7',
  paper: '#ffffff',
  paperMuted: '#f2f2f7',
  ink: '#1d1d1f',
  inkMuted: '#86868b',
  border: '#e5e5ea',
  borderStrong: '#d1d1d6',
  brand: '#0071e3',
  brandStrong: '#0077ed',
  accent: '#0071e3',
  focus: '#0071e3',
  success: '#17845f',
  warning: '#a66508',
  danger: '#d13c4f',
  diffInsert: '#16803f',
  diffDelete: '#b42335',
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
  ui: '-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", "Segoe UI", "Noto Sans SC", "Microsoft YaHei", Arial, sans-serif',
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
