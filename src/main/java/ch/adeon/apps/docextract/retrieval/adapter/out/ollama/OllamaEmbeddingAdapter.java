package ch.adeon.apps.docextract.retrieval.adapter.out.ollama;

import ch.adeon.apps.docextract.retrieval.application.EmbeddingException;
import ch.adeon.apps.docextract.retrieval.application.EmbeddingPort;
import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.shared.http.Http1RestClientFactory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Embeds document chunks via Ollama's batch embedding endpoint ({@code POST /api/embed}, FR-3).
 * Runs against a dedicated embedding model, deliberately separate from the extraction LLM (§8.5);
 * the model tag is digest-pinned by the operator per C-5, this adapter only names it.
 */
@Component
public class OllamaEmbeddingAdapter implements EmbeddingPort {

  private final RestClient restClient;
  private final String baseUri;
  private final String model;

  public OllamaEmbeddingAdapter(
      @Value("${docextract.ollama.base-uri}") String baseUri,
      @Value("${docextract.retrieval.embedding-model}") String model) {
    this.restClient = Http1RestClientFactory.create();
    this.baseUri = baseUri;
    this.model = model;
  }

  @Override
  public List<EmbeddingVector> embed(List<String> texts) {
    if (texts.isEmpty()) {
      return List.of();
    }
    try {
      EmbedResponse response =
          restClient
              .post()
              .uri(baseUri + "/api/embed")
              .body(new EmbedRequest(model, texts))
              .retrieve()
              .body(EmbedResponse.class);
      if (response == null || response.embeddings() == null) {
        throw new EmbeddingException("Ollama returned no embeddings for model " + model);
      }
      if (response.embeddings().size() != texts.size()) {
        throw new EmbeddingException(
            "Ollama returned %d embeddings for %d input texts"
                .formatted(response.embeddings().size(), texts.size()));
      }
      return response.embeddings().stream().map(EmbeddingVector::new).toList();
    } catch (RestClientException ex) {
      throw new EmbeddingException(
          "Embedding call to Ollama failed at " + baseUri + ": " + ex.getMessage(), ex);
    }
  }

  private record EmbedRequest(String model, List<String> input) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record EmbedResponse(List<float[]> embeddings) {}
}
