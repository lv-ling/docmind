import type { SourceDocument } from '@/contracts';
import { describe, expect, it } from 'vitest';

import { filterSources, getSourceVersionLabel } from '@/views/source/list/model/source-list.js';

const createSource = (name: string, versionId: string | null): SourceDocument =>
  ({ name, current_version_id: versionId }) as SourceDocument;

describe('source list model', () => {
  const sources = [createSource('采购合同', 'version-123456789'), createSource('员工手册', null)];

  it('filters by localized query and registration state', () => {
    expect(filterSources(sources, '合同', 'all')).toEqual([sources[0]]);
    expect(filterSources(sources, '', 'registered')).toEqual([sources[0]]);
    expect(filterSources(sources, '', 'pending')).toEqual([sources[1]]);
  });

  it('formats a short version label and a pending fallback', () => {
    expect(getSourceVersionLabel(sources[0]!)).toBe('version-');
    expect(getSourceVersionLabel(sources[1]!)).toBe('待上传');
  });
});
