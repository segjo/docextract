-- Records provenance of the chunk (docling vs. pdfbox-fast-track structuring mode) and pins the
-- single embedding model currently allowed to produce rows in this table (see docextract.retrieval
-- .embedding-model in application.yml; keep this constraint in sync when switching models).
-- Added nullable first and backfilled, since pre-existing rows have no value for these new
-- columns and a plain NOT NULL ADD COLUMN fails against a non-empty table (SQLSTATE 23502).
ALTER TABLE embedding
    ADD COLUMN extraction_source TEXT, -- 'docling' | 'pdfbox'
    ADD COLUMN embedding_model   TEXT; -- genau ein aktiver Wert

UPDATE embedding
SET extraction_source = 'pdfbox-fast-track',
    embedding_model   = 'qwen3-embedding:0.6b'
WHERE extraction_source IS NULL
   OR embedding_model IS NULL;

ALTER TABLE embedding
    ALTER COLUMN extraction_source SET NOT NULL,
    ALTER COLUMN embedding_model SET NOT NULL,
    ADD CONSTRAINT chk_model CHECK (embedding_model = 'qwen3-embedding:0.6b');
