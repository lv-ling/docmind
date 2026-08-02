import type { ControlledDocument } from '../model/document.js';
import type {
  BlockNode,
  InlineNode,
  TemplateFieldBinding,
  TemplatePlaceholderNode,
  TemplateRepeatNode,
} from '../model/nodes.js';

export const TEMPLATE_BINDING_RULES = {
  allows_executable_expressions: false,
  allows_raw_html_values: false,
  requires_schema_version_match: true,
  requires_unique_binding_ids: true,
} as const;

export interface CollectedTemplateBinding {
  node_type: TemplatePlaceholderNode['type'] | TemplateRepeatNode['type'];
  binding: TemplateFieldBinding;
}

const collectInlineBindings = (
  content: InlineNode[],
  collected: CollectedTemplateBinding[],
): void => {
  for (const node of content) {
    if (node.type === 'template_placeholder') {
      collected.push({ node_type: node.type, binding: node.binding });
    }
  }
};

const collectBlockBindings = (blocks: BlockNode[], collected: CollectedTemplateBinding[]): void => {
  for (const block of blocks) {
    switch (block.type) {
      case 'paragraph':
      case 'heading':
        collectInlineBindings(block.content, collected);
        break;
      case 'list':
        for (const item of block.items) collectBlockBindings(item.blocks, collected);
        break;
      case 'table':
        for (const row of block.rows) {
          for (const cell of row.cells) collectBlockBindings(cell.blocks, collected);
        }
        break;
      case 'template_repeat':
        collected.push({ node_type: block.type, binding: block.binding });
        collectBlockBindings(block.blocks, collected);
        break;
      case 'image':
      case 'table_of_contents':
      case 'page_break':
      case 'page_marker':
        break;
    }
  }
};

export const collectTemplateBindings = (
  document: ControlledDocument,
): CollectedTemplateBinding[] => {
  const collected: CollectedTemplateBinding[] = [];

  for (const region of [...document.headers, ...document.footers]) {
    collectBlockBindings(region.blocks, collected);
  }
  collectBlockBindings(document.blocks, collected);

  return collected;
};
