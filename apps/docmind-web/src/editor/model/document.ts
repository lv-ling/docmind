import type { SchemaVersionId } from '@/contracts';

import type { BlockNode, DocumentNodeId } from './nodes.js';
import { DEFAULT_PAGE_LAYOUT, type PageLayout } from './style.js';

export const DOCUMENT_MODEL_VERSION = '1.0' as const;

export interface HeaderFooterRegion {
  variant: 'default' | 'first_page' | 'even_pages';
  blocks: BlockNode[];
}

export interface ControlledDocumentMetadata {
  title: string;
  language: string;
  source_page_count: number | null;
}

export interface ControlledDocument {
  model_version: typeof DOCUMENT_MODEL_VERSION;
  root_id: DocumentNodeId;
  template_schema_version_id: SchemaVersionId | null;
  metadata: ControlledDocumentMetadata;
  page_layout: PageLayout;
  headers: HeaderFooterRegion[];
  footers: HeaderFooterRegion[];
  blocks: BlockNode[];
}

export interface CreateEmptyDocumentOptions {
  root_id: DocumentNodeId;
  title: string;
  language: string;
  template_schema_version_id?: SchemaVersionId | null;
}

const cloneDefaultPageLayout = (): PageLayout => structuredClone(DEFAULT_PAGE_LAYOUT);

export const createEmptyDocument = (options: CreateEmptyDocumentOptions): ControlledDocument => ({
  model_version: DOCUMENT_MODEL_VERSION,
  root_id: options.root_id,
  template_schema_version_id: options.template_schema_version_id ?? null,
  metadata: {
    title: options.title,
    language: options.language,
    source_page_count: null,
  },
  page_layout: cloneDefaultPageLayout(),
  headers: [],
  footers: [],
  blocks: [],
});
