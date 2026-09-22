package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
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
  void finds_only_approved_embeddings_for_the_caller_s_tenant_and_acl() {
    insertApproved("tenant-a", "acl-a", "repo-1", "doc-close", vectorOf(1f, 0f));
    insertApproved("tenant-a", "acl-a", "repo-1", "doc-far", vectorOf(0f, 1f));
    insertApproved("tenant-b", "acl-a", "repo-1", "doc-other-tenant", vectorOf(1f, 0f));
    insertApproved("tenant-a", "acl-b", "repo-1", "doc-other-acl", vectorOf(1f, 0f));
    insertPending("tenant-a", "acl-a", vectorOf(1f, 0f));

    List<RetrievalResult> results =
        adapter.search("tenant-a", "acl-a", new EmbeddingVector(vectorOf(1f, 0f)), 5);

    assertThat(results)
        .extracting(RetrievalResult::documentId)
        .containsExactly("doc-close", "doc-far");
  }

  @Test
  void respects_the_topK_limit() {
    for (int i = 0; i < 3; i++) {
      insertApproved("tenant-a", "acl-a", "repo-1", "doc-" + i, vectorOf(1f, 0f));
    }

    List<RetrievalResult> results =
        adapter.search("tenant-a", "acl-a", new EmbeddingVector(vectorOf(1f, 0f)), 2);

    assertThat(results).hasSize(2);
  }

  private void insertApproved(
      String tenantId, String aclRef, String repositoryId, String documentId, float[] vector) {
    jdbcTemplate.update(
        """
        INSERT INTO embedding
            (process_id, chunk_index, embedding, tenant_id, acl_ref, status, repository_id,
             dms_document_id, extraction_source, embedding_model)
        VALUES ('p', 0, ?::vector, ?, ?, 'APPROVED', ?, ?, 'pdfbox-fast-track', 'qwen3-embedding:0.6b')
        """,
        PgVectorLiteral.of(vector),
        tenantId,
        aclRef,
        repositoryId,
        documentId);
  }

  private void insertPending(String tenantId, String aclRef, float[] vector) {
    jdbcTemplate.update(
        """
        INSERT INTO embedding
            (process_id, chunk_index, embedding, tenant_id, acl_ref, status, extraction_source,
             embedding_model)
        VALUES ('p', 0, ?::vector, ?, ?, 'PENDING', 'pdfbox-fast-track', 'qwen3-embedding:0.6b')
        """,
        PgVectorLiteral.of(vector),
        tenantId,
        aclRef);
  }

  private static float[] vectorOf(float x, float y) {
    float[] values = new float[1024];
    Arrays.fill(values, 0f);
    values[0] = x;
    values[1] = y;
    return values;
  }
}
