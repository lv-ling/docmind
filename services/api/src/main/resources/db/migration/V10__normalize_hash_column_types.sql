-- Hibernate maps fixed-length Java hash strings as VARCHAR. Normalize the few
-- early fixed CHAR columns so schema validation behaves consistently on PostgreSQL.
ALTER TABLE extraction_review_operation
  ALTER COLUMN request_hash TYPE VARCHAR(64);

ALTER TABLE document_template
  ALTER COLUMN creation_request_hash TYPE VARCHAR(64);

ALTER TABLE document_template_resource
  ALTER COLUMN sha256 TYPE VARCHAR(64);

ALTER TABLE document_template_operation
  ALTER COLUMN request_hash TYPE VARCHAR(64);
