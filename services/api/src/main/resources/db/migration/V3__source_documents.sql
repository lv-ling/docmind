CREATE TABLE source_document (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  name VARCHAR(200) NOT NULL,
  current_version_id UUID,
  created_by UUID NOT NULL REFERENCES app_user(id),
  updated_by UUID NOT NULL REFERENCES app_user(id),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT ck_source_document_name_nonblank CHECK (length(trim(name)) > 0)
);

CREATE INDEX ix_source_document_workspace_created
  ON source_document(workspace_id, created_at DESC)
  WHERE deleted_at IS NULL;

CREATE TABLE source_version (
  id UUID PRIMARY KEY,
  source_document_id UUID NOT NULL REFERENCES source_document(id),
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  version_number INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  file_type VARCHAR(10) NOT NULL,
  declared_mime_type VARCHAR(255) NOT NULL,
  expected_size_bytes BIGINT NOT NULL,
  mime_type VARCHAR(255),
  size_bytes BIGINT,
  sha256 VARCHAR(64),
  upload_bucket VARCHAR(63) NOT NULL,
  upload_key VARCHAR(1024) NOT NULL,
  object_bucket VARCHAR(63) NOT NULL,
  object_key VARCHAR(1024) NOT NULL,
  object_etag VARCHAR(255),
  failure_code VARCHAR(100),
  created_by UUID NOT NULL REFERENCES app_user(id),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_source_version_number UNIQUE (source_document_id, version_number),
  CONSTRAINT uq_source_version_upload_object UNIQUE (upload_bucket, upload_key),
  CONSTRAINT uq_source_version_object UNIQUE (object_bucket, object_key),
  CONSTRAINT ck_source_version_status CHECK (status IN ('UPLOADING', 'UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
  CONSTRAINT ck_source_version_file_type CHECK (file_type IN ('DOC', 'DOCX', 'PDF')),
  CONSTRAINT ck_source_version_expected_size CHECK (expected_size_bytes BETWEEN 1 AND 10485760),
  CONSTRAINT ck_source_version_size CHECK (size_bytes IS NULL OR size_bytes BETWEEN 1 AND 10485760),
  CONSTRAINT ck_source_version_sha256 CHECK (sha256 IS NULL OR sha256 ~ '^[a-f0-9]{64}$')
);

CREATE INDEX ix_source_version_workspace_sha256
  ON source_version(workspace_id, sha256)
  WHERE sha256 IS NOT NULL;

ALTER TABLE source_document
  ADD CONSTRAINT fk_source_document_current_version
  FOREIGN KEY (current_version_id) REFERENCES source_version(id);

CREATE TABLE source_upload_session (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  source_document_id UUID NOT NULL REFERENCES source_document(id),
  source_version_id UUID NOT NULL REFERENCES source_version(id),
  status VARCHAR(20) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  created_by UUID NOT NULL REFERENCES app_user(id),
  creation_idempotency_key VARCHAR(128) NOT NULL,
  creation_request_hash VARCHAR(64) NOT NULL,
  completion_idempotency_key VARCHAR(128),
  completion_request_hash VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ,
  staging_cleaned_at TIMESTAMPTZ,
  CONSTRAINT uq_source_upload_version UNIQUE (source_version_id),
  CONSTRAINT uq_source_upload_creation_key UNIQUE (workspace_id, created_by, creation_idempotency_key),
  CONSTRAINT ck_source_upload_status CHECK (status IN ('PENDING', 'UPLOADING', 'COMPLETED', 'EXPIRED', 'ABORTED')),
  CONSTRAINT ck_source_upload_completion_pair CHECK (
    (completion_idempotency_key IS NULL AND completion_request_hash IS NULL)
    OR (completion_idempotency_key IS NOT NULL AND completion_request_hash IS NOT NULL)
  )
);

CREATE INDEX ix_source_upload_expiry
  ON source_upload_session(status, expires_at)
  WHERE status IN ('PENDING', 'UPLOADING');

CREATE TABLE source_preview (
  id UUID PRIMARY KEY,
  source_version_id UUID NOT NULL REFERENCES source_version(id),
  status VARCHAR(20) NOT NULL,
  format VARCHAR(20) NOT NULL,
  object_bucket VARCHAR(63),
  object_key VARCHAR(1024),
  page_count INTEGER,
  failure_code VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ,
  CONSTRAINT uq_source_preview_version UNIQUE (source_version_id),
  CONSTRAINT ck_source_preview_status CHECK (status IN ('QUEUED', 'PROCESSING', 'READY', 'FAILED')),
  CONSTRAINT ck_source_preview_format CHECK (format IN ('PDF', 'PAGE_IMAGES')),
  CONSTRAINT ck_source_preview_page_count CHECK (page_count IS NULL OR page_count > 0),
  CONSTRAINT ck_source_preview_object_pair CHECK (
    (object_bucket IS NULL AND object_key IS NULL)
    OR (object_bucket IS NOT NULL AND object_key IS NOT NULL)
  )
);
