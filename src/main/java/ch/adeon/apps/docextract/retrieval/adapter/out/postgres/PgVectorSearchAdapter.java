package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.port.RetrievalResult;
import ch.adeon.apps.docextract.retrieval.port.VectorSearchPort;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * ANN similarity search restricted to {@code APPROVED} embeddings, with {@code tenant_id},
 * {@code embedding_model} and {@code extraction_source} embedded directly in the query as a
 * mandatory pre-filter — never applied as a post-filter (ADR-002, NfA-4/T-3, SPEC §3 FR-3).
 * {@code PENDING} rows are structurally unreachable, since {@code status = 'APPROVED'} is a
 * mandatory query predicate, not an optional one.
 */
@Component
public class PgVectorSearchAdapter implements VectorSearchPort {

  private final JdbcTemplate jdbcTemplate;

  public PgVectorSearchAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<RetrievalResult> search(
      String tenantId,
      EmbeddingVector queryEmbedding,
      int topK,
      String embeddingModel,
      String extractionSource) {
    String queryVector = PgVectorLiteral.of(queryEmbedding.values());
    return jdbcTemplate.query(
        """
        SELECT repository_id, dms_document_id, 1 - (embedding <=> ?::vector) AS score
        FROM embedding
        WHERE status = 'APPROVED'
          AND tenant_id = ?
          AND embedding_model = ?
          AND extraction_source = ?
        ORDER BY embedding <=> ?::vector
        LIMIT ?
        """,
        (rs, rowNum) ->
            new RetrievalResult(
                rs.getString("repository_id"),
                rs.getString("dms_document_id"),
                rs.getDouble("score"),
                null),
        queryVector,
        tenantId,
        embeddingModel,
        extractionSource,
        queryVector,
        topK);
  }
}
