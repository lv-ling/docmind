import { parseFragment } from 'parse5';

export const HTML_SANITIZATION_POLICY_VERSION = 'dm-html-v1' as const;
export const MAX_CONTROLLED_HTML_BYTES = 10 * 1024 * 1024;
export const MAX_CONTROLLED_HTML_NODES = 100_000;
export const MAX_CONTROLLED_HTML_DEPTH = 100;

const ALLOWED_TAGS = new Set([
  'article',
  'template',
  'header',
  'footer',
  'main',
  'section',
  'div',
  'nav',
  'p',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'span',
  'br',
  'ol',
  'ul',
  'li',
  'table',
  'tbody',
  'tr',
  'td',
  'th',
  'img',
]);

const DROP_WITH_CONTENT_TAGS = new Set([
  'script',
  'style',
  'iframe',
  'object',
  'embed',
  'svg',
  'math',
  'link',
  'meta',
]);

const VOID_TAGS = new Set(['br', 'img']);
const ALLOWED_ATTRIBUTES = new Set([
  'class',
  'lang',
  'role',
  'alt',
  'title',
  'colspan',
  'rowspan',
  'start',
  'style',
]);

const ALLOWED_STYLE_PROPERTIES = new Set([
  'width',
  'height',
  'min-height',
  'padding',
  'font-family',
  'font-size',
  'font-weight',
  'font-style',
  'text-decoration',
  'color',
  'background-color',
  'letter-spacing',
  'vertical-align',
  'text-align',
  'line-height',
  'margin-top',
  'margin-bottom',
  'margin-left',
  'margin-right',
  'text-indent',
  'break-before',
  'break-inside',
]);

