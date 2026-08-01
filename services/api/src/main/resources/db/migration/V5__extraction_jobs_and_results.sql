CREATE TABLE async_job (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  job_type VARCHAR(40) NOT NULL,
  aggregate_type VARCHAR(80) NOT NULL,
  aggregate_id UUID NOT NULL,
  status VARCHAR(20) NOT NULL,
  attempt_count INTEGER NOT NULL,
  max_attempts INTEGER NOT NULL,
  available_at TIMESTAMPTZ NOT NULL,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  failure_code VARCHAR(100),
  revision BIGINT NOT NULL DEFAULT 0,
  creation_request_id UUID NOT NULL,
  created_by UUID NOT NULL REFERENCES app_user(id),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_async_job_aggregate UNIQUE (aggregate_type, aggregate_id),
  CONSTRAINT ck_async_job_type
    CHECK (job_type IN ('EXTRACTION', 'TEMPLATE_CONVERSION', 'PROOFREADING', 'DIFF')),
  CONSTRAINT ck_async_job_status
    CHECK (status IN ('QUEUED', 'RUNNING', 'RETRYING', 'SUCCEEDED', 'FAILED')),
  CONSTRAINT ck_async_job_attempts
    CHECK (attempt_count >= 0 AND max_attempts > 0 AND attempt_count <= max_attempts),
  CONSTRAINT ck_async_job_terminal_time CHECK (
    (status IN ('SUCCEEDED', 'FAILED') AND completed_at IS NOT NULL)
    OR (status NOT IN ('SUCCEEDED', 'FAILED') AND completed_at IS NULL)
  )
);

CREATE INDEX ix_async_job_dispatch
  ON async_job(status, available_at, created_at)
  WHERE status IN ('QUEUED', 'RETRYING');

CREATE INDEX ix_async_job_workspace_created
  ON async_job(workspace_id, created_at DESC);

