package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import java.time.Duration;
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
class PgRetrievalResultAdapterTest {

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
  private static PgRetrievalResultAdapter adapter;

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
    adapter = new PgRetrievalResultAdapter(jdbcTemplate, Duration.ofHours(2));
  }

  @AfterEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM retrieval_result");
  }

  @Test
  void stores_and_reads_back_the_result_for_a_process() {
    // jsonb normalizes whitespace on the way in/out, so the fixture already uses Postgres's
    // canonical spacing to keep this a plain equality check.
    RetrievalResult result = new RetrievalResult("repo-1", "doc-1", 0.9, "{\"id\": \"doc-1\"}");

    adapter.store("process-1", List.of(result));

    assertThat(adapter.find("process-1")).contains(List.of(result));
  }

  @Test
  void a_process_that_was_never_stored_is_reported_as_absent() {
    assertThat(adapter.find("unknown-process")).isEmpty();
  }

  @Test
  void storing_an_empty_result_clears_any_previous_one() {
    adapter.store("process-1", List.of(new RetrievalResult("repo-1", "doc-1", 0.9, null)));

    adapter.store("process-1", List.of());

    assertThat(adapter.find("process-1")).isEmpty();
  }

  @Test
  void an_expired_result_is_reported_as_absent() {
    PgRetrievalResultAdapter expiring = new PgRetrievalResultAdapter(jdbcTemplate, Duration.ZERO);
    expiring.store("process-1", List.of(new RetrievalResult("repo-1", "doc-1", 0.9, null)));

    assertThat(expiring.find("process-1")).isEmpty();
  }
}
