import type {
  AuditMetadata,
  IsoDateTime,
  JsonObject,
  JsonValue,
  SchemaFieldId,
  SchemaId,
  SchemaTemplateId,
  SchemaVersionId,
  UserId,
  WorkspaceId,
} from '../common/index.js';

export const SCHEMA_VALUE_TYPES = [
  'string',
  'number',
  'integer',
  'boolean',
  'date',
  'datetime',
  'object',
  'array',
] as const;
export type SchemaValueType = (typeof SCHEMA_VALUE_TYPES)[number];

export const FIELD_SENSITIVITY_LEVELS = ['none', 'low', 'medium', 'high'] as const;
export type FieldSensitivityLevel = (typeof FIELD_SENSITIVITY_LEVELS)[number];

export const SCHEMA_VERSION_STATUSES = ['draft', 'published', 'superseded'] as const;
export type SchemaVersionStatus = (typeof SCHEMA_VERSION_STATUSES)[number];

export type SchemaFieldDefault = { kind: 'none' } | { kind: 'literal'; value: JsonValue };

export interface SchemaFieldConstraints {
  format: string | null;
  pattern: string | null;
  enum_values: JsonValue[];
  min_length: number | null;
  max_length: number | null;
  minimum: number | null;
  maximum: number | null;
}

export interface SchemaFieldDisplayOptions {
  /** Presentation-only masking instruction; never changes the stored extracted value. */
  mask: 'none' | 'partial' | 'full';
  /** Role keys granted access to the unmasked field value. */
  view_role_keys: string[];
}

export interface SchemaFieldDefinition {
  id: SchemaFieldId;
  key: string;
  json_path: string;
  description: string;
  value_type: SchemaValueType;
  array_item_type: SchemaValueType | null;
  required: boolean;
  nullable: boolean;
  default: SchemaFieldDefault;
  sensitivity: FieldSensitivityLevel;
  constraints: SchemaFieldConstraints;
  examples: JsonValue[];
  extraction_hint: string | null;
  display: SchemaFieldDisplayOptions;
  /** Non-executable tenant metadata for future UI and workflow extensions. */
  metadata: JsonObject;
  position: number;
}

/** Client-supplied field definition. Entity IDs are allocated by the service. */
export interface SchemaFieldInput {
  key: string;
  json_path: string;
  description: string;
  value_type: SchemaValueType;
  array_item_type: SchemaValueType | null;
  required: boolean;
  nullable: boolean;
  default: SchemaFieldDefault;
  sensitivity: FieldSensitivityLevel;
  constraints: SchemaFieldConstraints;
  examples: JsonValue[];
  extraction_hint: string | null;
  display: SchemaFieldDisplayOptions;
  metadata: JsonObject;
  position: number;
}

export interface ExtractionSchema extends AuditMetadata {
  id: SchemaId;
  workspace_id: WorkspaceId;
  name: string;
  description: string;
  current_version_id: SchemaVersionId | null;
}

export interface SchemaVersion {
  id: SchemaVersionId;
  schema_id: SchemaId;
  workspace_id: WorkspaceId;
  version_number: number;
  status: SchemaVersionStatus;
  fields: SchemaFieldDefinition[];
  json_schema: JsonObject;
  change_summary: string;
  created_at: IsoDateTime;
  created_by: UserId;
  published_at: IsoDateTime | null;
}

/** Reusable pointer to an immutable, published schema version snapshot. */
export interface SchemaTemplate extends AuditMetadata {
  id: SchemaTemplateId;
  workspace_id: WorkspaceId;
  name: string;
  description: string;
  current_schema_version_id: SchemaVersionId;
}

export interface CreateSchemaRequest {
  name: string;
  description: string;
  fields: SchemaFieldInput[];
}

export interface CreateSchemaVersionRequest {
  fields: SchemaFieldInput[];
  change_summary: string;
}

export interface CreateSchemaTemplateRequest {
  name: string;
  description: string;
  schema_version_id: SchemaVersionId;
}
