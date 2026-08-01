CREATE TABLE document_template (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  source_document_id UUID NOT NULL REFERENCES source_document(id),
  source_version_id UUID NOT NULL REFERENCES source_version(id),
  conversion_job_id UUID NOT NULL REFERENCES async_job(id),
  name VARCHAR(200) NOT NULL,
  current_version_id UUID,
  conversion_status VARCHAR(20) NOT NULL,
  failure_code VARCHAR(100),
  created_by UUID NOT NULL REFERENCES app_user(id),
  creation_idempotency_key VARCHAR(128) NOT NULL,
  creation_request_hash CHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  updated_by UUID NOT NULL REFERENCES app_user(id),
  revision BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_document_template_job UNIQUE (conversion_job_id),
  CONSTRAINT uq_document_template_creation_key
    UNIQUE (source_version_id, created_by, creation_idempotency_key),
  CONSTRAINT ck_document_template_conversion_status
    CHECK (conversion_status IN ('QUEUED', 'RUNNING', 'READY', 'RETRYING', 'FAILED'))
);

CREATE INDEX ix_document_template_workspace_updated
  ON document_template(workspace_id, updated_at DESC);

CREATE TABLE parsed_content (
  id UUID PRIMARY KEY,
  source_version_id UUID NOT NULL REFERENCES source_version(id),
  parser_name VARCHAR(100) NOT NULL,
  parser_version VARCHAR(100) NOT NULL,
  document_model_envelope JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE document_template_version (
  id UUID PRIMARY KEY,
  template_id UUID NOT NULL REFERENCES document_template(id),
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  source_version_id UUID NOT NULL REFERENCES source_version(id),
  parsed_content_id UUID NOT NULL REFERENCES parsed_content(id),
  resource_version_id UUID NOT NULL,
  version_number INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL,
  document_model_envelope JSONB NOT NULL,
  html_envelope JSONB NOT NULL,
  css_text TEXT NOT NULL,
  sanitization_policy_version VARCHAR(50) NOT NULL,
  change_summary VARCHAR(1000) NOT NULL,
  diff_envelope JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  created_by UUID NOT NULL REFERENCES app_user(id),
  published_at TIMESTAMPTZ,
  published_by UUID REFERENCES app_user(id),
  revision BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_document_template_version_number UNIQUE (template_id, version_number),
  CONSTRAINT fk_document_template_version_resources
    FOREIGN KEY (resource_version_id) REFERENCES document_template_version(id),
  CONSTRAINT ck_document_template_version_number CHECK (version_number > 0),
  CONSTRAINT ck_document_template_version_status
    CHECK (status IN ('GENERATED', 'CHECKING', 'PUBLISHED', 'SUPERSEDED')),
  CONSTRAINT ck_document_template_version_publish CHECK (
    (status = 'PUBLISHED' AND published_at IS NOT NULL AND published_by IS NOT NULL)
    OR (status <> 'PUBLISHED')
  )
);

ALTER TABLE document_template
  ADD CONSTRAINT fk_document_template_current_version
  FOREIGN KEY (current_version_id) REFERENCES document_template_version(id);

CREATE TABLE document_template_resource (
  id UUID PRIMARY KEY,
  template_version_id UUID NOT NULL REFERENCES document_template_version(id) ON DELETE CASCADE,
  kind VARCHAR(20) NOT NULL,
  content_type VARCHAR(255) NOT NULL,
  byte_size BIGINT NOT NULL,
  sha256 CHAR(64) NOT NULL,
  object_bucket VARCHAR(63) NOT NULL,
  object_key VARCHAR(1024) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_document_template_resource_kind
    CHECK (kind IN ('IMAGE', 'STYLESHEET', 'FONT', 'ATTACHMENT')),
  CONSTRAINT ck_document_template_resource_size CHECK (byte_size >= 0)
);

CREATE INDEX ix_document_template_resource_version
  ON document_template_resource(template_version_id);

CREATE TABLE document_conversion_warning (
  id UUID PRIMARY KEY,
  template_version_id UUID NOT NULL REFERENCES document_template_version(id) ON DELETE CASCADE,
  severity VARCHAR(20) NOT NULL,
  code VARCHAR(100) NOT NULL,
  message VARCHAR(500) NOT NULL,
  source_node_id VARCHAR(255),
  page_number INTEGER,
  fallback VARCHAR(500),
  is_blocking BOOLEAN NOT NULL,
  warning_position INTEGER NOT NULL,
  CONSTRAINT uq_document_conversion_warning_position
    UNIQUE (template_version_id, warning_position),
  CONSTRAINT ck_document_conversion_warning_severity
    CHECK (severity IN ('INFO', 'WARNING', 'ERROR')),
  CONSTRAINT ck_document_conversion_warning_page
    CHECK (page_number IS NULL OR page_number > 0)
);

CREATE TABLE document_template_operation (
  id UUID PRIMARY KEY,
  template_id UUID NOT NULL REFERENCES document_template(id) ON DELETE CASCADE,
  result_version_id UUID NOT NULL REFERENCES document_template_version(id),
  operation_type VARCHAR(30) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  actor_id UUID NOT NULL REFERENCES app_user(id),
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_document_template_operation_key
    UNIQUE (actor_id, operation_type, idempotency_key),
  CONSTRAINT ck_document_template_operation_type
    CHECK (operation_type IN ('CREATE_VERSION', 'PUBLISH', 'ROLLBACK'))
);

COMMENT ON COLUMN parsed_content.document_model_envelope IS
  'Encrypted controlled document model; source text must not be stored as plaintext.';
COMMENT ON COLUMN document_template_version.html_envelope IS
  'Encrypted allowlisted HTML generated by the server; arbitrary client HTML is never stored.';
