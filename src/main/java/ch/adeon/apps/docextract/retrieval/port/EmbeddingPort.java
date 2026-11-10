package ch.adeon.apps.docextract.retrieval.port;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingBatchResult;
import java.util.List;

/**
 * Outbound port for the embedding model (Ollama, dedicated embedding model, separate from the
 * extraction LLM — §8.5). Implementations embed each text independently and return vectors in the
 * same order as the input, together with the token usage and model identity for the audit trail
 * (C-4, NfA-7).
 */
public interface EmbeddingPort {

  EmbeddingBatchResult embed(List<String> texts);
}
