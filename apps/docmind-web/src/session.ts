import { RouteName, RoutePath } from '@/router/constants.js';

export const SESSION_EXPIRED_EVENT = 'docmind:session-expired';

export const isSafeAppRedirect = (value: unknown): value is string =>
  typeof value === 'string' &&
  (value === RoutePath.Workbench || value.startsWith(`${RoutePath.Workbench}/`));

export const sessionExpiredLocation = (fullPath: string) => ({
  name: RouteName.Login,
  query: isSafeAppRedirect(fullPath)
    ? { reason: 'expired', redirect: fullPath }
    : { reason: 'expired' },
});
