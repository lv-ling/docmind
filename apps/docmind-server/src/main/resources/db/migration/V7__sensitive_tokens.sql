CREATE TABLE sensitive_token (
  id UUID PRIMARY KEY,
  extraction_run_id UUID NOT NULL REFERENCES extraction_run(id) ON DELETE CASCADE,
  source_version_id UUID NOT NULL REFERENCES source_version(id),
  token VARCHAR(160) NOT NULL,
  data_type VARCHAR(40) NOT NULL,
  value_envelope JSONB NOT NULL,
  masked_preview VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_sensitive_token_run_token UNIQUE (extraction_run_id, token),
  CONSTRAINT ck_sensitive_token_value_envelope_object
    CHECK (jsonb_typeof(value_envelope) = 'object'),
  CONSTRAINT ck_sensitive_token_nonblank
    CHECK (length(trim(token)) > 0 AND length(trim(masked_preview)) > 0)
);

CREATE INDEX ix_sensitive_token_run
  ON sensitive_token(extraction_run_id);

COMMENT ON TABLE sensitive_token IS
  'Task-scoped token metadata. Original values and occurrence data exist only inside value_envelope.';
COMMENT ON COLUMN sensitive_token.value_envelope IS
  'AES-GCM encrypted envelope. Plaintext sensitive values must never be stored outside this column.';
