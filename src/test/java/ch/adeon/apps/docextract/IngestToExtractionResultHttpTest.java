package ch.adeon.apps.docextract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.adeon.apps.docextract.shared.testsupport.OpenAiCompatibleStub;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Full-stack proof of the FR-1…FR-4 path under profile {@code ci} (ARCHITECTURE § 7): the real
 * application (all production beans, real adapters) is started against a Testcontainers
 * Postgres/pgvector, the repo's own {@code docker/dvelop-mock} WireMock image (identityprovider,
 * DMS objdef/chunk upload/value lists — built via {@link ImageFromDockerfile}, no WireMock
 * dependency) and the in-process {@link OpenAiCompatibleStub} for LLM and embedding (the same stub
 * the {@code LlmPort} contract test uses). Then: {@code POST /api/v1/ingest/documents} → {@code 202
 * + processId} → {@code GET .../extraction-result} delivers attribute suggestions. Nothing is
 * mocked inside the application.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("ci")
class IngestToExtractionResultHttpTest {

  private static final String REPOSITORY_ID = "7cd5d582-2b5d-5e56-9a15-619f94e6e3ed";
  private static final String TENANT = "default-tenant";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
              DockerImageName.parse("pgvector/pgvector:pg17-bookworm")
                  .asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("docextract")
          .withUsername("docextract")
          .withPassword("docextract-test");

  private static final GenericContainer<?> DVELOP_MOCK =
      new GenericContainer<>(
              new ImageFromDockerfile().withFileFromPath(".", Path.of("docker/dvelop-mock")))
          .withExposedPorts(8080)
          .waitingFor(Wait.forHttp("/__admin/health").forStatusCode(200));

  private static final OpenAiCompatibleStub LLM =
      new OpenAiCompatibleStub(IngestToExtractionResultHttpTest::chatAnswer);

  static {
    POSTGRES.start();
    try {
      POSTGRES.execInContainer(
          "psql",
          "-U",
          "docextract",
          "-d",
          "docextract",
          "-c",
          "CREATE EXTENSION IF NOT EXISTS vector");
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    DVELOP_MOCK.start();
  }

  /** Classification prompt → the one document type; extraction prompt → one property value. */
  private static String chatAnswer(String requestBody) {
    if (requestBody.contains("Candidate document types")) {
      return "{\"documentTypeId\":\"DREC\"}";
    }
    return "{\"attributes\":[{\"propertyId\":\"11\",\"value\":\"RE-2026-0042\",\"values\":null,"
        + "\"confidence\":0.95,\"sourceExcerpt\":\"Rechnung Nr. RE-2026-0042\"}]}";
  }

  private static String dvelopMockUrl() {
    return "http://" + DVELOP_MOCK.getHost() + ":" + DVELOP_MOCK.getMappedPort(8080);
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("docextract.audit.actor-key", () -> "ci-test-actor-key-0123456789-0123456789");
    // jstore answers 404 → AppConfigPort falls back to the configured system base URI
    registry.add("docextract.dvelop.jstore-uri", () -> dvelopMockUrl() + "/store/not-available");
    registry.add(
        "docextract.dvelop.system-base-uri", IngestToExtractionResultHttpTest::dvelopMockUrl);
    registry.add("docextract.adapters.llm.base-url", LLM::baseUrl);
    registry.add("docextract.adapters.llm.api-key", () -> "");
    registry.add("docextract.adapters.llm.api-version", () -> "");
    registry.add("docextract.adapters.llm.model-id", () -> "stub-llm");
    registry.add("docextract.adapters.embedding.base-url", LLM::baseUrl);
  }

  @AfterAll
  static void stopStub() {
    LLM.close();
  }

  @LocalServerPort private int port;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void uploaded_pdf_yields_attribute_suggestions_via_the_rest_api() throws Exception {
    RestClient http =
        RestClient.builder()
            .baseUrl("http://localhost:" + port + "/adeon-docextract/api/v1")
            .defaultHeader("Authorization", "Bearer ci-test-token")
            .build();

    MultipartBodyBuilder form = new MultipartBodyBuilder();
    form.part(
            "file",
            new ByteArrayResource(invoicePdf()) {
              @Override
              public String getFilename() {
                return "invoice.pdf";
              }
            })
        .contentType(MediaType.APPLICATION_PDF);
    form.part("repositoryId", REPOSITORY_ID);

    var upload =
        http.post()
            .uri("/ingest/documents")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(form.build())
            .retrieve()
            .toEntity(String.class);

    assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    String processId = JSON.readTree(upload.getBody()).get("processId").asString();
    assertThat(processId).isNotBlank();

    await()
        .atMost(Duration.ofSeconds(60))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          JsonNode result =
              JSON.readTree(
                  http.get()
                      .uri("/processes/{id}/extraction-result", processId)
                      .retrieve()
                      .body(String.class));
          assertThat(result.path("documentTypeId").asString()).isEqualTo("DREC");
          JsonNode attributes = result.path("attributes");
          assertThat(attributes.size()).isEqualTo(1);
          assertThat(attributes.get(0).path("propertyId").asString()).isEqualTo("11");
          assertThat(attributes.get(0).path("value").asString()).isEqualTo("RE-2026-0042");
        });

    // embedding + classify + suggest went through the OpenAI-compatible adapters
    assertThat(LLM.embeddingRequestBodies()).hasSize(1);
    assertThat(LLM.chatRequestBodies()).hasSize(2);

    // the vector was staged as PENDING for the tenant (never retrievable before approval, C-2/C-7)
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM embedding WHERE status = 'PENDING' AND tenant_id = ?"
                    + " AND extraction_source = 'pdfbox'",
                Integer.class,
                TENANT))
        .isEqualTo(1);

    // audit names provider/model/tokens and carries only hashes, never prompt text (C-4)
    List<Map<String, Object>> llmAudit =
        jdbcTemplate.queryForList(
            "SELECT event_type, provider, model_id, token_count, args_hash FROM audit_entry"
                + " WHERE event_type LIKE 'extraction.%' ORDER BY id");
    assertThat(llmAudit).hasSize(2);
    assertThat(llmAudit)
        .allSatisfy(row -> {
          assertThat(row.get("model_id")).isEqualTo("stub-llm");
          assertThat(row.get("provider")).isEqualTo("openai-compatible");
          assertThat(row.get("token_count")).isEqualTo(10);
          assertThat((String) row.get("args_hash")).hasSize(64).doesNotContain("Rechnung");
        });
  }

  private static byte[] invoicePdf() throws Exception {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDPage page = new PDPage();
      document.addPage(page);
      try (PDPageContentStream content = new PDPageContentStream(document, page)) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(50, 700);
        content.showText("Rechnung Nr. RE-2026-0042 Muster AG Betrag 123.45 CHF");
        content.endText();
      }
      document.save(out);
      return out.toByteArray();
    }
  }
}
