package ch.adeon.apps.docextract.retrieval.port;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import java.util.List;

/**
 * ANN similarity search restricted to {@code APPROVED} embeddings, with the {@code tenant_id}
 * predicate embedded directly in the query as a pre-filter — never applied as a post-filter
 * (ADR-002, NfA-4/T-3).
 */
public interface VectorSearchPort {

  List<RetrievalResult> search(String tenantId, EmbeddingVector queryEmbedding, int topK);
}
