package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import java.util.List;

/**
 * Outbound port for the embedding model (Ollama, dedicated embedding model, separate from the
 * extraction LLM — §8.5). Implementations embed each text independently and return vectors in the
 * same order as the input.
 */
public interface EmbeddingPort {

  List<EmbeddingVector> embed(List<String> texts);
}
