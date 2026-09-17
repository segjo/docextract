-- Durable, PII-free progress log for SSE catch-up/recovery (ADR-004, C-4). Cluster-wide fan-out
-- to live subscribers happens via LISTEN/NOTIFY on the process_step_events channel; this table is
-- the source of truth for replaying already-published steps to newly connecting clients.
CREATE TABLE process_step (
    id          BIGSERIAL    PRIMARY KEY,
    process_id  VARCHAR(64)  NOT NULL,
    step        VARCHAR(32)  NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    blob_id     VARCHAR(64),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_process_step_process_id ON process_step (process_id, id);
