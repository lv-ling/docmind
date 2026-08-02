import type {
  JsonObject,
  SchemaFieldId,
  SchemaValueType,
  SchemaVersionId,
  TemplateResourceId,
} from '@/contracts';

import type { DocumentLength, ParagraphStyle, TableCellStyle, TextStyle } from './style.js';

declare const documentNodeIdBrand: unique symbol;
declare const templateBindingIdBrand: unique symbol;

export type DocumentNodeId = string & { readonly [documentNodeIdBrand]: 'DocumentNodeId' };
export type TemplateBindingId = string & {
  readonly [templateBindingIdBrand]: 'TemplateBindingId';
};

export interface SourceNodeReference {
  source_node_id: string;
  page_number: number | null;
}

export interface BaseDocumentNode<Type extends string> {
  id: DocumentNodeId;
  type: Type;
  source: SourceNodeReference | null;
  attributes: JsonObject;
}

export const PLACEHOLDER_VALUE_POLICIES = [
  'plain_text',
  'sanitized_rich_text',
  'resource_reference',
] as const;
export type PlaceholderValuePolicy = (typeof PLACEHOLDER_VALUE_POLICIES)[number];

export const PLACEHOLDER_MISSING_BEHAVIORS = ['show_label', 'empty', 'error'] as const;
export type PlaceholderMissingBehavior = (typeof PLACEHOLDER_MISSING_BEHAVIORS)[number];

export interface TemplateFieldBinding {
  id: TemplateBindingId;
  schema_version_id: SchemaVersionId;
  schema_field_id: SchemaFieldId;
  json_path: string;
  value_type: SchemaValueType;
  path_scope: 'absolute' | 'relative_to_repeat';
  value_policy: PlaceholderValuePolicy;
  missing_behavior: PlaceholderMissingBehavior;
  format: string | null;
}

export interface TextNode extends BaseDocumentNode<'text'> {
  text: string;
  style: TextStyle;
}

export type LineBreakNode = BaseDocumentNode<'line_break'>;

export type TabNode = BaseDocumentNode<'tab'>;

export interface DynamicFieldNode extends BaseDocumentNode<'dynamic_field'> {
  field: 'page_number' | 'page_count' | 'current_date';
  format: string | null;
  style: TextStyle;
}

export interface TemplatePlaceholderNode extends BaseDocumentNode<'template_placeholder'> {
  binding: TemplateFieldBinding;
  label: string;
  style: TextStyle;
}

export type InlineNode =
  TextNode | LineBreakNode | TabNode | DynamicFieldNode | TemplatePlaceholderNode;

export interface ParagraphNode extends BaseDocumentNode<'paragraph'> {
  content: InlineNode[];
  style: ParagraphStyle;
}

export interface HeadingNode extends BaseDocumentNode<'heading'> {
  level: 1 | 2 | 3 | 4 | 5 | 6;
  content: InlineNode[];
  style: ParagraphStyle;
}

export interface ListItemNode extends BaseDocumentNode<'list_item'> {
  blocks: BlockNode[];
}

export interface ListNode extends BaseDocumentNode<'list'> {
  ordered: boolean;
  start: number;
  marker: 'decimal' | 'lower_alpha' | 'upper_alpha' | 'lower_roman' | 'upper_roman' | 'bullet';
  items: ListItemNode[];
}

export interface TableCellNode extends BaseDocumentNode<'table_cell'> {
  row_span: number;
  column_span: number;
  width: DocumentLength | null;
  style: TableCellStyle;
  blocks: BlockNode[];
}

export interface TableRowNode extends BaseDocumentNode<'table_row'> {
  is_header: boolean;
  height: DocumentLength | null;
  cells: TableCellNode[];
}

export interface TableNode extends BaseDocumentNode<'table'> {
  rows: TableRowNode[];
  width: DocumentLength | null;
  layout: 'auto' | 'fixed';
  repeat_header: boolean;
}

export interface ImageNode extends BaseDocumentNode<'image'> {
  resource_id: TemplateResourceId;
  alt_text: string;
  title: string | null;
  width: DocumentLength;
  height: DocumentLength;
  alignment: 'left' | 'center' | 'right';
}

export interface TableOfContentsEntry {
  heading_node_id: DocumentNodeId | null;
  level: number;
  label: string;
  page_number: number | null;
}

export interface TableOfContentsNode extends BaseDocumentNode<'table_of_contents'> {
  title: string;
  entries: TableOfContentsEntry[];
  auto_update: boolean;
}

export type PageBreakNode = BaseDocumentNode<'page_break'>;

/** Read-only conversion anchor used to align the HTML template with page previews. */
export interface PageMarkerNode extends BaseDocumentNode<'page_marker'> {
  page_number: number;
}

export interface TemplateRepeatNode extends BaseDocumentNode<'template_repeat'> {
  binding: TemplateFieldBinding & { value_type: 'array' };
  item_alias: string;
  blocks: BlockNode[];
  min_items: number;
  max_items: number | null;
}

export type BlockNode =
  | ParagraphNode
  | HeadingNode
  | ListNode
  | TableNode
  | ImageNode
  | TableOfContentsNode
  | PageBreakNode
  | PageMarkerNode
  | TemplateRepeatNode;

export const BLOCK_NODE_TYPES = [
  'paragraph',
  'heading',
  'list',
  'table',
  'image',
  'table_of_contents',
  'page_break',
  'page_marker',
  'template_repeat',
] as const satisfies readonly BlockNode['type'][];

export const INLINE_NODE_TYPES = [
  'text',
  'line_break',
  'tab',
  'dynamic_field',
  'template_placeholder',
] as const satisfies readonly InlineNode['type'][];
