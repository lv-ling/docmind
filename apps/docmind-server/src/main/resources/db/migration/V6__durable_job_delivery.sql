ALTER TABLE async_job
  ADD COLUMN published_at TIMESTAMPTZ,
  ADD COLUMN publish_attempt_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN last_publish_attempt_at TIMESTAMPTZ,
  ADD COLUMN publish_lease_id UUID,
  ADD COLUMN publish_lease_expires_at TIMESTAMPTZ,
  ADD COLUMN worker_lease_expires_at TIMESTAMPTZ;

UPDATE async_job
SET worker_lease_expires_at = CURRENT_TIMESTAMP + INTERVAL '2 minutes'
WHERE status = 'RUNNING';

ALTER TABLE async_job
  ADD CONSTRAINT ck_async_job_publish_attempts
    CHECK (publish_attempt_count >= 0),
  ADD CONSTRAINT ck_async_job_publish_lease_pair CHECK (
    (publish_lease_id IS NULL AND publish_lease_expires_at IS NULL)
    OR (publish_lease_id IS NOT NULL AND publish_lease_expires_at IS NOT NULL)
  ),
  ADD CONSTRAINT ck_async_job_worker_lease CHECK (
    (status = 'RUNNING' AND worker_lease_expires_at IS NOT NULL)
    OR (status <> 'RUNNING' AND worker_lease_expires_at IS NULL)
  );

CREATE INDEX ix_async_job_unpublished_due
  ON async_job(status, available_at, publish_lease_expires_at, created_at)
  WHERE status IN ('QUEUED', 'RETRYING') AND published_at IS NULL;

CREATE INDEX ix_async_job_expired_worker_lease
  ON async_job(worker_lease_expires_at)
  WHERE status = 'RUNNING';

COMMENT ON COLUMN async_job.publish_lease_id IS
  'Short-lived database dispatcher lease token; it prevents concurrent publishers from owning the same attempt.';
COMMENT ON COLUMN async_job.published_at IS
  'Broker-confirmed publication time for the current attempt; cleared before a retry is dispatched.';
