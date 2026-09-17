package ch.adeon.apps.docextract.ingest.domain;

import java.util.UUID;

/**
 * Reference to bytes held in the transient Postgres blobstore ({@code DOCUMENT_BLOB} / {@code
 * DOCUMENT_BLOB_PAGE}), the cluster-visible handover point for the async job (ADR-008). {@code
 * tenantId}/{@code userId} pin ownership so only the uploading tenant and user may read it back
 * (NfA-4).
 */
public record BlobRef(
    UUID blobId,
    BlobKind kind,
    MediaType mediaType,
    long sizeBytes,
    String processId,
    String tenantId,
    String userId) {}
