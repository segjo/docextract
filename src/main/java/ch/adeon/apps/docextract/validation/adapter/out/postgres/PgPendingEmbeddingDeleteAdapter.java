package ch.adeon.apps.docextract.validation.adapter.out.postgres;

import ch.adeon.apps.docextract.validation.port.PendingEmbeddingDeletePort;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Deletes a process's {@code PENDING} embedding on rejection/final failure (ADR-006, C-7, T-2). */
@Component
public class PgPendingEmbeddingDeleteAdapter implements PendingEmbeddingDeletePort {

  private final JdbcTemplate jdbcTemplate;

  public PgPendingEmbeddingDeleteAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void delete(String processId) {
    jdbcTemplate.update(
        "DELETE FROM embedding WHERE process_id = ? AND status = 'PENDING'", processId);
  }

  @Override
  public List<String> findExpiredProcessIds() {
    return jdbcTemplate.queryForList(
        """
        SELECT DISTINCT process_id FROM embedding
        WHERE status = 'PENDING' AND expires_at < now()
        """,
        String.class);
  }

  @Override
  public void deleteExpired(String processId) {
    // Re-checked in SQL: a confirm may have reached FINALIZED_INDEX_PENDING since the lookup.
    jdbcTemplate.update(
        """
        DELETE FROM embedding e
        WHERE e.process_id = ? AND e.status = 'PENDING' AND e.expires_at < now()
          AND NOT EXISTS (
            SELECT 1 FROM process_finalization f
            WHERE f.process_id = e.process_id AND f.status = 'FINALIZED_INDEX_PENDING')
        """,
        processId);
  }
}
