-- Transient blobstore for raw/preview bytes (ADR-008). DOCUMENT_BLOB carries header data,
-- DOCUMENT_BLOB_PAGE holds the content as 1-MiB BYTEA segments so reads stay range-capable
-- without materializing the whole object.
CREATE TABLE document_blob (
    blob_id     UUID PRIMARY KEY,
    kind        VARCHAR(16)  NOT NULL,
    media_type  VARCHAR(255) NOT NULL,
    size_bytes  BIGINT       NOT NULL,
    process_id  VARCHAR(64)  NOT NULL,
    tenant_id   VARCHAR(128) NOT NULL,
    user_id     VARCHAR(128) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ  NOT NULL
);

-- Cleanup job predicate (TTL-Ablauf, ADR-008).
CREATE INDEX idx_document_blob_expires_at ON document_blob (expires_at);

CREATE TABLE document_blob_page (
    blob_id     UUID    NOT NULL REFERENCES document_blob (blob_id) ON DELETE CASCADE,
    segment_no  INTEGER NOT NULL,
    bytes       BYTEA   NOT NULL,
    PRIMARY KEY (blob_id, segment_no)
);

-- Bytes are already compressed (PDFs); avoid TOAST compression overhead (ARCHITECTURE.md §7).
ALTER TABLE document_blob_page ALTER COLUMN bytes SET STORAGE EXTERNAL;
