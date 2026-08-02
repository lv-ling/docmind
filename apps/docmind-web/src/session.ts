export const SESSION_EXPIRED_EVENT = 'docmind:session-expired';

export const isSafeWorkspaceRedirect = (value: unknown): value is string =>
  typeof value === 'string' && value.startsWith('/w/');

export const sessionExpiredLocation = (fullPath: string) => ({
  name: 'login' as const,
  query: isSafeWorkspaceRedirect(fullPath)
    ? { reason: 'expired', redirect: fullPath }
    : { reason: 'expired' },
});
