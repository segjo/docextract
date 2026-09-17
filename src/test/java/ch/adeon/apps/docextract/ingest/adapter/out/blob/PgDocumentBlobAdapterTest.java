package ch.adeon.apps.docextract.ingest.adapter.out.blob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.adeon.apps.docextract.ingest.application.BlobNotFoundException;
import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import java.time.Duration;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PgDocumentBlobAdapterTest {

  @Container
  @SuppressWarnings("resource") // lifecycle managed by the @Testcontainers JUnit extension
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("docextract")
          .withUsername("docextract")
          .withPassword("docextract-test");

  private static JdbcTemplate jdbcTemplate;
  private static PgDocumentBlobAdapter adapter;

  @BeforeAll
  static void migrateAndConnect() {
    DataSource dataSource =
        DataSourceBuilder.create()
            .url(POSTGRES.getJdbcUrl())
            .username(POSTGRES.getUsername())
            .password(POSTGRES.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();
    Flyway.configure().dataSource(dataSource).load().migrate();
    jdbcTemplate = new JdbcTemplate(dataSource);
    adapter = new PgDocumentBlobAdapter(jdbcTemplate, Duration.ofHours(2));
  }

  @AfterEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM document_blob");
  }

  @Test
  void stores_and_describes_a_blob_tagged_with_tenant_and_user() {
    BlobRef stored =
        adapter.store(
            BlobKind.ORIGINAL, MediaType.PDF, new byte[] {1, 2, 3}, "process-1", "tenant-a",
            "user-1");

    BlobRef described = adapter.describe(stored.blobId());

    assertThat(described.kind()).isEqualTo(BlobKind.ORIGINAL);
    assertThat(described.mediaType()).isEqualTo(MediaType.PDF);
    assertThat(described.sizeBytes()).isEqualTo(3);
    assertThat(described.processId()).isEqualTo("process-1");
    assertThat(described.tenantId()).isEqualTo("tenant-a");
    assertThat(described.userId()).isEqualTo("user-1");
  }

  @Test
  void round_trips_content_spanning_multiple_1_mib_pages() {
    byte[] content = new byte[PgDocumentBlobAdapter.PAGE_SIZE_BYTES + 1024];
    for (int i = 0; i < content.length; i++) {
      content[i] = (byte) (i % 251);
    }

    BlobRef stored =
        adapter.store(BlobKind.ORIGINAL, MediaType.PDF, content, "p", "tenant-a", "user-1");
    byte[] readBack = adapter.readRange(stored.blobId(), PageRange.full(content.length));

    assertThat(readBack).isEqualTo(content);
  }

  @Test
  void reads_a_partial_range_across_a_page_boundary() {
    byte[] content = new byte[PgDocumentBlobAdapter.PAGE_SIZE_BYTES + 100];
    for (int i = 0; i < content.length; i++) {
      content[i] = (byte) (i % 256);
    }
    BlobRef stored =
        adapter.store(BlobKind.ORIGINAL, MediaType.PDF, content, "p", "tenant-a", "user-1");

    long start = PgDocumentBlobAdapter.PAGE_SIZE_BYTES - 10;
    long end = PgDocumentBlobAdapter.PAGE_SIZE_BYTES + 20;
    byte[] slice = adapter.readRange(stored.blobId(), new PageRange(start, end));

    assertThat(slice).hasSize((int) (end - start + 1));
    assertThat(slice[0]).isEqualTo(content[(int) start]);
    assertThat(slice[slice.length - 1]).isEqualTo(content[(int) end]);
  }

  @Test
  void delete_cascades_to_pages_and_makes_the_blob_unreadable() {
    BlobRef stored =
        adapter.store(
            BlobKind.ORIGINAL, MediaType.PDF, new byte[] {9, 9}, "p", "tenant-a", "user-1");

    adapter.delete(stored.blobId());

    assertThatThrownBy(() -> adapter.describe(stored.blobId()))
        .isInstanceOf(BlobNotFoundException.class);
    Integer remainingPages =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM document_blob_page WHERE blob_id = ?",
            Integer.class,
            stored.blobId());
    assertThat(remainingPages).isZero();
  }

  @Test
  void describe_of_unknown_blob_throws_not_found() {
    assertThatThrownBy(() -> adapter.describe(UUID.randomUUID()))
        .isInstanceOf(BlobNotFoundException.class);
  }

  @Test
  void store_rejects_content_above_the_50_mb_hard_cap() {
    byte[] tooLarge = new byte[(int) PgDocumentBlobAdapter.MAX_BLOB_SIZE_BYTES + 1];

    assertThatThrownBy(
            () ->
                adapter.store(
                    BlobKind.ORIGINAL, MediaType.PDF, tooLarge, "p", "tenant-a", "user-1"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
