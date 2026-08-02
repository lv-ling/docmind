CREATE TABLE extraction_review_operation (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  extraction_run_id UUID NOT NULL REFERENCES extraction_run(id) ON DELETE CASCADE,
  field_result_id UUID REFERENCES extraction_field_result(id) ON DELETE CASCADE,
  operation_type VARCHAR(30) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  actor_id UUID NOT NULL REFERENCES app_user(id),
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_extraction_review_operation_key
    UNIQUE (actor_id, operation_type, idempotency_key),
  CONSTRAINT ck_extraction_review_operation_type
    CHECK (operation_type IN ('FIELD_REVIEW', 'APPROVE')),
  CONSTRAINT ck_extraction_review_operation_target CHECK (
    (operation_type = 'FIELD_REVIEW' AND field_result_id IS NOT NULL)
    OR (operation_type = 'APPROVE' AND field_result_id IS NULL)
  )
);

CREATE INDEX ix_extraction_review_operation_run
  ON extraction_review_operation(extraction_run_id, created_at DESC);
