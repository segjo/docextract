package ch.adeon.apps.docextract.process.adapter.out.memory;

import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.shared.jobcontext.JobContextStore;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** In-memory staging; the TTL is a safety net if a process never reaches its later steps. */
@Component
public class InMemoryProcessRepositoryAdapter implements ProcessRepositoryPort {

  private final JobContextStore<String> store;

  public InMemoryProcessRepositoryAdapter(
      @Value("${docextract.content.staging-ttl:PT30M}") Duration ttl) {
    this.store = new JobContextStore<>(ttl);
  }

  @Override
  public void stage(String processId, String repositoryId) {
    store.put(processId, repositoryId);
  }

  @Override
  public Optional<String> retrieve(String processId) {
    return store.get(processId);
  }
}
