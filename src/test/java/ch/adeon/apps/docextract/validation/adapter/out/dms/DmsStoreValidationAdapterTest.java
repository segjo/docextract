package ch.adeon.apps.docextract.validation.adapter.out.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
import ch.adeon.apps.docextract.validation.port.PropertyValue;
import ch.adeon.apps.docextract.validation.port.StoreValidationError;
import ch.adeon.apps.docextract.validation.port.StoreValidationRequest;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult.Invalid;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult.Valid;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code DmsStoreValidationAdapter} against the repo's own {@code docker/dvelop-mock} WireMock
 * image (the stubs {@code dms_storevalidate_200/400}), built like in {@code
 * IngestToExtractionResultHttpTest}: real HTTP, no WireMock dependency in the build (ADR-016).
 */
class DmsStoreValidationAdapterTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final GenericContainer<?> DVELOP_MOCK =
      new GenericContainer<>(
              new ImageFromDockerfile().withFileFromPath(".", Path.of("docker/dvelop-mock")))
          .withExposedPorts(8080)
          .waitingFor(Wait.forHttp("/__admin/health").forStatusCode(200));

  private static DvelopCredential credential;
  private static RestClient admin;
  private static DmsStoreValidationAdapter adapter;

  @BeforeAll
  static void start() {
    DVELOP_MOCK.start();
    String baseUri = "http://" + DVELOP_MOCK.getHost() + ":" + DVELOP_MOCK.getMappedPort(8080);
    credential = new DvelopCredential("Authorization", "Bearer secret-token", "de-CH", baseUri);
    admin = RestClient.builder().baseUrl(baseUri).build();
    DmsPropertyPayloadMapper mapper = new DmsPropertyPayloadMapper("Europe/Zurich");
    adapter =
        new DmsStoreValidationAdapter(
            JSON, mapper, new StoreValidateResponseParser(JSON, mapper), Duration.ofSeconds(1));
  }

  @AfterAll
  static void stop() {
    DVELOP_MOCK.stop();
  }

  private static Map<String, DmsPropertyDefinition> definitions() {
    Map<String, DmsPropertyDefinition> definitions = new LinkedHashMap<>();
    definitions.put("11", definition("11", DmsPropertyDataType.ALPHANUMERIC));
    definitions.put("4", definition("4", DmsPropertyDataType.DATE));
    definitions.put("13", definition("13", DmsPropertyDataType.MONEY));
    definitions.put("14", definition("14", DmsPropertyDataType.ALPHANUMERIC));
    definitions.put("15", definition("15", DmsPropertyDataType.ALPHANUMERIC));
    return definitions;
  }

  private static DmsPropertyDefinition definition(String id, DmsPropertyDataType type) {
    return new DmsPropertyDefinition(
        id, id, type, false, false, false, false, true, false, false, false, 0, 0, List.of(),
        false);
  }

  private static StoreValidationRequest request(String repositoryId, PropertyValue... properties) {
    return new StoreValidationRequest(
        repositoryId, "DREC", "invoice.pdf", List.of(properties), definitions());
  }

  private static JsonNode lastRequestTo(String repositoryId) {
    JsonNode journal =
        JSON.readTree(admin.get().uri("/__admin/requests").retrieve().body(String.class));
    for (JsonNode entry : journal.get("requests")) {
      if (entry.at("/request/url").asString().contains("/dms/r/" + repositoryId + "/")) {
        return entry.get("request");
      }
    }
    throw new AssertionError("no request to repository " + repositoryId);
  }

  private static void stubStatus(String repositoryId, int status, int delayMillis) {
    admin
        .post()
        .uri("/__admin/mappings")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {"priority":1,
             "request":{"method":"POST","urlPath":"/dms/r/%s/storevalidate"},
             "response":{"status":%d,"fixedDelayMilliseconds":%d,"body":"{\\"message\\":\\"secret\\"}"}}
            """
                .formatted(repositoryId, status, delayMillis))
        .retrieve()
        .toBodilessEntity();
  }

  @Test
  void a_complete_document_is_valid_and_the_answer_is_read_through_the_mapper() {
    StoreValidationResult result =
        adapter.validate(
            "tenant-a",
            request(
                "repo-valid",
                new PropertyValue("11", "dawd", null),
                new PropertyValue("4", "11.11.2026", null),
                new PropertyValue("13", "12,00", null),
                new PropertyValue("14", "unbekannt", null)),
            credential);

    assertThat(result).isInstanceOf(Valid.class);
    Valid valid = (Valid) result;
    assertThat(valid.submitted())
        .containsExactly(
            new PropertyValue("11", "dawd", null),
            new PropertyValue("4", "2026-11-11", null),
            new PropertyValue("13", "12.00", null));
    assertThat(valid.returned())
        .containsExactly(
            new PropertyValue("11", "dawd", null),
            new PropertyValue("4", "2026-11-11", null),
            new PropertyValue("13", "12.00", null),
            new PropertyValue("15", "In Prüfung", null));
  }

  @Test
  void the_request_is_valid_json_sent_as_text_plain_with_the_callers_credential() {
    adapter.validate(
        "tenant-a",
        request(
            "repo-wire",
            new PropertyValue("11", "dawd", null),
            new PropertyValue("4", "2026-11-11", null)),
        credential);

    JsonNode sent = lastRequestTo("repo-wire");
    assertThat(sent.at("/headers/Content-Type").asString()).isEqualToIgnoringCase("text/plain");
    assertThat(sent.at("/headers/Authorization").asString()).isEqualTo("Bearer secret-token");
    assertThat(sent.at("/headers/Origin").asString()).isEqualTo(credential.baseUri());
    assertThat(sent.at("/headers/Accept-Language").asString()).isEqualTo("de-CH");
    JsonNode body = JSON.readTree(sent.get("body").asString());
    assertThat(body.get("type").asInt()).isEqualTo(1);
    assertThat(body.get("objectDefinitionId").asString()).isEqualTo("DREC");
    assertThat(body.at("/systemProperties/property_filename").asString()).isEqualTo("invoice.pdf");
    assertThat(body.at("/systemProperties/property_filetype").asString()).isEqualTo("pdf");
    assertThat(body.get("remarks").size()).isZero();
    assertThat(body.at("/extendedProperties/4").asString()).isEqualTo("2026-11-11");
  }

  @Test
  void a_missing_mandatory_field_is_one_error_with_the_property_and_the_dms_code() {
    StoreValidationResult result =
        adapter.validate(
            "tenant-a", request("repo-invalid", new PropertyValue("11", "dawd", null)), credential);

    assertThat(result).isInstanceOf(Invalid.class);
    StoreValidationError error = ((Invalid) result).error();
    assertThat(error.propertyId()).isEqualTo("4");
    assertThat(error.dmsCode()).isEqualTo("0010019");
    assertThat(error.message()).contains("Pflichtfeld").contains("Rechnungsdatum");
  }

  @Test
  void an_error_property_outside_the_allowlist_is_not_reported() {
    Map<String, DmsPropertyDefinition> withoutDate = definitions();
    withoutDate.remove("4");

    StoreValidationResult result =
        adapter.validate(
            "tenant-a",
            new StoreValidationRequest(
                "repo-allowlist",
                "DREC",
                "invoice.pdf",
                List.of(new PropertyValue("11", "dawd", null)),
                withoutDate),
            credential);

    assertThat(((Invalid) result).error().propertyId()).isNull();
  }

  @Test
  void statuses_without_a_validation_result_are_an_unavailable_dms() {
    for (int status : List.of(401, 403, 404, 422, 500, 503)) {
      String repositoryId = "repo-" + status;
      stubStatus(repositoryId, status, 0);

      assertThatThrownBy(() ->
              adapter.validate(
                  "tenant-a",
                  request(repositoryId, new PropertyValue("11", "x", null)),
                  credential))
          .as("HTTP " + status)
          .isInstanceOf(AdapterUnavailableException.class)
          .satisfies(ex ->
              assertThat(((AdapterUnavailableException) ex).detail()).doesNotContain("secret"));
    }
  }

  @Test
  void a_timeout_is_an_unavailable_dms() {
    stubStatus("repo-slow", 200, 3000);

    assertThatThrownBy(() ->
            adapter.validate(
                "tenant-a", request("repo-slow", new PropertyValue("11", "x", null)), credential))
        .isInstanceOf(AdapterUnavailableException.class);
  }

  @Test
  void an_unreachable_dms_is_an_unavailable_dms() {
    DvelopCredential nowhere =
        new DvelopCredential("Authorization", "Bearer t", null, "http://127.0.0.1:1");

    assertThatThrownBy(() ->
            adapter.validate(
                "tenant-a", request("repo-x", new PropertyValue("11", "x", null)), nowhere))
        .isInstanceOf(AdapterUnavailableException.class);
  }
}
