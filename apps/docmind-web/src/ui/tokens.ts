export const DOCMIND_COLOR_TOKENS = {
  canvas: '#e7e4da',
  paper: '#fdfcf8',
  paperMuted: '#f3f0e7',
  ink: '#171b20',
  inkMuted: '#5e646b',
  border: '#c9c6bc',
  borderStrong: '#8d9195',
  brand: '#234f73',
  brandStrong: '#173a58',
  accent: '#c65231',
  focus: '#1670ad',
  success: '#236b4b',
  warning: '#93651b',
  danger: '#a63d35',
  diffInsert: '#2d7a52',
  diffDelete: '#b2443b',
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
  ui: '"IBM Plex Sans", "Noto Sans SC", "Microsoft YaHei", sans-serif',
  document: '"Noto Serif SC", "Songti SC", "Times New Roman", serif',
  mono: '"JetBrains Mono", "SFMono-Regular", Consolas, monospace',
} as const;

export const DOCMIND_MOTION_TOKENS = {
  fast: '120ms',
  normal: '180ms',
  easing: 'cubic-bezier(0.2, 0.8, 0.2, 1)',
} as const;

export const DOCMIND_DESIGN_TOKENS = {
  color: DOCMIND_COLOR_TOKENS,
  space: DOCMIND_SPACE_TOKENS,
  type: DOCMIND_TYPE_TOKENS,
  motion: DOCMIND_MOTION_TOKENS,
} as const;
