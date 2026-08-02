declare const cursorBrand: unique symbol;

export type Cursor = string & { readonly [cursorBrand]: true };

export const DEFAULT_PAGE_LIMIT = 20;
export const MAX_PAGE_LIMIT = 100;

export interface CursorPageRequest {
  cursor?: Cursor;
  limit?: number;
}

export interface CursorPage<T> {
  items: T[];
  next_cursor: Cursor | null;
  has_more: boolean;
}
