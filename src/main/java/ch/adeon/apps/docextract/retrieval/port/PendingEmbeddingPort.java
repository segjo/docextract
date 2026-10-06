package ch.adeon.apps.docextract.retrieval.port;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.PendingEmbedding;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port persisting document-chunk embeddings as non-retrievalfähige {@code PENDING} rows
 * with a TTL (ADR-006, C-7, T-2). Implementations must never write anything other than {@code
 * PENDING} — promotion to {@code APPROVED} is the exclusive responsibility of the validation module
 * after consent.
 */
public interface PendingEmbeddingPort {

  void stage(List<PendingEmbedding> embeddings);

  /**
   * Looks up an already-{@code APPROVED} vector for a byte-identical document (same hash, tenant,
   * embedding model and extraction source), so the caller can reuse it instead of calling the
   * embedding model again (CHANGES.md duplicate detection). Read-only; never touches {@code
   * PENDING} rows.
   */
  Optional<EmbeddingVector> findApprovedByHash(
      String tenantId, String documentHash, String embeddingModel, String extractionSource);
}
