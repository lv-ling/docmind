ALTER TABLE async_job
  DROP CONSTRAINT ck_async_job_type;

ALTER TABLE async_job
  ADD CONSTRAINT ck_async_job_type CHECK (
    job_type IN ('SOURCE_PREVIEW', 'EXTRACTION', 'TEMPLATE_CONVERSION', 'PROOFREADING', 'DIFF')
  );

INSERT INTO async_job (
  id,
  workspace_id,
  job_type,
  aggregate_type,
  aggregate_id,
  status,
  attempt_count,
  max_attempts,
  available_at,
  started_at,
  completed_at,
  failure_code,
  revision,
  creation_request_id,
  created_by,
  created_at,
  updated_at,
  published_at,
  publish_attempt_count,
  last_publish_attempt_at,
  publish_lease_id,
  publish_lease_expires_at,
  worker_lease_expires_at
)
SELECT
  gen_random_uuid(),
  version.workspace_id,
  'SOURCE_PREVIEW',
  'source_preview',
  preview.id,
  'QUEUED',
  0,
  3,
  CURRENT_TIMESTAMP,
  NULL,
  NULL,
  NULL,
  0,
  gen_random_uuid(),
  version.created_by,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  NULL,
  0,
  NULL,
  NULL,
  NULL,
  NULL
FROM source_preview preview
JOIN source_version version ON version.id = preview.source_version_id
WHERE preview.status = 'QUEUED'
  AND NOT EXISTS (
    SELECT 1
    FROM async_job job
    WHERE job.aggregate_type = 'source_preview'
      AND job.aggregate_id = preview.id
  );
