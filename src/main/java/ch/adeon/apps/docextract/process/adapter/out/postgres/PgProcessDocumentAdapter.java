package ch.adeon.apps.docextract.process.adapter.out.postgres;

import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Persists the DMS target on the ORIGINAL {@code document_blob} row (shared TTL cleanup). */
@Component
public class PgProcessDocumentAdapter implements ProcessDocumentPort {

  private final JdbcTemplate jdbcTemplate;

  public PgProcessDocumentAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(String processId, String dmsLocation, String filename) {
    jdbcTemplate.update(
        """
        UPDATE document_blob SET dms_location = ?, filename = ?
        WHERE process_id = ? AND kind = 'ORIGINAL'
        """,
        dmsLocation,
        filename,
        processId);
  }

  @Override
  public Optional<StoredDocument> find(String processId) {
    return jdbcTemplate
        .query(
            """
            SELECT dms_location, filename FROM document_blob
            WHERE process_id = ? AND kind = 'ORIGINAL' AND dms_location IS NOT NULL
            """,
            (rs, i) -> new StoredDocument(rs.getString("dms_location"), rs.getString("filename")),
            processId)
        .stream()
        .findFirst();
  }
}
