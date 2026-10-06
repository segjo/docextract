-- Extraction result for a process (FR-4): the classified document type id plus the LLM's
-- attribute-value suggestions (one JSON array column, mirroring document_type_candidate's shape --
-- there's no per-attribute score/document id to key rows on separately).
CREATE TABLE extraction_result (
    process_id       VARCHAR(64)  PRIMARY KEY,
    document_type_id VARCHAR(255),
    attributes       JSONB        NOT NULL,
    expires_at       TIMESTAMPTZ  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_extraction_result_expires_at ON extraction_result (expires_at);
