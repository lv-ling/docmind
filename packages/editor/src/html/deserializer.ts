import { parseFragment } from 'parse5';
import type { SafeHtmlDocument } from '@docmind/contracts';

import type { ControlledDocument } from '../model/document.js';
import { DOCUMENT_VALIDATION_LIMITS, assertValidControlledDocument } from '../validation/index.js';
import { applyControlledHtmlPolicy, UnsafeControlledHtmlError } from './policy.js';
import { CONTROLLED_DOCUMENT_CSS } from './serializer.js';
import { HTML_SANITIZATION_POLICY_VERSION } from './policy.js';

interface HtmlAstAttribute {
  name: string;
  value: string;
}

interface HtmlAstNode {
  nodeName: string;
  tagName?: string;
  value?: string;
  attrs?: HtmlAstAttribute[];
  childNodes?: HtmlAstNode[];
  content?: HtmlAstNode;
}

export class ControlledDocumentMetadataError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ControlledDocumentMetadataError';
  }
}

const findMetadataTemplates = (node: HtmlAstNode, found: HtmlAstNode[]): void => {
  if (
    node.tagName === 'template' &&
    node.attrs?.some(
      (attribute) => attribute.name === 'data-dm-document-model' && attribute.value === '1.0',
    ) === true
  ) {
    found.push(node);
  }
  for (const child of node.childNodes ?? []) findMetadataTemplates(child, found);
  if (node.content !== undefined) findMetadataTemplates(node.content, found);
};

const collectText = (node: HtmlAstNode): string => {
  if (node.nodeName === '#text') return node.value ?? '';
  const container = node.tagName === 'template' && node.content !== undefined ? node.content : node;
  return (container.childNodes ?? []).map(collectText).join('');
};

export const deserializeControlledDocument = (html: string): ControlledDocument => {
  const policyResult = applyControlledHtmlPolicy(html);
  if (!policyResult.valid) throw new UnsafeControlledHtmlError(policyResult.issues);

  const fragment = parseFragment(policyResult.html) as unknown as HtmlAstNode;
  const templates: HtmlAstNode[] = [];
  findMetadataTemplates(fragment, templates);
  if (templates.length !== 1) {
    throw new ControlledDocumentMetadataError(
      `Expected exactly one DocMind model template, found ${templates.length}.`,
    );
  }

  const metadataJson = collectText(templates[0] as HtmlAstNode);
  if (
    new TextEncoder().encode(metadataJson).byteLength >
    DOCUMENT_VALIDATION_LIMITS.max_metadata_json_bytes
  ) {
    throw new ControlledDocumentMetadataError('Embedded document model exceeds the size limit.');
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(metadataJson) as unknown;
  } catch {
    throw new ControlledDocumentMetadataError('Embedded document model is not valid JSON.');
  }
  assertValidControlledDocument(parsed);
  return parsed;
};

export const deserializeSafeHtmlDocument = (document: SafeHtmlDocument): ControlledDocument => {
  if (document.sanitization_policy_version !== HTML_SANITIZATION_POLICY_VERSION) {
    throw new ControlledDocumentMetadataError('Unsupported HTML sanitization policy version.');
  }
  if (document.css !== CONTROLLED_DOCUMENT_CSS) {
    throw new ControlledDocumentMetadataError('Controlled document CSS does not match the policy.');
  }
  return deserializeControlledDocument(document.html);
};
