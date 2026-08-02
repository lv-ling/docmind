import { describe, expect, it } from 'vitest';

import { RouteName } from '@/router/constants.js';
import { isSafeAppRedirect, sessionExpiredLocation } from '@/session.js';

describe('session navigation', () => {
  it('preserves only workbench-local destinations after expiry', () => {
    expect(sessionExpiredLocation('/workbench/source/list?tab=latest')).toEqual({
      name: RouteName.Login,
      query: {
        reason: 'expired',
        redirect: '/workbench/source/list?tab=latest',
      },
    });
    expect(sessionExpiredLocation('/settings')).toEqual({
      name: RouteName.Login,
      query: { reason: 'expired' },
    });
  });

  it('rejects external and malformed redirect targets', () => {
    expect(isSafeAppRedirect('/workbench/source/detail?sourceId=one')).toBe(true);
    expect(isSafeAppRedirect('https://example.com')).toBe(false);
    expect(isSafeAppRedirect('//example.com/workbench/source/list')).toBe(false);
    expect(isSafeAppRedirect(['/workbench/source/list'])).toBe(false);
  });
});
