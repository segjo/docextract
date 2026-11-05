-- Saga state of the confirm step (ARCHITECTURE § 6.2, ADR-015): persisted right after the DMS
-- write so a retry on any instance only repeats the idempotent corpus promotion, never the DMS
-- write. Ids only, no PII (C-4). FINALIZED_INDEX_PENDING protects the PENDING embedding from the
-- TTL cleanup (C-7). No expires_at: rows are only deleted on an explicit deletion request per
-- tenant (NfA-5).
CREATE TABLE process_finalization (
    process_id      VARCHAR(64)  PRIMARY KEY,
    tenant_id       VARCHAR(128) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    repository_id   VARCHAR(128) NOT NULL,
    dms_document_id VARCHAR(128),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_process_finalization_index_pending
    ON process_finalization (process_id)
    WHERE status = 'FINALIZED_INDEX_PENDING';

CREATE INDEX idx_process_finalization_tenant ON process_finalization (tenant_id);
