package ch.adeon.apps.docextract.validation.adapter.out.postgres;

import ch.adeon.apps.docextract.validation.domain.PendingEmbeddingCheck;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingCheckPort;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PgPendingEmbeddingCheckAdapter implements PendingEmbeddingCheckPort {

  private final JdbcTemplate jdbcTemplate;

  public PgPendingEmbeddingCheckAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<PendingEmbeddingCheck> find(String processId) {
    return jdbcTemplate
        .query(
            """
            SELECT tenant_id, expires_at < now() AS expired FROM embedding
            WHERE process_id = ? AND status = 'PENDING'
            """,
            (rs, i) ->
                new PendingEmbeddingCheck(rs.getString("tenant_id"), rs.getBoolean("expired")),
            processId)
        .stream()
        .findFirst();
  }
}
