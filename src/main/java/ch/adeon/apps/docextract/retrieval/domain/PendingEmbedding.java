package ch.adeon.apps.docextract.retrieval.domain;

/**
 * One chunk's embedding staged for a document under processing. Not yet retrievalfähig — it only
 * becomes visible to {@code VectorSearchPort} once the validation module promotes it to {@code
 * APPROVED} after consent (ADR-006, C-7, T-2). {@code repositoryId}/{@code dmsDocumentId} are
 * unknown at this point and are attached only at promotion time.
 */
public record PendingEmbedding(
    String processId,
    int chunkIndex,
    EmbeddingVector vector,
    String tenantId,
    String aclRef,
    String extractionSource,
    String embeddingModel) {}
