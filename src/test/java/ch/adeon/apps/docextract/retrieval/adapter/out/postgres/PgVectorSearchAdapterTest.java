package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.port.RetrievalResult;
import java.util.Arrays;
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
class PgVectorSearchAdapterTest {

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
  private static PgVectorSearchAdapter adapter;

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
    adapter = new PgVectorSearchAdapter(jdbcTemplate);
  }

  @AfterEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM embedding");
  }

  @Test
  void finds_only_approved_embeddings_for_the_caller_s_tenant() {
    insertApproved("tenant-a", "repo-1", "doc-close", vectorOf(1f, 0f));
    insertApproved("tenant-a", "repo-1", "doc-far", vectorOf(0f, 1f));
    insertApproved("tenant-b", "repo-1", "doc-other-tenant", vectorOf(1f, 0f));
    insertPending("tenant-a", vectorOf(1f, 0f));

    List<RetrievalResult> results =
        adapter.search(
            "tenant-a", new EmbeddingVector(vectorOf(1f, 0f)), 5, "qwen3-embedding:0.6b", "pdfbox");

    assertThat(results)
        .extracting(RetrievalResult::documentId)
        .containsExactly("doc-close", "doc-far");
  }

  @Test
  void respects_the_topK_limit() {
    for (int i = 0; i < 3; i++) {
      insertApproved("tenant-a", "repo-1", "doc-" + i, vectorOf(1f, 0f));
    }

    List<RetrievalResult> results =
        adapter.search(
            "tenant-a", new EmbeddingVector(vectorOf(1f, 0f)), 2, "qwen3-embedding:0.6b", "pdfbox");

    assertThat(results).hasSize(2);
  }

  @Test
  void excludes_approved_embeddings_of_a_different_extraction_source_or_embedding_model() {
    insertApproved(
        "tenant-a", "repo-1", "doc-current", vectorOf(1f, 0f), "pdfbox", "qwen3-embedding:0.6b");
    // chk_model (V7) was dropped in V18 so a second embedding_model is insertable, as required by
    // ReindexService/ADR-012's STALE re-indexing scenario; both values share the fixed vector(1024)
    // dimension (V3) — a dimension change is a separate, unresolved concern (see V18's OFFEN note).
    insertApproved(
        "tenant-a", "repo-1", "doc-stale-model", vectorOf(1f, 0f), "pdfbox", "nomic-embed-text");
    insertApproved(
        "tenant-a", "repo-1", "doc-stale-source", vectorOf(1f, 0f), "docling", "qwen3-embedding:0.6b");

    List<RetrievalResult> results =
        adapter.search(
            "tenant-a", new EmbeddingVector(vectorOf(1f, 0f)), 5, "qwen3-embedding:0.6b", "pdfbox");

    assertThat(results).extracting(RetrievalResult::documentId).containsExactly("doc-current");

    List<RetrievalResult> noMatch =
        adapter.search(
            "tenant-a", new EmbeddingVector(vectorOf(1f, 0f)), 5, "unknown-model", "pdfbox");

    assertThat(noMatch).isEmpty();
  }

  @Test
  void pre_filter_separates_two_distinct_embedding_model_values_in_the_same_corpus() {
    insertApproved(
        "tenant-a", "repo-1", "doc-current-model", vectorOf(1f, 0f), "pdfbox", "qwen3-embedding:0.6b");
    insertApproved(
        "tenant-a", "repo-1", "doc-superseded-model", vectorOf(1f, 0f), "pdfbox", "nomic-embed-text");

    List<RetrievalResult> currentModelResults =
        adapter.search(
            "tenant-a", new EmbeddingVector(vectorOf(1f, 0f)), 5, "qwen3-embedding:0.6b", "pdfbox");
    List<RetrievalResult> supersededModelResults =
        adapter.search(
            "tenant-a", new EmbeddingVector(vectorOf(1f, 0f)), 5, "nomic-embed-text", "pdfbox");

    assertThat(currentModelResults)
        .extracting(RetrievalResult::documentId)
        .containsExactly("doc-current-model");
    assertThat(supersededModelResults)
        .extracting(RetrievalResult::documentId)
        .containsExactly("doc-superseded-model");
  }

  private void insertApproved(
      String tenantId, String repositoryId, String documentId, float[] vector) {
    insertApproved(tenantId, repositoryId, documentId, vector, "pdfbox", "qwen3-embedding:0.6b");
  }

  private void insertApproved(
      String tenantId,
      String repositoryId,
      String documentId,
      float[] vector,
      String extractionSource,
      String embeddingModel) {
    jdbcTemplate.update(
        """
        INSERT INTO embedding
            (process_id, embedding, tenant_id, status, repository_id,
             dms_document_id, extraction_source, embedding_model)
        VALUES ('p', ?::vector, ?, 'APPROVED', ?, ?, ?, ?)
        """,
        PgVectorLiteral.of(vector),
        tenantId,
        repositoryId,
        documentId,
        extractionSource,
        embeddingModel);
  }

  private void insertPending(String tenantId, float[] vector) {
    jdbcTemplate.update(
        """
        INSERT INTO embedding
            (process_id, embedding, tenant_id, status, extraction_source, embedding_model)
        VALUES ('p', ?::vector, ?, 'PENDING', 'pdfbox', 'qwen3-embedding:0.6b')
        """,
        PgVectorLiteral.of(vector),
        tenantId);
  }

  private static float[] vectorOf(float x, float y) {
    float[] values = new float[1024];
    Arrays.fill(values, 0f);
    values[0] = x;
    values[1] = y;
    return values;
  }
}
