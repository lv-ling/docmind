export interface DmTabItem {
  value: string;
  label: string;
  count?: number;
  disabled?: boolean;
  panelId?: string;
}

export interface DmDropdownItem {
  type: 'item';
  value: string;
  label: string;
  disabled?: boolean;
  destructive?: boolean;
}

export interface DmDropdownSeparator {
  type: 'separator';
  key: string;
}

export type DmDropdownEntry = DmDropdownItem | DmDropdownSeparator;
