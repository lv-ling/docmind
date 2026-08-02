export const withoutUndefined = <T extends object>(value: T): Partial<T> =>
  Object.fromEntries(
    Object.entries(value).filter(([, entry]) => entry !== undefined),
  ) as Partial<T>;

export const withoutKeys = <T extends object, K extends keyof T>(
  value: T,
  keys: readonly K[],
): Omit<T, K> =>
  Object.fromEntries(Object.entries(value).filter(([key]) => !keys.includes(key as K))) as Omit<
    T,
    K
  >;
