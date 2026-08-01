import type { SafeHtmlDocument } from '@docmind/contracts';

import type { ControlledDocument, HeaderFooterRegion } from '../model/document.js';
import type { BlockNode, InlineNode, TableCellNode, TemplateFieldBinding } from '../model/nodes.js';
import type { DocumentLength, ParagraphStyle, TextStyle } from '../model/style.js';
import { assertValidControlledDocument } from '../validation/index.js';
import { HTML_SANITIZATION_POLICY_VERSION, applyControlledHtmlPolicy } from './policy.js';

export const CONTROLLED_DOCUMENT_CSS = [
  '.dm-document{box-sizing:border-box;margin:0 auto;background:#fff;}',
  '.dm-document *{box-sizing:border-box;}',
  '.dm-document table{border-collapse:collapse;max-width:100%;}',
  '.dm-document img{max-width:100%;}',
  '.dm-page-break{break-after:page;height:0;}',
  '.dm-page-marker{display:none;}',
  '.dm-placeholder{border-radius:2px;background:#fff3bf;color:#664d03;}',
  '.dm-repeat{border:1px dashed #8c8c8c;}',
].join('');

const escapeText = (value: string): string =>
  value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');

const escapeAttribute = (value: string): string =>
  escapeText(value).replaceAll('"', '&quot;').replaceAll("'", '&#39;');

const lengthToCss = (length: DocumentLength): string =>
  `${length.value}${length.unit === 'percent' ? '%' : length.unit}`;

const styleAttribute = (declarations: Array<string | null>): string => {
  const style = declarations
    .filter((declaration): declaration is string => declaration !== null)
    .join(';');
  return style.length === 0 ? '' : ` style="${escapeAttribute(`${style};`)}"`;
};

const textStyleAttribute = (style: TextStyle): string =>
  styleAttribute([
    style.font_family === undefined ? null : `font-family:${style.font_family}`,
    style.font_size === undefined ? null : `font-size:${lengthToCss(style.font_size)}`,
    style.font_weight === undefined ? null : `font-weight:${style.font_weight}`,
    style.italic === undefined ? null : `font-style:${style.italic ? 'italic' : 'normal'}`,
    style.decorations === undefined ? null : `text-decoration:${style.decorations.join(' ')}`,
    style.color === undefined ? null : `color:${style.color}`,
    style.background_color === undefined ? null : `background-color:${style.background_color}`,
    style.letter_spacing === undefined
      ? null
      : `letter-spacing:${lengthToCss(style.letter_spacing)}`,
    style.vertical_align === undefined ? null : `vertical-align:${style.vertical_align}`,
  ]);

const paragraphStyleAttribute = (style: ParagraphStyle): string =>
  styleAttribute([
    style.alignment === undefined ? null : `text-align:${style.alignment}`,
    style.line_height === undefined ? null : `line-height:${style.line_height}`,
    style.spacing_before === undefined ? null : `margin-top:${lengthToCss(style.spacing_before)}`,
    style.spacing_after === undefined ? null : `margin-bottom:${lengthToCss(style.spacing_after)}`,
    style.first_line_indent === undefined
      ? null
      : `text-indent:${lengthToCss(style.first_line_indent)}`,
    style.left_indent === undefined ? null : `margin-left:${lengthToCss(style.left_indent)}`,
    style.right_indent === undefined ? null : `margin-right:${lengthToCss(style.right_indent)}`,
    style.page_break_before === true ? 'break-before:page' : null,
    style.keep_lines_together === true ? 'break-inside:avoid' : null,
  ]);

const nodeAttributes = (id: string, type: string): string =>
  ` data-dm-node-id="${escapeAttribute(id)}" data-dm-node-type="${escapeAttribute(type)}"`;

const bindingAttributes = (binding: TemplateFieldBinding): string =>
  [
    `data-dm-binding-id="${escapeAttribute(binding.id)}"`,
    `data-dm-schema-field-id="${escapeAttribute(binding.schema_field_id)}"`,
    `data-dm-json-path="${escapeAttribute(binding.json_path)}"`,
  ].join(' ');

const renderInline = (node: InlineNode): string => {
  const attributes = nodeAttributes(node.id, node.type);
  switch (node.type) {
    case 'text':
      return `<span${attributes}${textStyleAttribute(node.style)}>${escapeText(node.text)}</span>`;
    case 'line_break':
      return `<br${attributes}>`;
    case 'tab':
      return `<span${attributes}>\t</span>`;
    case 'dynamic_field':
      return `<span${attributes} data-dm-dynamic-field="${node.field}"${textStyleAttribute(node.style)}>{${node.field.toUpperCase()}}</span>`;
    case 'template_placeholder':
      return `<span${attributes} class="dm-placeholder" ${bindingAttributes(node.binding)}${textStyleAttribute(node.style)}>{{${escapeText(node.label)}}}</span>`;
  }
};

