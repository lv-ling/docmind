import { describe, expect, it } from 'vitest';

import { isSafeWorkspaceRedirect, sessionExpiredLocation } from './session.js';

describe('session navigation', () => {
  it('preserves only workspace-local destinations after expiry', () => {
    expect(sessionExpiredLocation('/w/workspace-id/sources?tab=latest')).toEqual({
      name: 'login',
      query: {
        reason: 'expired',
        redirect: '/w/workspace-id/sources?tab=latest',
      },
    });
    expect(sessionExpiredLocation('/settings')).toEqual({
      name: 'login',
      query: { reason: 'expired' },
    });
  });

  it('rejects external and malformed redirect targets', () => {
    expect(isSafeWorkspaceRedirect('/w/one/sources')).toBe(true);
    expect(isSafeWorkspaceRedirect('https://example.com')).toBe(false);
    expect(isSafeWorkspaceRedirect('//example.com/w/one')).toBe(false);
    expect(isSafeWorkspaceRedirect(['/w/one'])).toBe(false);
  });
});
