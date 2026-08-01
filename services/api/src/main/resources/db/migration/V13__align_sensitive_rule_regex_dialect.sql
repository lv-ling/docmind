ALTER TABLE sensitive_rule
  DROP CONSTRAINT ck_sensitive_rule_regex_dialect;

ALTER TABLE sensitive_rule
  ADD CONSTRAINT ck_sensitive_rule_regex_dialect
  CHECK (regex_dialect IS NULL OR regex_dialect = 're2');
