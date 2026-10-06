-- CHANGES.md duplicate detection: SHA-256 of the raw uploaded bytes, so a byte-identical
-- re-upload can reuse an already-APPROVED embedding instead of calling the embedding model again.
ALTER TABLE embedding
    ADD COLUMN document_hash VARCHAR(64);

CREATE INDEX idx_embedding_approved_hash
    ON embedding (tenant_id, document_hash, embedding_model, extraction_source)
    WHERE status = 'APPROVED';