CREATE TABLE extraction_run (
  id UUID PRIMARY KEY,
  job_id UUID NOT NULL REFERENCES async_job(id),
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  source_version_id UUID NOT NULL REFERENCES source_version(id),
  schema_version_id UUID NOT NULL REFERENCES extraction_schema_version(id),
  sensitive_rule_template_version_id UUID REFERENCES sensitive_rule_template_version(id),
  status VARCHAR(30) NOT NULL,
  result_data_envelope JSONB,
  contains_sensitive_values BOOLEAN NOT NULL DEFAULT FALSE,
  model_provider VARCHAR(100),
  model_name VARCHAR(200),
  prompt_version VARCHAR(100),
  input_tokens INTEGER,
  output_tokens INTEGER,
  validation_errors JSONB NOT NULL DEFAULT '[]'::jsonb,
  failure_code VARCHAR(100),
  created_by UUID NOT NULL REFERENCES app_user(id),
  creation_idempotency_key VARCHAR(128) NOT NULL,
  creation_request_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ,
  approved_by UUID REFERENCES app_user(id),
  approved_at TIMESTAMPTZ,
  approval_note VARCHAR(2000),
  revision BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_extraction_run_job UNIQUE (job_id),
  CONSTRAINT uq_extraction_run_creation_key
    UNIQUE (source_version_id, created_by, creation_idempotency_key),
  CONSTRAINT ck_extraction_run_status CHECK (
    status IN ('QUEUED', 'RUNNING', 'REVIEW_REQUIRED', 'APPROVED', 'FAILED', 'RETRYING')
  ),
  CONSTRAINT ck_extraction_run_token_counts CHECK (
    (input_tokens IS NULL OR input_tokens >= 0)
    AND (output_tokens IS NULL OR output_tokens >= 0)
  ),
  CONSTRAINT ck_extraction_run_completion CHECK (
    (status IN ('REVIEW_REQUIRED', 'APPROVED', 'FAILED') AND completed_at IS NOT NULL)
    OR (status IN ('QUEUED', 'RUNNING', 'RETRYING') AND completed_at IS NULL)
  ),
  CONSTRAINT ck_extraction_run_approval CHECK (
    (status = 'APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL)
    OR (status <> 'APPROVED' AND approved_by IS NULL AND approved_at IS NULL AND approval_note IS NULL)
  )
);

CREATE INDEX ix_extraction_run_workspace_created
  ON extraction_run(workspace_id, created_at DESC);

CREATE INDEX ix_extraction_run_source_created
  ON extraction_run(source_version_id, created_at DESC);

CREATE TABLE extraction_field_result (
  id UUID PRIMARY KEY,
  extraction_run_id UUID NOT NULL REFERENCES extraction_run(id),
  schema_field_id UUID NOT NULL REFERENCES extraction_schema_field(id),
  json_path VARCHAR(500) NOT NULL,
  value_envelope JSONB NOT NULL,
  masked_preview VARCHAR(512) NOT NULL,
  value_source VARCHAR(20) NOT NULL,
  missing_reason VARCHAR(40),
  confidence NUMERIC(5,4),
  needs_review BOOLEAN NOT NULL,
  review_status VARCHAR(20) NOT NULL,
  reviewed_value_envelope JSONB,
  review_reason VARCHAR(2000),
  reviewed_by UUID REFERENCES app_user(id),
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  revision BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_extraction_field_schema_field UNIQUE (extraction_run_id, schema_field_id),
  CONSTRAINT uq_extraction_field_json_path UNIQUE (extraction_run_id, json_path),
  CONSTRAINT ck_extraction_field_value_source
    CHECK (value_source IN ('EXTRACTED', 'DEFAULT', 'MANUAL', 'NULL_VALUE')),
  CONSTRAINT ck_extraction_field_missing_reason CHECK (
    missing_reason IS NULL OR missing_reason IN (
      'NOT_FOUND', 'INSUFFICIENT_EVIDENCE', 'AMBIGUOUS',
      'INVALID_MODEL_OUTPUT', 'SENSITIVE_TOKEN_MISSING'
    )
  ),
  CONSTRAINT ck_extraction_field_confidence
    CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
  CONSTRAINT ck_extraction_field_review_status
    CHECK (review_status IN ('PENDING', 'ACCEPTED', 'MODIFIED', 'REJECTED')),
  CONSTRAINT ck_extraction_field_review_pair CHECK (
    (reviewed_by IS NULL AND reviewed_at IS NULL)
    OR (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
  ),
  CONSTRAINT ck_extraction_field_review_value CHECK (
    (review_status = 'MODIFIED' AND reviewed_value_envelope IS NOT NULL)
    OR (review_status <> 'MODIFIED' AND reviewed_value_envelope IS NULL)
  )
);

CREATE TABLE extraction_candidate (
  id UUID PRIMARY KEY,
  field_result_id UUID NOT NULL REFERENCES extraction_field_result(id),
  candidate_position INTEGER NOT NULL,
  value_envelope JSONB NOT NULL,
  masked_preview VARCHAR(512) NOT NULL,
  confidence NUMERIC(5,4) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_extraction_candidate_position UNIQUE (field_result_id, candidate_position),
  CONSTRAINT uq_extraction_candidate_field UNIQUE (id, field_result_id),
  CONSTRAINT ck_extraction_candidate_position CHECK (candidate_position >= 0),
  CONSTRAINT ck_extraction_candidate_confidence CHECK (confidence BETWEEN 0 AND 1)
);

CREATE TABLE extraction_evidence (
  id UUID PRIMARY KEY,
  field_result_id UUID NOT NULL REFERENCES extraction_field_result(id),
  candidate_id UUID,
  source_version_id UUID NOT NULL REFERENCES source_version(id),
  evidence_position INTEGER NOT NULL,
  page_number INTEGER,
  node_id VARCHAR(255) NOT NULL,
  text_envelope JSONB NOT NULL,
  masked_preview VARCHAR(1000) NOT NULL,
  start_offset INTEGER,
  end_offset INTEGER,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_extraction_evidence_candidate_field
    FOREIGN KEY (candidate_id, field_result_id)
    REFERENCES extraction_candidate(id, field_result_id),
  CONSTRAINT ck_extraction_evidence_page CHECK (page_number IS NULL OR page_number > 0),
  CONSTRAINT ck_extraction_evidence_node_nonblank CHECK (length(trim(node_id)) > 0),
  CONSTRAINT ck_extraction_evidence_position CHECK (evidence_position >= 0),
  CONSTRAINT ck_extraction_evidence_offsets CHECK (
    (start_offset IS NULL AND end_offset IS NULL)
    OR (
      start_offset IS NOT NULL AND end_offset IS NOT NULL
      AND start_offset >= 0 AND end_offset >= start_offset
    )
  )
);

CREATE UNIQUE INDEX uq_extraction_evidence_direct_position
  ON extraction_evidence(field_result_id, evidence_position)
  WHERE candidate_id IS NULL;

CREATE UNIQUE INDEX uq_extraction_evidence_candidate_position
  ON extraction_evidence(candidate_id, evidence_position)
  WHERE candidate_id IS NOT NULL;

COMMENT ON COLUMN extraction_run.result_data_envelope IS
  'Encrypted JSON envelope; plaintext extracted values must not be stored in this column.';
COMMENT ON COLUMN extraction_field_result.value_envelope IS
  'Encrypted JSON envelope containing the persisted field value, including JSON null.';
COMMENT ON COLUMN extraction_evidence.text_envelope IS
  'Encrypted JSON envelope; only masked_preview is safe for unconditional display.';
