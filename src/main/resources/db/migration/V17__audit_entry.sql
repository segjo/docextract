-- C-4/NfA-7, ARCHITECTURE § 8.6: append-only audit trail without raw PII.
-- tenant_id is NULL only for the TTL cleanup; actor_kind and actor_hash are NULL for system events;
-- provider and model_id are set for LLM calls.
CREATE TABLE audit_entry (
    id          BIGSERIAL    PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    tenant_id   VARCHAR(128),
    channel     VARCHAR(8)   NOT NULL CHECK (channel IN ('MCP', 'UI', 'SYSTEM')),
    actor_kind  VARCHAR(8)   CHECK (actor_kind IN ('HUMAN', 'AGENT')),
    actor_hash  VARCHAR(64),
    event_type  VARCHAR(128) NOT NULL,
    args_hash   VARCHAR(64),
    token_count INTEGER      NOT NULL DEFAULT 0,
    provider    VARCHAR(64),
    model_id    VARCHAR(128)
);

CREATE INDEX idx_audit_entry_created_at ON audit_entry (created_at);

CREATE FUNCTION audit_entry_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_entry is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_entry_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_entry
    FOR EACH ROW EXECUTE FUNCTION audit_entry_append_only();
