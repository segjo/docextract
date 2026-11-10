package ch.adeon.apps.docextract.validation.domain;

import java.time.Instant;

/**
 * PII-free record of a finished DMS write, so a retry never repeats it (ADR-015). {@code
 * firstFailedAt} starts the promotion retry limit.
 */
public record Finalization(
    String processId,
    String tenantId,
    FinalizationStatus status,
    String repositoryId,
    String dmsDocumentId,
    Instant firstFailedAt) {

  public Finalization(
      String processId,
      String tenantId,
      FinalizationStatus status,
      String repositoryId,
      String dmsDocumentId) {
    this(processId, tenantId, status, repositoryId, dmsDocumentId, null);
  }

  public Finalization withStatus(FinalizationStatus newStatus) {
    return new Finalization(
        processId, tenantId, newStatus, repositoryId, dmsDocumentId, firstFailedAt);
  }

  public Finalization withFirstFailedAt(Instant instant) {
    return new Finalization(processId, tenantId, status, repositoryId, dmsDocumentId, instant);
  }
}
