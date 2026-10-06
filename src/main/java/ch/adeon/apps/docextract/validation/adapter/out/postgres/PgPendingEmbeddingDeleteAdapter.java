package ch.adeon.apps.docextract.validation.adapter.out.postgres;

import ch.adeon.apps.docextract.validation.port.PendingEmbeddingDeletePort;
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
}
