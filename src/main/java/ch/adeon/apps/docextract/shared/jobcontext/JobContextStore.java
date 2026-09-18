package ch.adeon.apps.docextract.shared.jobcontext;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic, TTL-bounded in-memory store for transient per-{@code processId} job state (e.g.
 * structuring chunks) that must never be persisted at rest (ADR-006). Entries older than {@code
 * ttl} are treated as absent and swept on access; there is no filesystem or database fallback by
 * design, since the values held here may carry raw document content/PII.
 */
public final class JobContextStore<T> {

  private record Entry<T>(T value, Instant storedAt) {}

  private final ConcurrentHashMap<String, Entry<T>> entries = new ConcurrentHashMap<>();
  private final Duration ttl;

  public JobContextStore(Duration ttl) {
    this.ttl = ttl;
  }

  public void put(String processId, T value) {
    entries.put(processId, new Entry<>(value, Instant.now()));
  }

  public Optional<T> get(String processId) {
    Entry<T> entry = entries.get(processId);
    if (entry == null) {
      return Optional.empty();
    }
    if (Instant.now().isAfter(entry.storedAt().plus(ttl))) {
      entries.remove(processId);
      return Optional.empty();
    }
    return Optional.of(entry.value());
  }

  public void remove(String processId) {
    entries.remove(processId);
  }
}
