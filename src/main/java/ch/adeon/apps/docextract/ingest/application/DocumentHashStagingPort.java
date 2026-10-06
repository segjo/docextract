package ch.adeon.apps.docextract.ingest.application;

import java.util.Optional;

/**
 * Outbound port staging the uploaded document's SHA-256 hash for the retrieval step to consume,
 * keyed by {@code processId} (CHANGES.md duplicate-detection step). The hash itself is not PII and
 * could be persisted, but is kept in-memory alongside the other per-process job state (ADR-006)
 * since it is only ever needed transiently between ingest and the embedding-staging step.
 */
public interface DocumentHashStagingPort {

  void stage(String processId, String documentHash);

  Optional<String> retrieve(String processId);

  void discard(String processId);
}
