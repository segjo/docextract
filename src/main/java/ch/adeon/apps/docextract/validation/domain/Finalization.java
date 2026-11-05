package ch.adeon.apps.docextract.validation.domain;

/** PII-free record of a finished DMS write, so a retry never repeats it (ADR-015). */
public record Finalization(
    String processId,
    String tenantId,
    FinalizationStatus status,
    String repositoryId,
    String dmsDocumentId) {

  public Finalization withStatus(FinalizationStatus newStatus) {
    return new Finalization(processId, tenantId, newStatus, repositoryId, dmsDocumentId);
  }
}
