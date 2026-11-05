package ch.adeon.apps.docextract.validation.adapter.out.postgres;

import ch.adeon.apps.docextract.validation.application.FinalizationStatePort;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Saga state in {@code process_finalization} (V14/V15), visible to every cluster node (ADR-004).
 */
@Component
public class PgFinalizationStateAdapter implements FinalizationStatePort {

  private static final String SELECT =
      "SELECT process_id, tenant_id, status, repository_id, dms_document_id, first_failed_at"
          + " FROM process_finalization";

  private final JdbcTemplate jdbcTemplate;

  public PgFinalizationStateAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(Finalization finalization) {
    jdbcTemplate.update(
        """
        INSERT INTO process_finalization
            (process_id, tenant_id, status, repository_id, dms_document_id, first_failed_at)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (process_id) DO UPDATE
        SET status = EXCLUDED.status,
            repository_id = EXCLUDED.repository_id,
            dms_document_id = EXCLUDED.dms_document_id,
            first_failed_at = EXCLUDED.first_failed_at,
            updated_at = now()
        """,
        finalization.processId(),
        finalization.tenantId(),
        finalization.status().name(),
        finalization.repositoryId(),
        finalization.dmsDocumentId(),
        finalization.firstFailedAt() == null ? null : Timestamp.from(finalization.firstFailedAt()));
  }

  @Override
  public Optional<Finalization> find(String processId) {
    return jdbcTemplate
        .query(SELECT + " WHERE process_id = ?", PgFinalizationStateAdapter::map, processId)
        .stream()
        .findFirst();
  }

  @Override
  public List<Finalization> findByStatus(FinalizationStatus status) {
    return jdbcTemplate.query(
        SELECT + " WHERE status = ?", PgFinalizationStateAdapter::map, status.name());
  }

  @Override
  public void discardWriteStarted(String processId) {
    jdbcTemplate.update(
        "DELETE FROM process_finalization WHERE process_id = ? AND status = 'DMS_WRITE_STARTED'",
        processId);
  }

  @Override
  public void deleteByTenant(String tenantId) {
    jdbcTemplate.update("DELETE FROM process_finalization WHERE tenant_id = ?", tenantId);
  }

  private static Finalization map(ResultSet rs, int rowNum) throws SQLException {
    Timestamp firstFailedAt = rs.getTimestamp("first_failed_at");
    return new Finalization(
        rs.getString("process_id"),
        rs.getString("tenant_id"),
        FinalizationStatus.valueOf(rs.getString("status")),
        rs.getString("repository_id"),
        rs.getString("dms_document_id"),
        firstFailedAt == null ? null : firstFailedAt.toInstant());
  }
}
