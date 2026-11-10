package ch.adeon.apps.docextract.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.extraction.adapter.out.postgres.PgExtractionResultAdapter;
import ch.adeon.apps.docextract.extraction.application.ExtractedAttribute;
import ch.adeon.apps.docextract.extraction.application.ExtractionResult;
import ch.adeon.apps.docextract.process.adapter.out.postgres.PgProcessDocumentAdapter;
import ch.adeon.apps.docextract.retrieval.adapter.out.postgres.PgDocumentTypeCandidatesAdapter;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentType;
import java.time.Duration;
import java.util.List;
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
import tools.jackson.databind.ObjectMapper;

/** NfA-4 on the tables: a process's rows are only visible to, and writable by, its tenant. */
@Testcontainers
class PgTenantFilterAdapterTest {

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
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
    Flyway.configure().dataSource(dataSource).load().migrate();
  }

  @AfterEach
  void clearTables() {
    jdbcTemplate.update("DELETE FROM extraction_result");
    jdbcTemplate.update("DELETE FROM document_type_candidate");
    jdbcTemplate.update("DELETE FROM document_blob");
  }

  @Test
  void extraction_result_is_visible_and_replaceable_for_its_tenant_only() {
    PgExtractionResultAdapter adapter =
        new PgExtractionResultAdapter(jdbcTemplate, new ObjectMapper(), Duration.ofHours(1));
    ExtractionResult result =
        new ExtractionResult("DREC", List.of(new ExtractedAttribute("1", "v", null, 0.9, "x")));
    adapter.store("tenant-a", "p1", result);

    assertThat(adapter.find("tenant-a", "p1")).contains(result);
    assertThat(adapter.find("tenant-b", "p1")).isEmpty();

    // the key is (tenant_id, process_id): the same id of another tenant is a separate row
    ExtractionResult other = new ExtractionResult("OTHER", List.of());
    adapter.store("tenant-b", "p1", other);

    assertThat(adapter.find("tenant-a", "p1")).contains(result);
    assertThat(adapter.find("tenant-b", "p1")).contains(other);
  }

  @Test
  void document_type_candidates_are_visible_for_their_tenant_only() {
    PgDocumentTypeCandidatesAdapter adapter =
        new PgDocumentTypeCandidatesAdapter(jdbcTemplate, new ObjectMapper(), Duration.ofHours(1));
    List<DmsDocumentMetadata> candidates =
        List.of(
            new DmsDocumentMetadata(
                new DmsDocumentType("DREC", "Rechnung", List.of()), null, List.of()));
    adapter.store("tenant-a", "p1", candidates);

    assertThat(adapter.find("tenant-a", "p1")).isPresent();
    assertThat(adapter.find("tenant-b", "p1")).isEmpty();

    adapter.store("tenant-b", "p1", candidates);
    assertThat(adapter.find("tenant-a", "p1")).isPresent();
    assertThat(adapter.find("tenant-b", "p1")).isPresent();
  }

  @Test
  void process_document_is_visible_and_updatable_for_its_tenant_only() {
    jdbcTemplate.update(
        """
        INSERT INTO document_blob
          (blob_id, kind, media_type, size_bytes, process_id, tenant_id, user_id, expires_at)
        VALUES (?, 'ORIGINAL', 'application/pdf', 1, 'p1', 'tenant-a', 'u',
                now() + interval '1 hour')
        """,
        UUID.randomUUID());
    PgProcessDocumentAdapter adapter = new PgProcessDocumentAdapter(jdbcTemplate);
    adapter.save("tenant-a", "p1", "/dms/chunk/1", "a.pdf");

    assertThat(adapter.find("tenant-a", "p1")).isPresent();
    assertThat(adapter.find("tenant-b", "p1")).isEmpty();

    adapter.save("tenant-b", "p1", "/dms/chunk/evil", "evil.pdf");
    assertThat(adapter.find("tenant-a", "p1"))
        .hasValueSatisfying(
            document -> assertThat(document.dmsLocation()).isEqualTo("/dms/chunk/1"));
  }
}
