package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import ch.adeon.apps.docextract.retrieval.application.CorpusMaintenancePort;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Postgres implementation of {@link CorpusMaintenancePort} (ADR-012). */
@Component
public class PgCorpusMaintenanceAdapter implements CorpusMaintenancePort {

  private final JdbcTemplate jdbcTemplate;

  public PgCorpusMaintenanceAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public int markStale(String embeddingModel) {
    return jdbcTemplate.update(
        "UPDATE embedding SET status = 'STALE' WHERE status = 'APPROVED' AND embedding_model = ?",
        embeddingModel);
  }

  @Override
  public List<String> approvedModels() {
    return jdbcTemplate.queryForList(
        "SELECT DISTINCT embedding_model FROM embedding WHERE status = 'APPROVED'", String.class);
  }
}
