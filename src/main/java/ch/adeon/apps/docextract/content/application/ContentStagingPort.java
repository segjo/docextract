package ch.adeon.apps.docextract.content.application;

import ch.adeon.apps.docextract.content.domain.DocumentContent;
import java.util.Optional;

/**
 * Outbound port staging the document representation for the extraction step to consume, keyed by
 * {@code processId}. Never persisted at rest (ADR-006) — in-memory only, TTL as an orphan safety
 * net.
 */
public interface ContentStagingPort {

  void stage(String processId, DocumentContent content);

  Optional<DocumentContent> retrieve(String processId);

  void discard(String processId);
}
