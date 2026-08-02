UPDATE async_job
SET status = 'QUEUED',
    attempt_count = 0,
    available_at = CURRENT_TIMESTAMP,
    started_at = NULL,
    completed_at = NULL,
    failure_code = NULL,
    published_at = NULL,
    publish_lease_id = NULL,
    publish_lease_expires_at = NULL,
    worker_lease_expires_at = NULL,
    updated_at = CURRENT_TIMESTAMP,
    revision = revision + 1
WHERE job_type = 'SOURCE_PREVIEW'
  AND aggregate_id IN (
    SELECT id
    FROM source_preview
    WHERE status = 'READY'
      AND page_count IS NULL
  );

UPDATE source_preview
SET status = 'QUEUED',
    failure_code = NULL,
    completed_at = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'READY'
  AND page_count IS NULL;
