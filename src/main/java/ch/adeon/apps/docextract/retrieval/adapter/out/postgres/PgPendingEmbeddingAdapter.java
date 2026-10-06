package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.PendingEmbedding;
import ch.adeon.apps.docextract.retrieval.port.PendingEmbeddingPort;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Persists document-chunk embeddings as non-retrievalfähige {@code PENDING} rows with a TTL
 * (ADR-006, C-7, T-2). {@code repository_id}/{@code dms_document_id} stay {@code NULL} until the
 * validation module promotes a row to {@code APPROVED} after consent — this adapter never writes
 * any other status.
 */
@Component
public class PgPendingEmbeddingAdapter implements PendingEmbeddingPort {

  private final JdbcTemplate jdbcTemplate;
  private final Duration ttl;

  public PgPendingEmbeddingAdapter(
      JdbcTemplate jdbcTemplate,
      @Value("${docextract.retrieval.pending-embedding-ttl:PT2H}") Duration ttl) {
    this.jdbcTemplate = jdbcTemplate;
    this.ttl = ttl;
  }

  @Override
  public void stage(List<PendingEmbedding> embeddings) {
    if (embeddings.isEmpty()) {
      return;
    }
    Timestamp expiresAt = Timestamp.from(Instant.now().plus(ttl));
    for (PendingEmbedding embedding : embeddings) {
      jdbcTemplate.update(
          """
          INSERT INTO embedding
              (process_id, embedding, tenant_id, status, expires_at,
               extraction_source, embedding_model, document_hash)
          VALUES (?, ?::vector, ?, 'PENDING', ?, ?, ?, ?)
          """,
          embedding.processId(),
          PgVectorLiteral.of(embedding.vector().values()),
          embedding.tenantId(),
          expiresAt,
          embedding.extractionSource(),
          embedding.embeddingModel(),
          embedding.documentHash());
    }
  }

  @Override
  public Optional<EmbeddingVector> findApprovedByHash(
      String tenantId, String documentHash, String embeddingModel, String extractionSource) {
    if (documentHash == null || documentHash.isBlank()) {
      return Optional.empty();
    }
    return jdbcTemplate
        .query(
            """
            SELECT embedding::text AS embedding
            FROM embedding
            WHERE status = 'APPROVED'
              AND tenant_id = ?
              AND document_hash = ?
              AND embedding_model = ?
              AND extraction_source = ?
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getString("embedding"),
            tenantId,
            documentHash,
            embeddingModel,
            extractionSource)
        .stream()
        .findFirst()
        .map(literal -> new EmbeddingVector(PgVectorLiteral.parse(literal)));
  }
}
