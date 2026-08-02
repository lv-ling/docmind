import type { TemplateVersion } from '@/contracts';

export type MutableDocumentNode = Record<string, unknown> & { id?: unknown; type?: unknown };

export interface EditableDocumentBlock {
  id: string;
  type: 'paragraph' | 'heading';
  text: string;
  page: number | null;
}

export interface TemplateDiffChange {
  path: string;
  kind: 'added' | 'removed' | 'changed';
}

export const isDocumentRecord = (value: unknown): value is MutableDocumentNode =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

export const findDocumentNode = (value: unknown, id: string): MutableDocumentNode | null => {
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findDocumentNode(item, id);
      if (found !== null) return found;
    }
    return null;
  }
  if (!isDocumentRecord(value)) return null;
  if (value.id === id && typeof value.type === 'string') return value;
  for (const nested of Object.values(value)) {
    const found = findDocumentNode(nested, id);
    if (found !== null) return found;
  }
  return null;
};

export const collectDocumentTextNodes = (node: MutableDocumentNode): MutableDocumentNode[] => {
  if (!Array.isArray(node.content)) return [];
  return node.content.filter(
    (item): item is MutableDocumentNode =>
      isDocumentRecord(item) && item.type === 'text' && typeof item.text === 'string',
  );
};

export const collectEditableDocumentBlocks = (
  value: unknown,
  result: EditableDocumentBlock[] = [],
): EditableDocumentBlock[] => {
  if (Array.isArray(value)) {
    value.forEach((item) => collectEditableDocumentBlocks(item, result));
    return result;
  }
  if (!isDocumentRecord(value)) return result;
  if (
    (value.type === 'paragraph' || value.type === 'heading') &&
    typeof value.id === 'string' &&
    collectDocumentTextNodes(value).length > 0
  ) {
    const source = isDocumentRecord(value.source) ? value.source : null;
    result.push({
      id: value.id,
      type: value.type,
      text: collectDocumentTextNodes(value)
        .map((node) => String(node.text))
        .join(''),
      page: typeof source?.page_number === 'number' ? source.page_number : null,
    });
  }
  Object.values(value).forEach((nested) => collectEditableDocumentBlocks(nested, result));
  return result;
};

export const ensureDocumentNodeStyle = (node: MutableDocumentNode): MutableDocumentNode => {
  if (!isDocumentRecord(node.style)) node.style = {};
  return node.style as MutableDocumentNode;
};

export const injectTemplateResourceUrls = (
  html: string,
  resources: TemplateVersion['resources'],
  resourceUrls: string[],
): string => {
  let result = html;
  resources.forEach((resource, index) => {
    const url = resourceUrls[index];
    if (url === undefined) return;
    result = result.replaceAll(resource.download_url, url);
    result = result.replaceAll(
      `data-dm-resource-id="${resource.id}"`,
      `data-dm-resource-id="${resource.id}" src="${url}"`,
    );
  });
  return result;
};

export const parseTemplateDiffChanges = (changes: unknown): TemplateDiffChange[] => {
  if (!Array.isArray(changes)) return [];
  const result: TemplateDiffChange[] = [];
  changes.forEach((change) => {
    if (
      isDocumentRecord(change) &&
      typeof change.path === 'string' &&
      ['added', 'removed', 'changed'].includes(String(change.kind))
    ) {
      result.push({
        path: change.path,
        kind: String(change.kind) as TemplateDiffChange['kind'],
      });
    }
  });
  return result;
};

export const createTemplatePreviewSrcdoc = (
  html: string,
  css: string,
  zoomPercentage: number,
): string => `<!doctype html><html><head>
  <meta charset="utf-8">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src blob: data:; style-src 'unsafe-inline'; font-src blob: data:">
  <style>${css}html{background:#d8d7d2}body{margin:0;padding:28px;zoom:${zoomPercentage / 100}}.dm-document{box-shadow:0 8px 34px rgba(21,31,39,.16)}</style>
  </head><body>${html}</body></html>`;
