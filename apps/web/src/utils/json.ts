export const cloneJsonValue = <T>(value: T): T => {
  const serialized = JSON.stringify(value);
  if (serialized === undefined) {
    throw new TypeError('Value is not JSON serializable');
  }
  return JSON.parse(serialized) as T;
};
