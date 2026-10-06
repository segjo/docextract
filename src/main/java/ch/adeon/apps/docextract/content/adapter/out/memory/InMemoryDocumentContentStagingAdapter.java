package ch.adeon.apps.docextract.content.adapter.out.memory;

import ch.adeon.apps.docextract.content.application.ContentStagingPort;
import ch.adeon.apps.docextract.content.domain.DocumentContent;
import ch.adeon.apps.docextract.shared.jobcontext.JobContextStore;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory-only staging (ADR-006): document content carries raw document text/PII and must never
 * be written to disk or a database. The TTL is a safety net against orphaned entries if a job never
 * reaches extraction (e.g. crash), not a deliberate persistence window.
 */
@Component
public class InMemoryDocumentContentStagingAdapter implements ContentStagingPort {

  private final JobContextStore<DocumentContent> store;

  public InMemoryDocumentContentStagingAdapter(
      @Value("${docextract.content.staging-ttl:PT30M}") Duration ttl) {
    this.store = new JobContextStore<>(ttl);
  }

  @Override
  public void stage(String processId, DocumentContent content) {
    store.put(processId, content);
  }

  @Override
  public Optional<DocumentContent> retrieve(String processId) {
    return store.get(processId);
  }

  @Override
  public void discard(String processId) {
    store.remove(processId);
  }
}
