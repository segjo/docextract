package ch.adeon.apps.docextract.process.application;

import java.util.Optional;

/**
 * Cross-module port (same role as {@code ProcessEventPort}/{@code
 * ingest.application.DocumentHashStagingPort}) staging the repository the user picked in the
 * import mask for a given {@code processId}, so every later step of that same process (retrieval's
 * object-definition fallback, extraction's value-list lookups) targets the user's chosen repository
 * instead of a single globally configured one. Kept in-memory alongside the other per-process job
 * state (ADR-006) — the repository id is not PII, but is only ever needed transiently for the
 * lifetime of one process.
 */
public interface ProcessRepositoryPort {

  void stage(String processId, String repositoryId);

  Optional<String> retrieve(String processId);
}
