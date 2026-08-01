export const DOCUMENT_LENGTH_UNITS = ['pt', 'px', 'mm', 'cm', 'in', 'percent'] as const;
export type DocumentLengthUnit = (typeof DOCUMENT_LENGTH_UNITS)[number];

export interface DocumentLength {
  value: number;
  unit: DocumentLengthUnit;
}

export interface DocumentInsets {
  top: DocumentLength;
  right: DocumentLength;
  bottom: DocumentLength;
  left: DocumentLength;
}

export const TEXT_DECORATIONS = ['underline', 'line_through'] as const;
export type TextDecoration = (typeof TEXT_DECORATIONS)[number];

export interface TextStyle {
  font_family?: string;
  font_size?: DocumentLength;
  font_weight?: number;
  italic?: boolean;
  decorations?: TextDecoration[];
  color?: string;
  background_color?: string;
  letter_spacing?: DocumentLength;
  vertical_align?: 'baseline' | 'subscript' | 'superscript';
}

export const PARAGRAPH_ALIGNMENTS = ['left', 'center', 'right', 'justify'] as const;
export type ParagraphAlignment = (typeof PARAGRAPH_ALIGNMENTS)[number];

export interface ParagraphStyle {
  alignment?: ParagraphAlignment;
  line_height?: number;
  spacing_before?: DocumentLength;
  spacing_after?: DocumentLength;
  first_line_indent?: DocumentLength;
  left_indent?: DocumentLength;
  right_indent?: DocumentLength;
  keep_with_next?: boolean;
  keep_lines_together?: boolean;
  page_break_before?: boolean;
}

export interface BorderStyle {
  width: DocumentLength;
  style: 'none' | 'solid' | 'dashed' | 'dotted' | 'double';
  color: string;
}

export interface TableCellStyle {
  background_color?: string;
  vertical_align?: 'top' | 'middle' | 'bottom';
  padding?: DocumentInsets;
  borders?: Partial<Record<'top' | 'right' | 'bottom' | 'left', BorderStyle>>;
}

export interface PageLayout {
  size: 'a4' | 'a3' | 'letter' | 'legal' | 'custom';
  orientation: 'portrait' | 'landscape';
  width: DocumentLength;
  height: DocumentLength;
  margins: DocumentInsets;
  header_distance: DocumentLength;
  footer_distance: DocumentLength;
}

const millimeters = (value: number): DocumentLength => ({ value, unit: 'mm' });

export const DEFAULT_PAGE_LAYOUT: Readonly<PageLayout> = {
  size: 'a4',
  orientation: 'portrait',
  width: millimeters(210),
  height: millimeters(297),
  margins: {
    top: millimeters(25.4),
    right: millimeters(25.4),
    bottom: millimeters(25.4),
    left: millimeters(25.4),
  },
  header_distance: millimeters(12.7),
  footer_distance: millimeters(12.7),
};
