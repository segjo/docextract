-- ADR-012/SPEC: one embedding per document (no chunking) and tenant_id-only pre-filter (no ACL).
-- Drops the now-unused chunk_index/acl_ref columns and rebuilds the APPROVED partial index without
-- acl_ref. Existing PENDING/APPROVED rows are not migrated to a single-row-per-document shape
-- retroactively — new ingests already only ever produce one row per process (StagePendingEmbeddings
-- Service), so this is a forward-looking constraint, not a backfill.
DROP INDEX IF EXISTS idx_embedding_approved_tenant_acl;

ALTER TABLE embedding
    DROP COLUMN chunk_index,
    DROP COLUMN acl_ref;

CREATE INDEX idx_embedding_approved_tenant
    ON embedding (tenant_id, embedding_model)
    WHERE status = 'APPROVED';
