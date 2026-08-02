import { describe, expect, it } from 'vitest';

import { CONTRACTS_API_VERSION } from '@/contracts';

describe('web contracts', () => {
  it('exposes the current public contract generation', () => {
    expect(CONTRACTS_API_VERSION).toBe('v1');
  });
});
