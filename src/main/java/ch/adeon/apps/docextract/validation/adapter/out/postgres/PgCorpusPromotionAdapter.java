package ch.adeon.apps.docextract.validation.adapter.out.postgres;

import ch.adeon.apps.docextract.validation.port.CorpusPromotionPort;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Atomically promotes a process's {@code PENDING} embedding to {@code APPROVED} (ADR-002/-006,
 * C-7): sets the DMS identifiers only known after finalization and clears {@code expires_at} so the
 * TTL cleanup no longer targets this row.
 */
@Component
public class PgCorpusPromotionAdapter implements CorpusPromotionPort {

  private final JdbcTemplate jdbcTemplate;

  public PgCorpusPromotionAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void promote(String processId, String repositoryId, String dmsDocumentId) {
    jdbcTemplate.update(
        """
        UPDATE embedding
        SET status = 'APPROVED',
            repository_id = ?,
            dms_document_id = ?,
            approved_at = ?,
            expires_at = NULL
        WHERE process_id = ? AND status = 'PENDING'
        """,
        repositoryId,
        dmsDocumentId,
        Timestamp.from(Instant.now()),
        processId);
  }
}
