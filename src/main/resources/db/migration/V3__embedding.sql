-- Retrieval-Korpus (ADR-006/-002, FR-3): embeddings start out as PENDING with a TTL and are not
-- retrievalfähig; only the validation module's consent-gated promotion (a later block) may set
-- status = APPROVED, attach repository_id/dms_document_id and clear expires_at. This adapter set
-- only ever inserts PENDING rows.
--
-- Vector dimension 1024 matches the default output of the Qwen3-Embedding-0.6B model referenced
-- in ARCHITECTURE.md §8.5 / docling-openapi.json tokenizer options. Switching embedding models
-- requires a follow-up migration with the new dimension.
CREATE TABLE embedding (
    id              BIGSERIAL    PRIMARY KEY,
    process_id      VARCHAR(64)  NOT NULL,
    chunk_index     INT          NOT NULL,
    embedding       vector(1024) NOT NULL,
    tenant_id       VARCHAR(128) NOT NULL,
    acl_ref         VARCHAR(256) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    repository_id   VARCHAR(128),
    dms_document_id VARCHAR(128),
    provenance      VARCHAR(64),
    expires_at      TIMESTAMPTZ,
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Retrieval only ever queries status = APPROVED with a tenant/ACL pre-filter as part of the
-- index-backed predicate (ADR-002, T-3/NfA-4) — never as a post-filter.
CREATE INDEX idx_embedding_approved_tenant_acl
    ON embedding (tenant_id, acl_ref)
    WHERE status = 'APPROVED';

-- TTL-Cleanup target for orphaned PENDING entries (C-7); the FINALIZED_INDEX_PENDING recovery
-- state (ADR-006) is out of scope for this migration and handled by the validation module.
CREATE INDEX idx_embedding_pending_expiry
    ON embedding (expires_at)
    WHERE status = 'PENDING';
