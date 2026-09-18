package ch.adeon.apps.docextract.structuring.adapter.out.memory;

import ch.adeon.apps.docextract.shared.jobcontext.JobContextStore;
import ch.adeon.apps.docextract.structuring.application.ChunkStagingPort;
import ch.adeon.apps.docextract.structuring.domain.DocChunk;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory-only chunk staging (ADR-006): chunks carry raw document text/PII and must never be
 * written to disk or a database. The TTL is a safety net against orphaned entries if a job never
 * reaches retrieval/extraction (e.g. crash), not a deliberate persistence window.
 */
@Component
public class InMemoryChunkStagingAdapter implements ChunkStagingPort {

  private final JobContextStore<List<DocChunk>> store;

  public InMemoryChunkStagingAdapter(
      @Value("${docextract.structuring.chunk-ttl:PT30M}") Duration ttl) {
    this.store = new JobContextStore<>(ttl);
  }

  @Override
  public void stage(String processId, List<DocChunk> chunks) {
    store.put(processId, chunks);
  }

  @Override
  public Optional<List<DocChunk>> retrieve(String processId) {
    return store.get(processId);
  }

  @Override
  public void discard(String processId) {
    store.remove(processId);
  }
}
