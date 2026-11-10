package ch.adeon.apps.docextract.extraction.adapter.out.openai;

import ch.adeon.apps.docextract.extraction.port.LlmPort;
import ch.adeon.apps.docextract.extraction.port.LlmPortContractTest;
import ch.adeon.apps.docextract.shared.testsupport.OpenAiCompatibleStub;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves the reference {@link LlmPort} adapter (ADR-011) fulfils the port's contract-test suite
 * (NfA-8) in both configurations the adapter supports: a local runtime (no api-key) and a hosted
 * external/Azure-flavoured endpoint (api-key + api-version) — same adapter class, only
 * configuration differs. Both talk to the shared {@link OpenAiCompatibleStub}; {@code api-version}
 * does not change the request path/body this adapter's client sends (verified), so one stub
 * serves both.
 */
class OpenAiCompatibleLlmAdapterContractTest extends LlmPortContractTest {

  private static OpenAiCompatibleStub stub;

  @BeforeAll
  static void startStub() {
    stub = new OpenAiCompatibleStub(requestBody -> "{\"value\":\"example\"}");
  }

  @AfterAll
  static void stopStub() {
    stub.close();
  }

  @Override
  protected Stream<Named<LlmPort>> configurations() {
    LlmPort local =
        new OpenAiCompatibleLlmAdapter(
            JsonMapper.builder().build(),
            stub.baseUrl(),
            "",
            "contract-test-model-local",
            "",
            "",
            "");
    LlmPort external =
        new OpenAiCompatibleLlmAdapter(
            JsonMapper.builder().build(),
            stub.baseUrl(),
            "contract-test-key",
            "contract-test-model-external",
            "2025-04-01-preview",
            "",
            "");
    return Stream.of(
        Named.of("lokal (ohne api-key)", local),
        Named.of("extern (api-key + api-version)", external));
  }
}
