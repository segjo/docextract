package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import ch.adeon.apps.docextract.retrieval.application.RetrievalResultPort;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Persists the (already min-score-filtered) similar-documents result for a process in Postgres
 * rather than per-instance memory: the node serving {@code GET /processes/{id}/similar-documents}
 * need not be the one that ran {@code FindSimilar} (cluster mode, ADR-004). Only document
 * ids/scores are stored, no raw content (ADR-006). Expired rows are treated as absent and left for
 * TTL cleanup rather than deleted eagerly on read.
 */
@Component
public class PgRetrievalResultAdapter implements RetrievalResultPort {

  private final JdbcTemplate jdbcTemplate;
  private final Duration ttl;

  public PgRetrievalResultAdapter(
      JdbcTemplate jdbcTemplate, @Value("${docextract.retrieval.result-ttl:PT2H}") Duration ttl) {
    this.jdbcTemplate = jdbcTemplate;
    this.ttl = ttl;
  }

  @Override
  public void store(String processId, List<RetrievalResult> results) {
    jdbcTemplate.update("DELETE FROM retrieval_result WHERE process_id = ?", processId);
    if (results.isEmpty()) {
      return;
    }
    Timestamp expiresAt = Timestamp.from(Instant.now().plus(ttl));
    for (RetrievalResult result : results) {
      jdbcTemplate.update(
          """
          INSERT INTO retrieval_result
              (process_id, repository_id, dms_document_id, score, properties, expires_at)
          VALUES (?, ?, ?, ?, ?::jsonb, ?)
          """,
          processId,
          result.repositoryId(),
          result.documentId(),
          result.score(),
          result.properties(),
          expiresAt);
    }
  }

  @Override
  public Optional<List<RetrievalResult>> find(String processId) {
    List<RetrievalResult> results =
        jdbcTemplate.query(
            """
            SELECT repository_id, dms_document_id, score, properties
            FROM retrieval_result
            WHERE process_id = ? AND expires_at > now()
            ORDER BY id
            """,
            (rs, rowNum) ->
                new RetrievalResult(
                    rs.getString("repository_id"),
                    rs.getString("dms_document_id"),
                    rs.getDouble("score"),
                    rs.getString("properties")),
            processId);
    return results.isEmpty() ? Optional.empty() : Optional.of(results);
  }
}
