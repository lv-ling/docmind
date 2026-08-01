import { DOCUMENT_MODEL_VERSION, type ControlledDocument } from '../model/document.js';

export const DOCUMENT_VALIDATION_LIMITS = {
  max_nodes: 100_000,
  max_depth: 100,
  max_metadata_json_bytes: 5 * 1024 * 1024,
} as const;

export interface DocumentValidationIssue {
  code: string;
  path: string;
  message: string;
}

export interface DocumentValidationResult {
  valid: boolean;
  issues: DocumentValidationIssue[];
}

export class InvalidControlledDocumentError extends Error {
  readonly issues: DocumentValidationIssue[];

  constructor(issues: DocumentValidationIssue[]) {
    super(
      `Controlled document is invalid (${issues.length} issue${issues.length === 1 ? '' : 's'})`,
    );
    this.name = 'InvalidControlledDocumentError';
    this.issues = issues;
  }
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

const isNonEmptyString = (value: unknown): value is string =>
  typeof value === 'string' && value.length > 0;

const isNonNegativeInteger = (value: unknown): value is number =>
  Number.isInteger(value) && Number(value) >= 0;

const validateLength = (
  value: unknown,
  path: string,
  context: ValidationContext,
  allowNegative = false,
): void => {
  if (!isRecord(value)) {
    addIssue(context, path, 'LENGTH_INVALID', 'Length must be an object.');
    return;
  }
  if (
    typeof value.value !== 'number' ||
    !Number.isFinite(value.value) ||
    (!allowNegative && value.value < 0)
  ) {
    addIssue(context, `${path}.value`, 'LENGTH_VALUE_INVALID', 'Length value is invalid.');
  }
  if (!['pt', 'px', 'mm', 'cm', 'in', 'percent'].includes(String(value.unit))) {
    addIssue(context, `${path}.unit`, 'LENGTH_UNIT_INVALID', 'Length unit is unsupported.');
  }
};

const validateStyleObject = (
  value: unknown,
  path: string,
  context: ValidationContext,
): value is Record<string, unknown> => {
  if (!isRecord(value)) {
    addIssue(context, path, 'STYLE_INVALID', 'Style must be an object.');
    return false;
  }
  return true;
};

const validatePageLayout = (value: unknown, path: string, context: ValidationContext): void => {
  if (!isRecord(value)) {
    addIssue(context, path, 'PAGE_LAYOUT_INVALID', 'Page layout must be an object.');
    return;
  }
  validateLength(value.width, `${path}.width`, context);
  validateLength(value.height, `${path}.height`, context);
  validateLength(value.header_distance, `${path}.header_distance`, context);
  validateLength(value.footer_distance, `${path}.footer_distance`, context);
  if (!isRecord(value.margins)) {
    addIssue(context, `${path}.margins`, 'PAGE_MARGINS_INVALID', 'Page margins must be an object.');
  } else {
    for (const side of ['top', 'right', 'bottom', 'left'] as const) {
      validateLength(value.margins[side], `${path}.margins.${side}`, context);
    }
  }
};

interface ValidationContext {
  issues: DocumentValidationIssue[];
  node_ids: Set<string>;
  binding_ids: Set<string>;
  schema_version_id: string | null;
  node_count: number;
}

const addIssue = (
  context: ValidationContext,
  path: string,
  code: string,
  message: string,
): void => {
  context.issues.push({ code, path, message });
};

const validateNodeBase = (
  node: Record<string, unknown>,
  path: string,
  depth: number,
  context: ValidationContext,
): boolean => {
  context.node_count += 1;
  if (context.node_count > DOCUMENT_VALIDATION_LIMITS.max_nodes) {
    addIssue(context, path, 'NODE_LIMIT_EXCEEDED', 'Document contains too many nodes.');
    return false;
  }
  if (depth > DOCUMENT_VALIDATION_LIMITS.max_depth) {
    addIssue(context, path, 'DEPTH_LIMIT_EXCEEDED', 'Document nesting is too deep.');
    return false;
  }
  if (!isNonEmptyString(node.id)) {
    addIssue(context, `${path}.id`, 'NODE_ID_REQUIRED', 'Every node requires a stable ID.');
  } else if (context.node_ids.has(node.id)) {
    addIssue(context, `${path}.id`, 'NODE_ID_DUPLICATE', `Duplicate node ID: ${node.id}`);
  } else {
    context.node_ids.add(node.id);
  }
  if (!isRecord(node.attributes)) {
    addIssue(
      context,
      `${path}.attributes`,
      'ATTRIBUTES_INVALID',
      'Node attributes must be an object.',
    );
  }
  return true;
};

const validateBinding = (
  value: unknown,
  path: string,
  context: ValidationContext,
  requiredType?: 'array',
): void => {
  if (!isRecord(value)) {
    addIssue(context, path, 'BINDING_INVALID', 'Template binding must be an object.');
    return;
  }
  if (!isNonEmptyString(value.id)) {
    addIssue(context, `${path}.id`, 'BINDING_ID_REQUIRED', 'Binding ID is required.');
  } else if (context.binding_ids.has(value.id)) {
    addIssue(context, `${path}.id`, 'BINDING_ID_DUPLICATE', `Duplicate binding ID: ${value.id}`);
  } else {
    context.binding_ids.add(value.id);
  }
  if (!isNonEmptyString(value.schema_version_id)) {
    addIssue(
      context,
      `${path}.schema_version_id`,
      'SCHEMA_VERSION_REQUIRED',
      'Schema version is required.',
    );
  } else if (context.schema_version_id !== value.schema_version_id) {
    addIssue(
      context,
      `${path}.schema_version_id`,
      'SCHEMA_VERSION_MISMATCH',
      'Binding schema version must match the document schema version.',
    );
  }
  if (!isNonEmptyString(value.schema_field_id)) {
    addIssue(
      context,
      `${path}.schema_field_id`,
      'SCHEMA_FIELD_REQUIRED',
      'Schema field is required.',
    );
  }
  if (!isNonEmptyString(value.json_path) || !value.json_path.startsWith('$')) {
    addIssue(
      context,
      `${path}.json_path`,
      'JSON_PATH_INVALID',
      'Binding path must be a JSON path.',
    );
  }
  if (requiredType !== undefined && value.value_type !== requiredType) {
    addIssue(
      context,
      `${path}.value_type`,
      'BINDING_TYPE_INVALID',
      `Binding must be ${requiredType}.`,
    );
  }
  if (!['absolute', 'relative_to_repeat'].includes(String(value.path_scope))) {
    addIssue(context, `${path}.path_scope`, 'PATH_SCOPE_INVALID', 'Unsupported path scope.');
  }
  if (
    !['plain_text', 'sanitized_rich_text', 'resource_reference'].includes(
      String(value.value_policy),
    )
  ) {
    addIssue(context, `${path}.value_policy`, 'VALUE_POLICY_INVALID', 'Unsupported value policy.');
  }
  if (!['show_label', 'empty', 'error'].includes(String(value.missing_behavior))) {
    addIssue(
      context,
      `${path}.missing_behavior`,
      'MISSING_BEHAVIOR_INVALID',
      'Unsupported behavior.',
    );
  }
  if (value.format !== null && typeof value.format !== 'string') {
    addIssue(context, `${path}.format`, 'FORMAT_INVALID', 'Format must be a string or null.');
  }
};

const validateInline = (
  value: unknown,
  path: string,
  depth: number,
  context: ValidationContext,
): void => {
  if (!isRecord(value) || !isNonEmptyString(value.type)) {
    addIssue(context, path, 'INLINE_NODE_INVALID', 'Inline node must be an object with a type.');
    return;
  }
  if (!validateNodeBase(value, path, depth, context)) return;

  switch (value.type) {
    case 'text':
      if (typeof value.text !== 'string') {
        addIssue(context, `${path}.text`, 'TEXT_REQUIRED', 'Text node content must be a string.');
      }
      validateStyleObject(value.style, `${path}.style`, context);
      break;
    case 'line_break':
    case 'tab':
      break;
    case 'dynamic_field':
      if (!['page_number', 'page_count', 'current_date'].includes(String(value.field))) {
        addIssue(context, `${path}.field`, 'DYNAMIC_FIELD_INVALID', 'Unsupported dynamic field.');
      }
      validateStyleObject(value.style, `${path}.style`, context);
      break;
    case 'template_placeholder':
      validateBinding(value.binding, `${path}.binding`, context);
      if (typeof value.label !== 'string') {
        addIssue(
          context,
          `${path}.label`,
          'PLACEHOLDER_LABEL_INVALID',
          'Placeholder label is required.',
        );
      }
      validateStyleObject(value.style, `${path}.style`, context);
      break;
    default:
      addIssue(
        context,
        `${path}.type`,
        'INLINE_NODE_TYPE_INVALID',
        `Unsupported inline node: ${value.type}`,
      );
  }
};

const validateBlockArray = (
  value: unknown,
  path: string,
  depth: number,
  context: ValidationContext,
): void => {
  if (!Array.isArray(value)) {
    addIssue(context, path, 'BLOCKS_INVALID', 'Blocks must be an array.');
    return;
  }
  value.forEach((block, index) => validateBlock(block, `${path}[${index}]`, depth, context));
};

const validateBlock = (
  value: unknown,
  path: string,
  depth: number,
  context: ValidationContext,
): void => {
  if (!isRecord(value) || !isNonEmptyString(value.type)) {
    addIssue(context, path, 'BLOCK_NODE_INVALID', 'Block node must be an object with a type.');
    return;
  }
  if (!validateNodeBase(value, path, depth, context)) return;

  switch (value.type) {
    case 'paragraph':
    case 'heading':
      validateStyleObject(value.style, `${path}.style`, context);
      if (!Array.isArray(value.content)) {
        addIssue(
          context,
          `${path}.content`,
          'INLINE_CONTENT_INVALID',
          'Inline content must be an array.',
        );
      } else {
        value.content.forEach((node, index) =>
          validateInline(node, `${path}.content[${index}]`, depth + 1, context),
        );
      }
      if (value.type === 'heading' && ![1, 2, 3, 4, 5, 6].includes(Number(value.level))) {
        addIssue(context, `${path}.level`, 'HEADING_LEVEL_INVALID', 'Heading level must be 1-6.');
      }
      break;
    case 'list':
      if (!Array.isArray(value.items)) {
        addIssue(context, `${path}.items`, 'LIST_ITEMS_INVALID', 'List items must be an array.');
      } else {
        value.items.forEach((item, index) => {
          const itemPath = `${path}.items[${index}]`;
          if (!isRecord(item) || item.type !== 'list_item') {
            addIssue(context, itemPath, 'LIST_ITEM_INVALID', 'List item node is invalid.');
            return;
          }
          if (!validateNodeBase(item, itemPath, depth + 1, context)) return;
          validateBlockArray(item.blocks, `${itemPath}.blocks`, depth + 2, context);
        });
      }
      break;
    case 'table':
      if (!Array.isArray(value.rows)) {
        addIssue(context, `${path}.rows`, 'TABLE_ROWS_INVALID', 'Table rows must be an array.');
      } else {
        value.rows.forEach((row, rowIndex) => {
          const rowPath = `${path}.rows[${rowIndex}]`;
          if (!isRecord(row) || row.type !== 'table_row') {
            addIssue(context, rowPath, 'TABLE_ROW_INVALID', 'Table row node is invalid.');
            return;
          }
          if (!validateNodeBase(row, rowPath, depth + 1, context)) return;
          if (!Array.isArray(row.cells)) {
            addIssue(
              context,
              `${rowPath}.cells`,
              'TABLE_CELLS_INVALID',
              'Table cells must be an array.',
            );
            return;
          }
          row.cells.forEach((cell, cellIndex) => {
            const cellPath = `${rowPath}.cells[${cellIndex}]`;
            if (!isRecord(cell) || cell.type !== 'table_cell') {
              addIssue(context, cellPath, 'TABLE_CELL_INVALID', 'Table cell node is invalid.');
              return;
            }
            if (!validateNodeBase(cell, cellPath, depth + 2, context)) return;
            validateStyleObject(cell.style, `${cellPath}.style`, context);
            if (!Number.isInteger(cell.row_span) || Number(cell.row_span) < 1) {
              addIssue(
                context,
                `${cellPath}.row_span`,
                'ROW_SPAN_INVALID',
                'Row span must be positive.',
              );
            }
            if (!Number.isInteger(cell.column_span) || Number(cell.column_span) < 1) {
              addIssue(
                context,
                `${cellPath}.column_span`,
                'COLUMN_SPAN_INVALID',
                'Column span must be positive.',
              );
            }
            validateBlockArray(cell.blocks, `${cellPath}.blocks`, depth + 3, context);
          });
        });
      }
      break;
    case 'image':
      if (!isNonEmptyString(value.resource_id)) {
        addIssue(
          context,
          `${path}.resource_id`,
          'IMAGE_RESOURCE_REQUIRED',
          'Image resource is required.',
        );
      }
      if (typeof value.alt_text !== 'string') {
        addIssue(context, `${path}.alt_text`, 'IMAGE_ALT_REQUIRED', 'Image alt text is required.');
      }
      validateLength(value.width, `${path}.width`, context);
      validateLength(value.height, `${path}.height`, context);
      break;
    case 'table_of_contents':
      if (!Array.isArray(value.entries)) {
        addIssue(
          context,
          `${path}.entries`,
          'TOC_ENTRIES_INVALID',
          'TOC entries must be an array.',
        );
      }
      break;
    case 'page_break':
      break;
    case 'page_marker':
      if (!Number.isInteger(value.page_number) || Number(value.page_number) < 1) {
        addIssue(
          context,
          `${path}.page_number`,
          'PAGE_NUMBER_INVALID',
          'Page number must be positive.',
        );
      }
      break;
    case 'template_repeat':
      validateBinding(value.binding, `${path}.binding`, context, 'array');
      if (
        !isNonEmptyString(value.item_alias) ||
        !/^[A-Za-z_][A-Za-z0-9_]*$/u.test(value.item_alias)
      ) {
        addIssue(context, `${path}.item_alias`, 'ITEM_ALIAS_INVALID', 'Repeat alias is invalid.');
      }
      if (!isNonNegativeInteger(value.min_items)) {
        addIssue(
          context,
          `${path}.min_items`,
          'MIN_ITEMS_INVALID',
          'Minimum item count is invalid.',
        );
      }
      if (
        value.max_items !== null &&
        (!isNonNegativeInteger(value.max_items) ||
          (isNonNegativeInteger(value.min_items) && value.max_items < value.min_items))
      ) {
        addIssue(
          context,
          `${path}.max_items`,
          'MAX_ITEMS_INVALID',
          'Maximum item count is invalid.',
        );
      }
      validateBlockArray(value.blocks, `${path}.blocks`, depth + 1, context);
      break;
    default:
      addIssue(
        context,
        `${path}.type`,
        'BLOCK_NODE_TYPE_INVALID',
        `Unsupported block node: ${value.type}`,
      );
  }
};

export const validateControlledDocument = (value: unknown): DocumentValidationResult => {
  const context: ValidationContext = {
    issues: [],
    node_ids: new Set(),
    binding_ids: new Set(),
    schema_version_id: null,
    node_count: 0,
  };

  if (!isRecord(value)) {
    return {
      valid: false,
      issues: [{ code: 'DOCUMENT_INVALID', path: '$', message: 'Document must be an object.' }],
    };
  }
  if (value.model_version !== DOCUMENT_MODEL_VERSION) {
    addIssue(
      context,
      '$.model_version',
      'MODEL_VERSION_UNSUPPORTED',
      'Unsupported document model version.',
    );
  }
  if (!isNonEmptyString(value.root_id)) {
    addIssue(context, '$.root_id', 'ROOT_ID_REQUIRED', 'Document root ID is required.');
  }
  if (
    value.template_schema_version_id !== null &&
    !isNonEmptyString(value.template_schema_version_id)
  ) {
    addIssue(
      context,
      '$.template_schema_version_id',
      'SCHEMA_VERSION_INVALID',
      'Schema version is invalid.',
    );
  } else {
    context.schema_version_id = value.template_schema_version_id;
  }
  if (!isRecord(value.metadata)) {
    addIssue(context, '$.metadata', 'METADATA_INVALID', 'Document metadata must be an object.');
  } else {
    if (typeof value.metadata.title !== 'string') {
      addIssue(context, '$.metadata.title', 'TITLE_INVALID', 'Document title must be a string.');
    }
    if (!isNonEmptyString(value.metadata.language)) {
      addIssue(
        context,
        '$.metadata.language',
        'LANGUAGE_INVALID',
        'Document language is required.',
      );
    }
  }
  validatePageLayout(value.page_layout, '$.page_layout', context);

  for (const regionName of ['headers', 'footers'] as const) {
    const regions = value[regionName];
    if (!Array.isArray(regions)) {
      addIssue(context, `$.${regionName}`, 'REGIONS_INVALID', `${regionName} must be an array.`);
      continue;
    }
    regions.forEach((region, index) => {
      const path = `$.${regionName}[${index}]`;
      if (!isRecord(region)) {
        addIssue(context, path, 'REGION_INVALID', 'Header/footer region must be an object.');
        return;
      }
      if (!['default', 'first_page', 'even_pages'].includes(String(region.variant))) {
        addIssue(
          context,
          `${path}.variant`,
          'REGION_VARIANT_INVALID',
          'Unsupported region variant.',
        );
      }
      validateBlockArray(region.blocks, `${path}.blocks`, 1, context);
    });
  }
  validateBlockArray(value.blocks, '$.blocks', 1, context);

  return { valid: context.issues.length === 0, issues: context.issues };
};

export function assertValidControlledDocument(value: unknown): asserts value is ControlledDocument {
  const result = validateControlledDocument(value);
  if (!result.valid) throw new InvalidControlledDocumentError(result.issues);
}
