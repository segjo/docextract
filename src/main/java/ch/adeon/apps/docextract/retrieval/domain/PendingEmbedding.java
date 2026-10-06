package ch.adeon.apps.docextract.retrieval.domain;

/**
 * The single embedding staged for a document under processing (ADR-012: one embedding per document,
 * not per chunk). Not yet retrievalfähig — it only becomes visible to {@code VectorSearchPort} once
 * the validation module promotes it to {@code APPROVED} after consent (ADR-006, C-7, T-2). {@code
 * repositoryId}/{@code dmsDocumentId} are unknown at this point and are attached only at promotion
 * time. {@code documentHash} is the SHA-256 of the raw uploaded bytes, nullable when unknown, used
 * to detect a byte-identical re-upload and skip re-embedding it (CHANGES.md duplicate detection).
 */
public record PendingEmbedding(
    String processId,
    EmbeddingVector vector,
    String tenantId,
    String extractionSource,
    String embeddingModel,
    String documentHash) {}
