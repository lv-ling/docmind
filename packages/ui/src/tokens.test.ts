import { describe, expect, it } from 'vitest';

import {
  DOCMIND_COLOR_TOKENS,
  DOCMIND_DESIGN_TOKENS,
  DOCMIND_MOTION_TOKENS,
  DOCMIND_SPACE_TOKENS,
  DOCMIND_TYPE_TOKENS,
} from './index.js';

describe('design tokens', () => {
  it('publishes the editorial document workbench palette and scales', () => {
    expect(DOCMIND_DESIGN_TOKENS).toEqual({
      color: DOCMIND_COLOR_TOKENS,
      space: DOCMIND_SPACE_TOKENS,
      type: DOCMIND_TYPE_TOKENS,
      motion: DOCMIND_MOTION_TOKENS,
    });
    expect(DOCMIND_COLOR_TOKENS.paper).toBe('#fdfcf8');
    expect(DOCMIND_COLOR_TOKENS.diffInsert).not.toBe(DOCMIND_COLOR_TOKENS.diffDelete);
    expect(DOCMIND_TYPE_TOKENS.document).toContain('Noto Serif SC');
    expect(Number.parseInt(DOCMIND_MOTION_TOKENS.fast, 10)).toBeLessThan(
      Number.parseInt(DOCMIND_MOTION_TOKENS.normal, 10),
    );
  });
});
