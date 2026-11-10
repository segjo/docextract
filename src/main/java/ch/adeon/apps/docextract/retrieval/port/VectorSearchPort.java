package ch.adeon.apps.docextract.retrieval.port;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import java.util.List;

/**
 * ANN similarity search restricted to {@code APPROVED} embeddings, with the {@code tenant_id},
 * {@code embedding_model} and {@code extraction_source} predicates embedded directly in the query
 * as a mandatory pre-filter — never applied as a post-filter (ADR-002, NfA-4/T-3, SPEC §3 FR-3).
 */
public interface VectorSearchPort {

  List<RetrievalResult> search(
      String tenantId,
      EmbeddingVector queryEmbedding,
      int topK,
      String embeddingModel,
      String extractionSource);
}
