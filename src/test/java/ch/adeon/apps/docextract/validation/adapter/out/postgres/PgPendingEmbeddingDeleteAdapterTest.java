package ch.adeon.apps.docextract.validation.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import ch.adeon.apps.docextract.validation.domain.PendingEmbeddingCheck;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class PgPendingEmbeddingDeleteAdapterTest {

  @Container
  @SuppressWarnings("resource") // lifecycle managed by the @Testcontainers JUnit extension
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(
              DockerImageName.parse("pgvector/pgvector:pg17-bookworm")
                  .asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("docextract")
          .withUsername("docextract")
          .withPassword("docextract-test");

  private static JdbcTemplate jdbcTemplate;
  private static PgPendingEmbeddingDeleteAdapter adapter;
  private static PgFinalizationStateAdapter finalizationState;
  private static PgPendingEmbeddingCheckAdapter pendingCheck;

  @BeforeAll
  static void migrateAndConnect() {
    DataSource dataSource =
        DataSourceBuilder.create()
            .url(POSTGRES.getJdbcUrl())
            .username(POSTGRES.getUsername())
            .password(POSTGRES.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
    Flyway.configure().dataSource(dataSource).load().migrate();
    adapter = new PgPendingEmbeddingDeleteAdapter(jdbcTemplate);
    finalizationState = new PgFinalizationStateAdapter(jdbcTemplate);
    pendingCheck = new PgPendingEmbeddingCheckAdapter(jdbcTemplate);
  }

  @AfterEach
  void clearTables() {
    jdbcTemplate.update("DELETE FROM embedding");
    jdbcTemplate.update("DELETE FROM process_finalization");
  }

  @Test
  void finds_only_expired_pending_process_ids() {
    insertPending("p-expired", "now() - interval '1 minute'");
    insertPending("p-valid", "now() + interval '1 hour'");

    assertThat(adapter.findExpiredProcessIds()).containsExactly("p-expired");
  }

  // (c) SQL-Guard: auch bei Race nach dem Lookup bleibt FINALIZED_INDEX_PENDING unangetastet.
  @Test
  void delete_expired_never_removes_embedding_of_finalized_index_pending_process() {
    insertPending("p-pending", "now() - interval '1 minute'");
    insertPending("p-orphan", "now() - interval '1 minute'");
    finalizationState.save(
        new Finalization(
            "p-pending",
            "tenant-a",
            FinalizationStatus.FINALIZED_INDEX_PENDING,
            "repo-1",
            "doc-1"));

    adapter.deleteExpired("p-pending");
    adapter.deleteExpired("p-orphan");

    assertThat(processIds()).containsExactly("p-pending");
  }

  @Test
  void finalization_state_round_trips_and_updates_status() {
    Finalization pending =
        new Finalization(
            "p1", "tenant-a", FinalizationStatus.FINALIZED_INDEX_PENDING, "repo-1", "doc-1");
    finalizationState.save(pending);
    finalizationState.save(pending.withStatus(FinalizationStatus.COMPLETED));

    assertThat(finalizationState.find("p1"))
        .contains(pending.withStatus(FinalizationStatus.COMPLETED));
    assertThat(finalizationState.findByStatus(FinalizationStatus.FINALIZED_INDEX_PENDING))
        .isEmpty();
  }

  @Test
  void discard_removes_only_a_write_started_marker() {
    Finalization marker =
        new Finalization("p1", "tenant-a", FinalizationStatus.DMS_WRITE_STARTED, "repo-1", null);
    finalizationState.save(marker);
    finalizationState.save(
        new Finalization(
            "p2", "tenant-a", FinalizationStatus.FINALIZED_INDEX_PENDING, "repo-1", "doc-2"));

    finalizationState.discardWriteStarted("p1");
    finalizationState.discardWriteStarted("p2");

    assertThat(finalizationState.find("p1")).isEmpty();
    assertThat(finalizationState.find("p2")).isPresent();
  }

  // Keine TTL: nur Löschantrag je tenant_id entfernt Saga-Zustände.
  @Test
  void finalization_state_is_deleted_only_for_the_requested_tenant() {
    finalizationState.save(
        new Finalization("p-a", "tenant-a", FinalizationStatus.COMPLETED, "repo-1", "doc-a"));
    finalizationState.save(
        new Finalization("p-b", "tenant-b", FinalizationStatus.COMPLETED, "repo-1", "doc-b"));

    finalizationState.deleteByTenant("tenant-a");

    assertThat(finalizationState.find("p-a")).isEmpty();
    assertThat(finalizationState.find("p-b")).isPresent();
  }

  @Test
  void pending_check_reports_owner_tenant_and_expiry() {
    insertPending("p-expired", "now() - interval '1 minute'");
    insertPending("p-valid", "now() + interval '1 hour'");

    assertThat(pendingCheck.find("p-expired"))
        .contains(new PendingEmbeddingCheck("tenant-a", true));
    assertThat(pendingCheck.find("p-valid")).contains(new PendingEmbeddingCheck("tenant-a", false));
    assertThat(pendingCheck.find("p-unknown")).isEmpty();
  }

  private void insertPending(String processId, String expiresAtSql) {
    jdbcTemplate.update(
        """
        INSERT INTO embedding
            (process_id, embedding, tenant_id, status, extraction_source, embedding_model,
             expires_at)
        VALUES (?, ?::vector, 'tenant-a', 'PENDING', 'pdfbox', 'qwen3-embedding:0.6b', %s)
        """
            .formatted(expiresAtSql),
        processId,
        "[" + String.join(",", Collections.nCopies(1024, "0")) + "]");
  }

  private List<String> processIds() {
    return jdbcTemplate.queryForList(
        "SELECT process_id FROM embedding ORDER BY process_id", String.class);
  }
}
