package ch.adeon.apps.docextract.extraction.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.adeon.apps.docextract.extraction.domain.LlmCapabilities;
import ch.adeon.apps.docextract.extraction.domain.LlmResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Behavioural contract every {@link LlmPort} adapter must satisfy (NfA-8, ARCHITECTURE § 5.2/9):
 * for a schema-constrained call, the response carries parseable JSON plus the model identity and
 * token count the audit trail needs (C-4/NfA-7), and the adapter declares its capabilities. Every
 * adapter's own test extends this class and supplies one or more named, backed {@link
 * #configurations()} (see {@code OpenAiCompatibleLlmAdapterContractTest}, which supplies a "local"
 * config without an api-key and an "external" config with api-key + api-version — NfA-8's "LLM-Port
 * mit ≥ 2 Adaptern (1 lokal, 1 extern)" target, exercised here as ≥ 2 configurations of the one
 * generic adapter ADR-011 deliberately chose instead of two adapter classes); each test below runs
 * once per configuration ({@code @TestInstance(PER_CLASS)} so the overridden method can be an
 * instance method).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class LlmPortContractTest {

  private static final Map<String, Object> SCHEMA =
      Map.of(
          "type", "object",
          "properties", Map.of("value", Map.of("type", "string")),
          "required", List.of("value"),
          "additionalProperties", false);

  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** One named {@link LlmPort} instance per configuration to run the contract against. */
  protected abstract Stream<Named<LlmPort>> configurations();

  @ParameterizedTest(name = "{0}")
  @MethodSource("configurations")
  void generates_schema_shaped_json_with_model_identity_and_token_count(LlmPort port) {
    LlmResponse response = port.generate("You answer only in JSON.", "Say hello.", SCHEMA);

    assertThat(response.json()).isNotBlank();
    assertThatCode(() -> JSON.readTree(response.json())).doesNotThrowAnyException();
    assertThat(response.modelInfo()).isNotNull();
    assertThat(response.modelInfo().provider()).isNotBlank();
    assertThat(response.modelInfo().modelId()).isNotBlank();
    assertThat(response.promptTokenCount()).isGreaterThanOrEqualTo(0);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("configurations")
  void declares_its_capabilities(LlmPort port) {
    LlmCapabilities capabilities = port.capabilities();

    assertThat(capabilities).isNotNull();
    assertThat(capabilities.structuredOutput()).isNotNull();
    assertThat(capabilities.inputs()).isNotNull();
  }
}
