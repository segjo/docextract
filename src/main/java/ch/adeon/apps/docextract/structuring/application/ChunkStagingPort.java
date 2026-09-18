package ch.adeon.apps.docextract.structuring.application;

import ch.adeon.apps.docextract.structuring.domain.DocChunk;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port staging chunks for the retrieval/extraction steps to consume, keyed by {@code
 * processId}. Chunks are Volltext-Fragmente with raw PII (ADR-006) and must never be persisted at
 * rest — implementations are in-memory only, with a TTL as a safety net against orphaned entries.
 */
public interface ChunkStagingPort {

  void stage(String processId, List<DocChunk> chunks);

  Optional<List<DocChunk>> retrieve(String processId);

  void discard(String processId);
}
