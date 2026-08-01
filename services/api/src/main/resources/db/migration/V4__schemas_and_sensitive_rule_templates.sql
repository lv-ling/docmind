CREATE TABLE extraction_schema (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  name VARCHAR(100) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  current_version_id UUID,
  created_by UUID NOT NULL REFERENCES app_user(id),
  updated_by UUID NOT NULL REFERENCES app_user(id),
  creation_idempotency_key VARCHAR(128) NOT NULL,
  creation_request_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT uq_extraction_schema_creation_key
    UNIQUE (workspace_id, created_by, creation_idempotency_key),
  CONSTRAINT ck_extraction_schema_name_nonblank CHECK (length(trim(name)) > 0)
);

CREATE INDEX ix_extraction_schema_workspace_updated
  ON extraction_schema(workspace_id, updated_at DESC)
  WHERE deleted_at IS NULL;

CREATE TABLE extraction_schema_version (
  id UUID PRIMARY KEY,
  schema_id UUID NOT NULL REFERENCES extraction_schema(id),
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  version_number INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL,
  json_schema JSONB NOT NULL,
  change_summary VARCHAR(1000) NOT NULL,
  created_by UUID NOT NULL REFERENCES app_user(id),
  creation_idempotency_key VARCHAR(128),
  creation_request_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  published_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_extraction_schema_version_number UNIQUE (schema_id, version_number),
  CONSTRAINT uq_extraction_schema_version_creation_key
    UNIQUE (schema_id, created_by, creation_idempotency_key),
  CONSTRAINT ck_extraction_schema_version_number CHECK (version_number > 0),
  CONSTRAINT ck_extraction_schema_version_status
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'SUPERSEDED'))
);

ALTER TABLE extraction_schema
  ADD CONSTRAINT fk_extraction_schema_current_version
  FOREIGN KEY (current_version_id) REFERENCES extraction_schema_version(id);

CREATE TABLE extraction_schema_field (
  id UUID PRIMARY KEY,
  schema_version_id UUID NOT NULL REFERENCES extraction_schema_version(id),
  field_key VARCHAR(64) NOT NULL,
  json_path VARCHAR(500) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  value_type VARCHAR(20) NOT NULL,
  array_item_type VARCHAR(20),
  is_required BOOLEAN NOT NULL,
  nullable BOOLEAN NOT NULL,
  default_kind VARCHAR(20) NOT NULL,
  default_value JSONB,
  sensitivity VARCHAR(20) NOT NULL,
  constraints_json JSONB NOT NULL,
  examples_json JSONB NOT NULL,
  extraction_hint VARCHAR(2000),
  display_json JSONB NOT NULL,
  metadata_json JSONB NOT NULL,
  position INTEGER NOT NULL,
  CONSTRAINT uq_extraction_schema_field_key UNIQUE (schema_version_id, field_key),
  CONSTRAINT uq_extraction_schema_field_path UNIQUE (schema_version_id, json_path),
  CONSTRAINT uq_extraction_schema_field_position UNIQUE (schema_version_id, position),
  CONSTRAINT ck_extraction_schema_field_position CHECK (position >= 0),
  CONSTRAINT ck_extraction_schema_field_value_type
    CHECK (value_type IN ('STRING', 'NUMBER', 'INTEGER', 'BOOLEAN', 'DATE', 'DATETIME', 'OBJECT', 'ARRAY')),
  CONSTRAINT ck_extraction_schema_field_array_item_type
    CHECK (array_item_type IS NULL OR array_item_type IN ('STRING', 'NUMBER', 'INTEGER', 'BOOLEAN', 'DATE', 'DATETIME', 'OBJECT', 'ARRAY')),
  CONSTRAINT ck_extraction_schema_field_default_kind CHECK (default_kind IN ('NONE', 'LITERAL')),
  CONSTRAINT ck_extraction_schema_field_sensitivity CHECK (sensitivity IN ('NONE', 'LOW', 'MEDIUM', 'HIGH')),
  CONSTRAINT ck_extraction_schema_field_array_pair CHECK (
    (value_type = 'ARRAY' AND array_item_type IS NOT NULL)
    OR (value_type <> 'ARRAY' AND array_item_type IS NULL)
  )
);

