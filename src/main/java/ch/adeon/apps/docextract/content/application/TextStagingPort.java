package ch.adeon.apps.docextract.content.application;

import ch.adeon.apps.docextract.content.domain.ExtractedText;
import java.util.Optional;

/**
 * Outbound port staging the extracted text for the retrieval step to consume, keyed by {@code
 * processId}. Extracted text carries raw document content/PII (ADR-006) and must never be persisted
 * at rest — implementations are in-memory only, with a TTL as a safety net against orphaned
 * entries.
 */
public interface TextStagingPort {

  void stage(String processId, ExtractedText text);

  Optional<ExtractedText> retrieve(String processId);

  void discard(String processId);
}
