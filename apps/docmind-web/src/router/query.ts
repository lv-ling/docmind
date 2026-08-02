import type { LocationQueryValue } from 'vue-router';

export const getQueryString = (
  value: LocationQueryValue | LocationQueryValue[] | undefined,
): string | null => (typeof value === 'string' && value.length > 0 ? value : null);
