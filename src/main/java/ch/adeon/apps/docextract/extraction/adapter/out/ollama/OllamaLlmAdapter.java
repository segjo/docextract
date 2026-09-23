package ch.adeon.apps.docextract.extraction.adapter.out.ollama;

import ch.adeon.apps.docextract.extraction.application.LlmException;
import ch.adeon.apps.docextract.extraction.application.LlmPort;
import ch.adeon.apps.docextract.extraction.domain.LlmResponse;
import ch.adeon.apps.docextract.shared.http.Http1RestClientFactory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Generation calls to the extraction LLM via Ollama's chat endpoint ({@code POST /api/chat}, FR-4),
 * with {@code format} set to the caller's JSON schema for schema-constrained decoding (T-1) — never
 * a free-form completion. Runs against a dedicated model, deliberately separate from the embedding
 * model (§8.5); the model tag is digest-pinned by the operator per C-5, this adapter only names it.
 */
@Component
public class OllamaLlmAdapter implements LlmPort {

  private final RestClient restClient;
  private final String baseUri;
  private final String model;

  public OllamaLlmAdapter(
      @Value("${docextract.ollama.base-uri}") String baseUri,
      @Value("${docextract.extraction.llm-model}") String model) {
    this.restClient = Http1RestClientFactory.create();
    this.baseUri = baseUri;
    this.model = model;
  }

  @Override
  public LlmResponse generate(
      String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
    try {
      ChatResponse response =
          restClient
              .post()
              .uri(baseUri + "/api/chat")
              .body(
                  new ChatRequest(
                      model,
                      List.of(
                          new ChatMessage("system", systemPrompt),
                          new ChatMessage("user", userPrompt)),
                      false,
                      jsonSchema))
              .retrieve()
              .body(ChatResponse.class);
      if (response == null || response.message() == null || response.message().content() == null) {
        throw new LlmException("Ollama returned no content for model " + model);
      }
      return new LlmResponse(response.message().content(), model, response.promptEvalCount());
    } catch (RestClientException ex) {
      throw new LlmException(
          "Chat call to Ollama failed at " + baseUri + ": " + ex.getMessage(), ex);
    }
  }

  private record ChatRequest(
      String model, List<ChatMessage> messages, boolean stream, Map<String, Object> format) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ChatMessage(String role, String content) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ChatResponse(
      ChatMessage message, @JsonProperty("prompt_eval_count") int promptEvalCount) {}
}
