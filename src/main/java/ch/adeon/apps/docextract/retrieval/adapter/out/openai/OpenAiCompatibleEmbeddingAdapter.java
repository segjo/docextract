package ch.adeon.apps.docextract.retrieval.adapter.out.openai;

import ch.adeon.apps.docextract.retrieval.application.EmbeddingException;
import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.port.EmbeddingPort;
import java.util.List;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reference {@link EmbeddingPort} adapter (ADR-011): the widely supported OpenAI-compatible
 * embeddings API, reachable both from a locally hosted runtime (e.g. Ollama's {@code /v1} endpoint)
 * and a hosted external provider — same adapter, only configuration differs (ADR-009: Spring AI
 * types never leave this adapter).
 */
@Component
public class OpenAiCompatibleEmbeddingAdapter implements EmbeddingPort {

  private final OpenAiEmbeddingModel embeddingModel;

  public OpenAiCompatibleEmbeddingAdapter(
      @Value("${docextract.adapters.embedding.base-url}") String baseUrl,
      @Value("${docextract.adapters.embedding.api-key:}") String apiKey,
      @Value("${docextract.adapters.embedding.model-id}") String modelId) {
    OpenAiEmbeddingOptions options =
        OpenAiEmbeddingOptions.builder().baseUrl(baseUrl).apiKey(apiKey).model(modelId).build();
    this.embeddingModel = OpenAiEmbeddingModel.builder().options(options).build();
  }

  @Override
  public List<EmbeddingVector> embed(List<String> texts) {
    if (texts.isEmpty()) {
      return List.of();
    }
    try {
      EmbeddingResponse response = embeddingModel.embedForResponse(texts);
      List<Embedding> results = response.getResults();
      if (results.size() != texts.size()) {
        throw new EmbeddingException(
            "returned %d embeddings for %d input texts".formatted(results.size(), texts.size()));
      }
      return results.stream().map(embedding -> new EmbeddingVector(embedding.getOutput())).toList();
    } catch (EmbeddingException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new EmbeddingException("Embedding call failed: " + ex.getMessage(), ex);
    }
  }
}
