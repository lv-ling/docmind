import { describe, expect, it } from 'vitest';

import {
  DOCMIND_COLOR_TOKENS,
  DOCMIND_DESIGN_TOKENS,
  DOCMIND_LAYOUT_TOKENS,
  DOCMIND_MOTION_TOKENS,
  DOCMIND_SPACE_TOKENS,
  DOCMIND_TYPE_TOKENS,
} from '@/ui';

describe('design tokens', () => {
  it('publishes the editorial document workbench palette and scales', () => {
    expect(DOCMIND_DESIGN_TOKENS).toEqual({
      color: DOCMIND_COLOR_TOKENS,
      space: DOCMIND_SPACE_TOKENS,
      type: DOCMIND_TYPE_TOKENS,
      motion: DOCMIND_MOTION_TOKENS,
      layout: DOCMIND_LAYOUT_TOKENS,
    });
    expect(DOCMIND_COLOR_TOKENS.paper).toBe('#ffffff');
    expect(DOCMIND_COLOR_TOKENS.diffInsert).not.toBe(DOCMIND_COLOR_TOKENS.diffDelete);
    expect(DOCMIND_TYPE_TOKENS.document).toContain('Noto Serif SC');
    expect(Number.parseInt(DOCMIND_MOTION_TOKENS.fast, 10)).toBeLessThan(
      Number.parseInt(DOCMIND_MOTION_TOKENS.normal, 10),
    );
    expect(DOCMIND_COLOR_TOKENS.zinc[900]).toBe('#18181b');
    expect(DOCMIND_COLOR_TOKENS.brandScale[600]).toBe('#4f46e5');
    expect(DOCMIND_LAYOUT_TOKENS).toMatchObject({
      sidebarWidth: '220px',
      headerHeight: '48px',
      contentMaxWidth: '1600px',
      reviewDocumentPane: '42%',
      reviewFieldsPane: '28%',
      reviewAiPane: '30%',
    });
  });
});
