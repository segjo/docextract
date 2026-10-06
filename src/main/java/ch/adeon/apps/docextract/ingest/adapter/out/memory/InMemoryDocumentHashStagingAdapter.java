package ch.adeon.apps.docextract.ingest.adapter.out.memory;

import ch.adeon.apps.docextract.ingest.application.DocumentHashStagingPort;
import ch.adeon.apps.docextract.shared.jobcontext.JobContextStore;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** In-memory staging; the TTL is a safety net if a job never reaches retrieval (e.g. crash). */
@Component
public class InMemoryDocumentHashStagingAdapter implements DocumentHashStagingPort {

  private final JobContextStore<String> store;

  public InMemoryDocumentHashStagingAdapter(
      @Value("${docextract.content.staging-ttl:PT30M}") Duration ttl) {
    this.store = new JobContextStore<>(ttl);
  }

  @Override
  public void stage(String processId, String documentHash) {
    store.put(processId, documentHash);
  }

  @Override
  public Optional<String> retrieve(String processId) {
    return store.get(processId);
  }

  @Override
  public void discard(String processId) {
    store.remove(processId);
  }
}