CREATE TABLE schema_template (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  name VARCHAR(100) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  current_schema_version_id UUID NOT NULL REFERENCES extraction_schema_version(id),
  created_by UUID NOT NULL REFERENCES app_user(id),
  updated_by UUID NOT NULL REFERENCES app_user(id),
  creation_idempotency_key VARCHAR(128) NOT NULL,
  creation_request_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT uq_schema_template_creation_key
    UNIQUE (workspace_id, created_by, creation_idempotency_key),
  CONSTRAINT ck_schema_template_name_nonblank CHECK (length(trim(name)) > 0)
);

CREATE INDEX ix_schema_template_workspace_updated
  ON schema_template(workspace_id, updated_at DESC)
  WHERE deleted_at IS NULL;

CREATE TABLE sensitive_rule_template (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  name VARCHAR(100) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  current_version_id UUID,
  created_by UUID NOT NULL REFERENCES app_user(id),
  updated_by UUID NOT NULL REFERENCES app_user(id),
  creation_idempotency_key VARCHAR(128) NOT NULL,
  creation_request_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT uq_sensitive_rule_template_creation_key
    UNIQUE (workspace_id, created_by, creation_idempotency_key),
  CONSTRAINT ck_sensitive_rule_template_name_nonblank CHECK (length(trim(name)) > 0)
);

CREATE INDEX ix_sensitive_rule_template_workspace_updated
  ON sensitive_rule_template(workspace_id, updated_at DESC)
  WHERE deleted_at IS NULL;

CREATE TABLE sensitive_rule_template_version (
  id UUID PRIMARY KEY,
  template_id UUID NOT NULL REFERENCES sensitive_rule_template(id),
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  version_number INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL,
  change_summary VARCHAR(1000) NOT NULL,
  created_by UUID NOT NULL REFERENCES app_user(id),
  creation_idempotency_key VARCHAR(128),
  creation_request_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  published_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_sensitive_rule_template_version_number UNIQUE (template_id, version_number),
  CONSTRAINT uq_sensitive_rule_template_version_creation_key
    UNIQUE (template_id, created_by, creation_idempotency_key),
  CONSTRAINT ck_sensitive_rule_template_version_number CHECK (version_number > 0),
  CONSTRAINT ck_sensitive_rule_template_version_status
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'SUPERSEDED'))
);

ALTER TABLE sensitive_rule_template
  ADD CONSTRAINT fk_sensitive_rule_template_current_version
  FOREIGN KEY (current_version_id) REFERENCES sensitive_rule_template_version(id);

CREATE TABLE sensitive_rule (
  id UUID PRIMARY KEY,
  template_version_id UUID NOT NULL REFERENCES sensitive_rule_template_version(id),
  rule_key VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  data_type VARCHAR(40) NOT NULL,
  recognizer_kind VARCHAR(20) NOT NULL,
  locales_json JSONB NOT NULL,
  country_codes_json JSONB NOT NULL,
  regex_pattern VARCHAR(2000),
  regex_dialect VARCHAR(20),
  dictionary_terms_json JSONB NOT NULL,
  validator_name VARCHAR(100),
  confidence_threshold NUMERIC(4,3) NOT NULL,
  priority INTEGER NOT NULL,
  enabled BOOLEAN NOT NULL,
  position INTEGER NOT NULL,
  CONSTRAINT uq_sensitive_rule_key UNIQUE (template_version_id, rule_key),
  CONSTRAINT uq_sensitive_rule_position UNIQUE (template_version_id, position),
  CONSTRAINT ck_sensitive_rule_data_type CHECK (data_type IN (
    'CHINA_NATIONAL_ID', 'IDENTITY_DOCUMENT', 'PASSPORT', 'PHONE_NUMBER',
    'EMAIL_ADDRESS', 'CREDIT_CARD', 'BANK_ACCOUNT', 'IP_ADDRESS',
    'PERSON_NAME', 'LOCATION', 'CUSTOM'
  )),
  CONSTRAINT ck_sensitive_rule_recognizer_kind
    CHECK (recognizer_kind IN ('PRESIDIO', 'REGEX', 'DICTIONARY', 'VALIDATOR')),
  CONSTRAINT ck_sensitive_rule_regex_dialect
    CHECK (regex_dialect IS NULL OR regex_dialect = 'RE2'),
  CONSTRAINT ck_sensitive_rule_confidence
    CHECK (confidence_threshold BETWEEN 0 AND 1),
  CONSTRAINT ck_sensitive_rule_priority CHECK (priority BETWEEN -1000 AND 1000),
  CONSTRAINT ck_sensitive_rule_position CHECK (position >= 0)
);
