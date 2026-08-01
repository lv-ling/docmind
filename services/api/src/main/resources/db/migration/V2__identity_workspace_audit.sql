CREATE TABLE app_user (
  id UUID PRIMARY KEY,
  email VARCHAR(254) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_app_user_email UNIQUE (email),
  CONSTRAINT ck_app_user_email_lowercase CHECK (email = lower(email)),
  CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE workspace (
  id UUID PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  slug VARCHAR(63) NOT NULL,
  created_by UUID NOT NULL REFERENCES app_user(id),
  creation_idempotency_key VARCHAR(128) NOT NULL,
  creation_request_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT uq_workspace_slug UNIQUE (slug),
  CONSTRAINT uq_workspace_creation_key UNIQUE (created_by, creation_idempotency_key),
  CONSTRAINT ck_workspace_slug CHECK (slug ~ '^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$')
);

CREATE TABLE workspace_member (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  user_id UUID NOT NULL REFERENCES app_user(id),
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT uq_workspace_member UNIQUE (workspace_id, user_id),
  CONSTRAINT ck_workspace_member_role CHECK (role IN ('OWNER', 'ADMIN', 'EDITOR', 'REVIEWER', 'VIEWER')),
  CONSTRAINT ck_workspace_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE INDEX ix_workspace_member_user ON workspace_member(user_id, status);

CREATE TABLE audit_event (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL REFERENCES workspace(id),
  actor_user_id UUID NOT NULL REFERENCES app_user(id),
  action VARCHAR(100) NOT NULL,
  target_type VARCHAR(80) NOT NULL,
  target_id UUID NOT NULL,
  outcome VARCHAR(20) NOT NULL,
  request_id UUID NOT NULL,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  occurred_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILURE'))
);

CREATE INDEX ix_audit_event_workspace_time ON audit_event(workspace_id, occurred_at DESC);
CREATE INDEX ix_audit_event_target ON audit_event(target_type, target_id);

COMMENT ON TABLE audit_event IS 'Append-only security and business audit trail; update and delete are forbidden by application policy.';

CREATE FUNCTION prevent_audit_event_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  RAISE EXCEPTION 'audit_event is append-only';
END;
$$;

CREATE TRIGGER audit_event_append_only
BEFORE UPDATE OR DELETE ON audit_event
FOR EACH ROW EXECUTE FUNCTION prevent_audit_event_mutation();
