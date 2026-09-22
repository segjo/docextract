-- Cluster-safe handover of the similar-documents result to the validation UI (ADR-004): the
-- ingest job that computes a FindSimilar result may run on a different node than the one that
-- later serves GET /processes/{id}/similar-documents, so this must not live in per-instance
-- memory. No raw content, only document ids/scores (ADR-006).
CREATE TABLE retrieval_result (
    id              BIGSERIAL       PRIMARY KEY,
    process_id      VARCHAR(64)     NOT NULL,
    repository_id   VARCHAR(128)    NOT NULL,
    dms_document_id VARCHAR(128)    NOT NULL,
    score           DOUBLE PRECISION NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_retrieval_result_process_id ON retrieval_result (process_id, id);
CREATE INDEX idx_retrieval_result_expires_at ON retrieval_result (expires_at);
