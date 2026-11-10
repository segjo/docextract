package ch.adeon.apps.docextract.process.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
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
class PgProcessRepositoryAdapterTest {

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

  @BeforeAll
  static void migrate() {
    DataSource dataSource =
        DataSourceBuilder.create()
            .url(POSTGRES.getJdbcUrl())
            .username(POSTGRES.getUsername())
            .password(POSTGRES.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();
    new JdbcTemplate(dataSource).execute("CREATE EXTENSION IF NOT EXISTS vector");
    Flyway.configure().dataSource(dataSource).load().migrate();
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @AfterEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM document_blob");
  }

  @Test
  void staged_repository_is_visible_to_another_adapter_instance() {
    storeOriginal("p-1");
    new PgProcessRepositoryAdapter(jdbcTemplate).stage("t", "p-1", "repo-a");

    assertThat(new PgProcessRepositoryAdapter(jdbcTemplate).retrieve("t", "p-1"))
        .contains("repo-a");
  }

  @Test
  void another_tenant_neither_reads_nor_overwrites_the_repository() {
    storeOriginal("p-1");
    PgProcessRepositoryAdapter adapter = new PgProcessRepositoryAdapter(jdbcTemplate);
    adapter.stage("t", "p-1", "repo-a");

    adapter.stage("other", "p-1", "repo-x");

    assertThat(adapter.retrieve("other", "p-1")).isEmpty();
    assertThat(adapter.retrieve("t", "p-1")).contains("repo-a");
  }

  @Test
  void restaging_overwrites_the_repository() {
    storeOriginal("p-1");
    PgProcessRepositoryAdapter adapter = new PgProcessRepositoryAdapter(jdbcTemplate);
    adapter.stage("t", "p-1", "repo-a");
    adapter.stage("t", "p-1", "repo-b");

    assertThat(adapter.retrieve("t", "p-1")).contains("repo-b");
  }

  @Test
  void unknown_process_or_deleted_blob_yields_empty() {
    storeOriginal("p-1");
    PgProcessRepositoryAdapter adapter = new PgProcessRepositoryAdapter(jdbcTemplate);
    adapter.stage("t", "p-1", "repo-a");
    jdbcTemplate.update("DELETE FROM document_blob WHERE process_id = 'p-1'");

    assertThat(adapter.retrieve("t", "p-1")).isEmpty();
    assertThat(adapter.retrieve("t", "unknown")).isEmpty();
  }

  private static void storeOriginal(String processId) {
    jdbcTemplate.update(
        """
        INSERT INTO document_blob
          (blob_id, kind, media_type, size_bytes, process_id, tenant_id, user_id, expires_at)
        VALUES (?, 'ORIGINAL', 'application/pdf', 1, ?, 't', 'u', now() + interval '1 hour')
        """,
        UUID.randomUUID(),
        processId);
  }
}
