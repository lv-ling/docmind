/** Public types for the DmDataTable component. */
export type DmTableColumnAlign = 'left' | 'center' | 'right';
export type DmTableSortDirection = 'asc' | 'desc';

export interface DmDataTableColumn<TRow> {
  key: string;
  header: string;
  accessor: (row: TRow) => unknown;
  align?: DmTableColumnAlign;
  width?: string;
  sortable?: boolean;
}

export interface DmTableSort {
  key: string;
  direction: DmTableSortDirection;
}
