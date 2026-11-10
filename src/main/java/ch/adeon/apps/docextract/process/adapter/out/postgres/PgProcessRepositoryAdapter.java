package ch.adeon.apps.docextract.process.adapter.out.postgres;

import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Stored on the ORIGINAL {@code document_blob} row like dms_location (V12), shared TTL cleanup. */
@Component
public class PgProcessRepositoryAdapter implements ProcessRepositoryPort {

  private final JdbcTemplate jdbcTemplate;

  public PgProcessRepositoryAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void stage(String tenantId, String processId, String repositoryId) {
    jdbcTemplate.update(
        """
        UPDATE document_blob SET repository_id = ?
        WHERE process_id = ? AND tenant_id = ? AND kind = 'ORIGINAL'
        """,
        repositoryId,
        processId,
        tenantId);
  }

  @Override
  public Optional<String> retrieve(String tenantId, String processId) {
    return jdbcTemplate
        .queryForList(
            """
            SELECT repository_id FROM document_blob
            WHERE process_id = ? AND tenant_id = ? AND kind = 'ORIGINAL'
              AND repository_id IS NOT NULL
            """,
            String.class,
            processId,
            tenantId)
        .stream()
        .findFirst();
  }
}
