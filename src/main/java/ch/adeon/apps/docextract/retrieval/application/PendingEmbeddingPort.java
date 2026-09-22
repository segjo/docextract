package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.PendingEmbedding;
import java.util.List;

/**
 * Outbound port persisting document-chunk embeddings as non-retrievalfähige {@code PENDING} rows
 * with a TTL (ADR-006, C-7, T-2). Implementations must never write anything other than {@code
 * PENDING} — promotion to {@code APPROVED} is the exclusive responsibility of the validation module
 * after consent.
 */
public interface PendingEmbeddingPort {

  void stage(List<PendingEmbedding> embeddings);
}
