-- Document type candidate(s) for a process (SPEC §3): either the best-matched type read back from
-- an already-fetched similar-document hit, or -- when no similar APPROVED document was found -- all
-- repository document types + properties fetched from GET /r/{repositoryId}/objdef as a fallback.
-- Both are stored in the same shape (DmsDocumentMetadata). One row per process (candidates as a
-- JSON array), not one row per document type -- unlike retrieval_result there is no per-hit
-- score/document id to key on here.
CREATE TABLE document_type_candidate (
    process_id  VARCHAR(64)  PRIMARY KEY,
    candidates  JSONB        NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_type_candidate_expires_at ON document_type_candidate (expires_at);