const renderCell = (cell: TableCellNode): string => {
  const tag = cell.attributes.is_header === true ? 'th' : 'td';
  const spanAttributes = `${cell.row_span === 1 ? '' : ` rowspan="${cell.row_span}"`}${
    cell.column_span === 1 ? '' : ` colspan="${cell.column_span}"`
  }`;
  return `<${tag}${nodeAttributes(cell.id, cell.type)}${spanAttributes}>${renderBlocks(cell.blocks)}</${tag}>`;
};

const renderBlock = (node: BlockNode): string => {
  const attributes = nodeAttributes(node.id, node.type);
  switch (node.type) {
    case 'paragraph':
      return `<p${attributes}${paragraphStyleAttribute(node.style)}>${node.content.map(renderInline).join('')}</p>`;
    case 'heading':
      return `<h${node.level}${attributes}${paragraphStyleAttribute(node.style)}>${node.content.map(renderInline).join('')}</h${node.level}>`;
    case 'list': {
      const tag = node.ordered ? 'ol' : 'ul';
      const start = node.ordered && node.start !== 1 ? ` start="${node.start}"` : '';
      return `<${tag}${attributes}${start}>${node.items
        .map((item) => `<li${nodeAttributes(item.id, item.type)}>${renderBlocks(item.blocks)}</li>`)
        .join('')}</${tag}>`;
    }
    case 'table':
      return `<table${attributes}>${node.rows
        .map(
          (row) =>
            `<tr${nodeAttributes(row.id, row.type)}>${row.cells.map(renderCell).join('')}</tr>`,
        )
        .join('')}</table>`;
    case 'image':
      return `<img${attributes} data-dm-resource-id="${escapeAttribute(node.resource_id)}" alt="${escapeAttribute(node.alt_text)}"${
        node.title === null ? '' : ` title="${escapeAttribute(node.title)}"`
      }${styleAttribute([
        `width:${lengthToCss(node.width)}`,
        `height:${lengthToCss(node.height)}`,
      ])}>`;
    case 'table_of_contents':
      return `<nav${attributes} aria-label="${escapeAttribute(node.title)}"><p>${escapeText(node.title)}</p><ol>${node.entries
        .map(
          (entry) =>
            `<li data-dm-toc-level="${entry.level}"><span>${escapeText(entry.label)}</span>${
              entry.page_number === null ? '' : `<span>${entry.page_number}</span>`
            }</li>`,
        )
        .join('')}</ol></nav>`;
    case 'page_break':
      return `<div${attributes} class="dm-page-break" role="separator"></div>`;
    case 'page_marker':
      return `<div${attributes} class="dm-page-marker" data-dm-page-number="${node.page_number}"></div>`;
    case 'template_repeat':
      return `<section${attributes} class="dm-repeat" ${bindingAttributes(node.binding)} data-dm-item-alias="${escapeAttribute(node.item_alias)}">${renderBlocks(node.blocks)}</section>`;
  }
};

const renderBlocks = (blocks: BlockNode[]): string => blocks.map(renderBlock).join('');

const renderRegion = (tag: 'header' | 'footer', region: HeaderFooterRegion): string =>
  `<${tag} data-dm-region-variant="${region.variant}">${renderBlocks(region.blocks)}</${tag}>`;

const safeMetadataJson = (document: ControlledDocument): string =>
  JSON.stringify(document)
    .replaceAll('<', '\\u003c')
    .replaceAll('>', '\\u003e')
    .replaceAll('&', '\\u0026');

export const serializeControlledDocument = (document: ControlledDocument): SafeHtmlDocument => {
  assertValidControlledDocument(document);
  const layoutStyle = styleAttribute([
    `width:${lengthToCss(document.page_layout.width)}`,
    `min-height:${lengthToCss(document.page_layout.height)}`,
    `padding:${[
      document.page_layout.margins.top,
      document.page_layout.margins.right,
      document.page_layout.margins.bottom,
      document.page_layout.margins.left,
    ]
      .map(lengthToCss)
      .join(' ')}`,
  ]);
  const rawHtml = `<article class="dm-document" data-dm-document-version="${document.model_version}" data-dm-root-id="${escapeAttribute(document.root_id)}" lang="${escapeAttribute(document.metadata.language)}"${layoutStyle}><template data-dm-document-model="${document.model_version}">${safeMetadataJson(document)}</template>${document.headers
    .map((region) => renderRegion('header', region))
    .join('')}<main>${renderBlocks(document.blocks)}</main>${document.footers
    .map((region) => renderRegion('footer', region))
    .join('')}</article>`;
  const policyResult = applyControlledHtmlPolicy(rawHtml);
  if (!policyResult.valid) {
    throw new Error(`Serializer produced HTML that violates ${HTML_SANITIZATION_POLICY_VERSION}.`);
  }

  return {
    html: policyResult.html,
    css: CONTROLLED_DOCUMENT_CSS,
    sanitization_policy_version: HTML_SANITIZATION_POLICY_VERSION,
  };
};
