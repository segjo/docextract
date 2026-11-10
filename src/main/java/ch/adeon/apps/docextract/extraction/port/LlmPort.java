package ch.adeon.apps.docextract.extraction.port;

import ch.adeon.apps.docextract.extraction.domain.LlmCapabilities;
import ch.adeon.apps.docextract.extraction.domain.LlmResponse;
import ch.adeon.apps.docextract.extraction.domain.LlmTool;
import java.util.List;
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

  default LlmResponse generate(
      String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
    return generate(systemPrompt, userPrompt, jsonSchema, List.of());
  }

  /** {@code tools} are read-only lookups executed by DocExtract, never by the model (ADR-014). */
  LlmResponse generate(
      String systemPrompt, String userPrompt, Map<String, Object> jsonSchema, List<LlmTool> tools);

  LlmCapabilities capabilities();
}
