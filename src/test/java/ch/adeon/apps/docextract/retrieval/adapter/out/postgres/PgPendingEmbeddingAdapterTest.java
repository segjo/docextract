package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.PendingEmbedding;
import java.time.Duration;
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
class PgPendingEmbeddingAdapterTest {

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
  private static PgPendingEmbeddingAdapter adapter;

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
    adapter = new PgPendingEmbeddingAdapter(jdbcTemplate, Duration.ofHours(2));
  }

  @AfterEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM embedding");
  }

  @Test
  void stages_embeddings_as_pending_with_an_expiry_and_no_dms_reference_yet() {
    EmbeddingVector vector = new EmbeddingVector(floatsOfLength(1024, 0.5f));

    adapter.stage(
        java.util.List.of(
            new PendingEmbedding(
                "process-1", vector, "tenant-a", "pdfbox", "qwen3-embedding:0.6b", "hash-1")));

    var row =
        jdbcTemplate.queryForMap(
            "SELECT status, tenant_id, repository_id, dms_document_id, expires_at, document_hash"
                + " FROM embedding WHERE process_id = 'process-1'");
    assertThat(row).containsEntry("status", "PENDING");
    assertThat(row).containsEntry("tenant_id", "tenant-a");
    assertThat(row).containsEntry("document_hash", "hash-1");
    assertThat(row.get("repository_id")).isNull();
    assertThat(row.get("dms_document_id")).isNull();
    assertThat(row.get("expires_at")).isNotNull();
  }

  @Test
  void staging_an_empty_list_is_a_no_op() {
    adapter.stage(java.util.List.of());

    Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM embedding", Integer.class);
    assertThat(count).isZero();
  }

  @Test
  void finds_an_approved_vector_matching_hash_tenant_model_and_source() {
    EmbeddingVector vector = new EmbeddingVector(floatsOfLength(1024, 0.25f));
    adapter.stage(
        java.util.List.of(
            new PendingEmbedding(
                "process-2", vector, "tenant-a", "pdfbox", "qwen3-embedding:0.6b", "hash-2")));
    jdbcTemplate.update(
        "UPDATE embedding SET status = 'APPROVED', expires_at = NULL WHERE process_id ="
            + " 'process-2'");

    var found = adapter.findApprovedByHash("tenant-a", "hash-2", "qwen3-embedding:0.6b", "pdfbox");

    assertThat(found).isPresent();
    assertThat(found.get().values()).isEqualTo(vector.values());
  }

  @Test
  void does_not_find_a_pending_or_non_matching_row_as_a_duplicate() {
    EmbeddingVector vector = new EmbeddingVector(floatsOfLength(1024, 0.25f));
    adapter.stage(
        java.util.List.of(
            new PendingEmbedding(
                "process-3", vector, "tenant-a", "pdfbox", "qwen3-embedding:0.6b", "hash-3")));

    assertThat(adapter.findApprovedByHash("tenant-a", "hash-3", "qwen3-embedding:0.6b", "pdfbox"))
        .isEmpty();
    assertThat(adapter.findApprovedByHash("tenant-a", null, "qwen3-embedding:0.6b", "pdfbox"))
        .isEmpty();
  }

  static float[] floatsOfLength(int length, float value) {
    float[] values = new float[length];
    java.util.Arrays.fill(values, value);
    return values;
  }
}
