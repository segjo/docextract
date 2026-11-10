package ch.adeon.apps.docextract.process.application;

import java.util.Optional;

/**
 * Cross-module port (same role as {@code ProcessEventPort}/{@code
 * ingest.application.DocumentHashStagingPort}) staging the repository the user picked in the import
 * mask for a given {@code processId}, so every later step of that same process (retrieval's
 * object-definition fallback, extraction's value-list lookups) targets the user's chosen repository
 * instead of a single globally configured one. Must be readable from any instance (ADR-004): the
 * confirm request can land on a different node than the upload. The repository id is not PII.
 * {@code stage} requires the process's ORIGINAL blob to exist.
 */
public interface ProcessRepositoryPort {

  void stage(String tenantId, String processId, String repositoryId);

  Optional<String> retrieve(String tenantId, String processId);
}
