package ch.adeon.apps.docextract.extraction.port;

import ch.adeon.apps.docextract.extraction.domain.LlmCapabilities;
import ch.adeon.apps.docextract.extraction.domain.LlmResponse;
import java.util.Map;

/**
 * Outbound port to the extraction LLM (FR-4), deliberately separate from {@code
 * retrieval.port.EmbeddingPort}'s embedding model (§8.5). Every call is schema-constrained (T-1):
 * {@code jsonSchema} is handed to the model as a strict output format, never left to free- form
 * generation, and the document content only ever appears inside {@code userPrompt} as untrusted
 * data, never as part of the instruction itself. Server-side schema validation of the response is
 * always performed by the caller, regardless of {@link #capabilities()} (ADR-011).
 */
public interface LlmPort {

  LlmResponse generate(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema);

  LlmCapabilities capabilities();
}
