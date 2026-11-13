package ch.adeon.apps.docextract.validation.adapter.out.dms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The dvelop-mock stubs and the test fixtures of ADR-016 (decisions 2 and 3) are two copies of the
 * same documented bodies; they must not drift apart.
 */
class StoreValidateMockMappingTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Path MAPPINGS = Path.of("docker/dvelop-mock/mappings");

  private static JsonNode mapping(String name) throws IOException {
    return JSON.readTree(Files.readString(MAPPINGS.resolve(name)));
  }

  private static JsonNode fixture(String name) throws IOException {
    try (InputStream in =
        StoreValidateMockMappingTest.class.getResourceAsStream("/dms/storevalidate/" + name)) {
      return JSON.readTree(in);
    }
  }

  @Test
  void the_200_stub_answers_with_the_documented_body() throws IOException {
    JsonNode stub = mapping("dms_storevalidate_200.json");

    assertThat(stub.at("/response/status").asInt()).isEqualTo(200);
    assertThat(stub.at("/response/jsonBody")).isEqualTo(fixture("response-200.json"));
  }

  @Test
  void the_400_stub_answers_with_the_documented_body() throws IOException {
    JsonNode stub = mapping("dms_storevalidate_400.json");

    assertThat(stub.at("/response/status").asInt()).isEqualTo(400);
    assertThat(stub.at("/response/jsonBody")).isEqualTo(fixture("response-400.json"));
  }

  @Test
  void both_stubs_demand_text_plain_on_the_storevalidate_post() throws IOException {
    for (String name : new String[] {"dms_storevalidate_200.json", "dms_storevalidate_400.json"}) {
      JsonNode request = mapping(name).get("request");

      assertThat(request.get("method").asString()).isEqualTo("POST");
      assertThat(request.get("urlPathPattern").asString()).endsWith("/storevalidate");
      assertThat(request.at("/headers/Content-Type/contains").asString()).isEqualTo("text/plain");
    }
  }

  @Test
  void the_documented_request_is_valid_json() throws IOException {
    JsonNode request = fixture("request.json");

    assertThat(request.get("type").asInt()).isEqualTo(1);
    assertThat(request.get("objectDefinitionId").asString()).isEqualTo("DKOR");
  }
}