const SAFE_CSS_VALUE = /^[#(),.%\p{L}\p{N}\s_+-]+$/u;

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

export interface HtmlPolicyIssue {
  code: string;
  tag: string | null;
  attribute: string | null;
  message: string;
}

export interface HtmlPolicyResult {
  html: string;
  valid: boolean;
  issues: HtmlPolicyIssue[];
}

interface HtmlSanitizationContext {
  issues: HtmlPolicyIssue[];
  node_count: number;
}

export class UnsafeControlledHtmlError extends Error {
  readonly issues: HtmlPolicyIssue[];

  constructor(issues: HtmlPolicyIssue[]) {
    super(
      `Controlled HTML violates the whitelist (${issues.length} issue${issues.length === 1 ? '' : 's'})`,
    );
    this.name = 'UnsafeControlledHtmlError';
    this.issues = issues;
  }
}

const escapeText = (value: string): string =>
  value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');

const escapeAttribute = (value: string): string =>
  escapeText(value).replaceAll('"', '&quot;').replaceAll("'", '&#39;');

const sanitizeStyle = (
  value: string,
  tag: string,
  context: HtmlSanitizationContext,
): string | null => {
  const safeDeclarations: string[] = [];

  for (const declaration of value.split(';')) {
    const separator = declaration.indexOf(':');
    if (separator < 1) continue;
    const property = declaration.slice(0, separator).trim().toLowerCase();
    const cssValue = declaration.slice(separator + 1).trim();
    const loweredValue = cssValue.toLowerCase();
    if (
      !ALLOWED_STYLE_PROPERTIES.has(property) ||
      cssValue.length === 0 ||
      !SAFE_CSS_VALUE.test(cssValue) ||
      loweredValue.includes('url(') ||
      loweredValue.includes('expression') ||
      loweredValue.includes('javascript:')
    ) {
      context.issues.push({
        code: 'STYLE_DECLARATION_REMOVED',
        tag,
        attribute: 'style',
        message: `Unsafe CSS declaration removed: ${property}`,
      });
      continue;
    }
    safeDeclarations.push(`${property}:${cssValue}`);
  }

  return safeDeclarations.length === 0 ? null : `${safeDeclarations.join(';')};`;
};

const sanitizeAttributes = (
  tag: string,
  attributes: HtmlAstAttribute[],
  context: HtmlSanitizationContext,
): string => {
  const serialized: string[] = [];

  for (const attribute of attributes) {
    const name = attribute.name.toLowerCase();
    const allowed =
      ALLOWED_ATTRIBUTES.has(name) || name.startsWith('data-dm-') || name.startsWith('aria-');
    if (!allowed || name.startsWith('on') || name === 'src' || name === 'href') {
      context.issues.push({
        code: 'ATTRIBUTE_REMOVED',
        tag,
        attribute: name,
        message: `Attribute is not permitted: ${name}`,
      });
      continue;
    }
    if (name === 'style') {
      const safeStyle = sanitizeStyle(attribute.value, tag, context);
      if (safeStyle !== null) serialized.push(`style="${escapeAttribute(safeStyle)}"`);
      continue;
    }
    serialized.push(`${name}="${escapeAttribute(attribute.value)}"`);
  }

  return serialized.length === 0 ? '' : ` ${serialized.join(' ')}`;
};

const serializeChildren = (
  node: HtmlAstNode,
  context: HtmlSanitizationContext,
  depth: number,
): string => {
  const container = node.tagName === 'template' && node.content !== undefined ? node.content : node;
  return (container.childNodes ?? [])
    .map((child) => sanitizeNode(child, context, depth + 1))
    .join('');
};

const sanitizeNode = (
  node: HtmlAstNode,
  context: HtmlSanitizationContext,
  depth: number,
): string => {
  context.node_count += 1;
  if (context.node_count > MAX_CONTROLLED_HTML_NODES) {
    if (!context.issues.some(({ code }) => code === 'HTML_NODE_LIMIT_EXCEEDED')) {
      context.issues.push({
        code: 'HTML_NODE_LIMIT_EXCEEDED',
        tag: null,
        attribute: null,
        message: 'Controlled HTML contains too many nodes.',
      });
    }
    return '';
  }
  if (depth > MAX_CONTROLLED_HTML_DEPTH) {
    context.issues.push({
      code: 'HTML_DEPTH_LIMIT_EXCEEDED',
      tag: node.tagName ?? null,
      attribute: null,
      message: 'Controlled HTML nesting is too deep.',
    });
    return '';
  }
  if (node.nodeName === '#text') return escapeText(node.value ?? '');
  if (node.nodeName === '#comment') return '';
  if (node.nodeName === '#document-fragment') return serializeChildren(node, context, depth);

  const tag = (node.tagName ?? node.nodeName).toLowerCase();
  if (DROP_WITH_CONTENT_TAGS.has(tag)) {
    context.issues.push({
      code: 'DANGEROUS_TAG_REMOVED',
      tag,
      attribute: null,
      message: `Dangerous element removed: ${tag}`,
    });
    return '';
  }
  if (!ALLOWED_TAGS.has(tag)) {
    context.issues.push({
      code: 'TAG_UNWRAPPED',
      tag,
      attribute: null,
      message: `Unsupported element unwrapped: ${tag}`,
    });
    return serializeChildren(node, context, depth);
  }

  const attributes = sanitizeAttributes(tag, node.attrs ?? [], context);
  if (VOID_TAGS.has(tag)) return `<${tag}${attributes}>`;
  return `<${tag}${attributes}>${serializeChildren(node, context, depth)}</${tag}>`;
};

export const applyControlledHtmlPolicy = (html: string): HtmlPolicyResult => {
  if (new TextEncoder().encode(html).byteLength > MAX_CONTROLLED_HTML_BYTES) {
    const issue: HtmlPolicyIssue = {
      code: 'HTML_SIZE_LIMIT_EXCEEDED',
      tag: null,
      attribute: null,
      message: 'Controlled HTML exceeds the maximum size.',
    };
    return { html: '', valid: false, issues: [issue] };
  }

  const fragment = parseFragment(html) as unknown as HtmlAstNode;
  const context: HtmlSanitizationContext = { issues: [], node_count: 0 };
  const sanitized = sanitizeNode(fragment, context, 0);
  return { html: sanitized, valid: context.issues.length === 0, issues: context.issues };
};

export const sanitizeControlledHtml = (html: string): string =>
  applyControlledHtmlPolicy(html).html;

export const assertSafeControlledHtml = (html: string): void => {
  const result = applyControlledHtmlPolicy(html);
  if (!result.valid) throw new UnsafeControlledHtmlError(result.issues);
};
