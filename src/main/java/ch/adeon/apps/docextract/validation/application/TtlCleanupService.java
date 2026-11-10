package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.shared.cleanup.TtlPurger;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingDeletePort;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * TTL cleanup of the quarantine data of a process — expired {@code PENDING} embeddings and blobs
 * (C-7, ADR-006/-008, same cleanup for both). Never touches a process in {@code
 * FINALIZED_INDEX_PENDING} (ADR-015, CONTEXT § 4).
 */
@Service
public class TtlCleanupService implements TtlPurger {

  private final PendingEmbeddingDeletePort pendingEmbeddingDeletePort;
  private final DocumentBlobPort documentBlobPort;
  private final FinalizationStatePort finalizationStatePort;
  private final AuditPort auditPort;

  public TtlCleanupService(
      PendingEmbeddingDeletePort pendingEmbeddingDeletePort,
      DocumentBlobPort documentBlobPort,
      FinalizationStatePort finalizationStatePort,
      AuditPort auditPort) {
    this.pendingEmbeddingDeletePort = pendingEmbeddingDeletePort;
    this.documentBlobPort = documentBlobPort;
    this.finalizationStatePort = finalizationStatePort;
    this.auditPort = auditPort;
  }

  @Override
  public int purgeExpired() {
    // System-wide job over all tenants, hence no tenant filter on these ports.
    Set<String> indexPending =
        finalizationStatePort.findByStatus(FinalizationStatus.FINALIZED_INDEX_PENDING).stream()
            .map(Finalization::processId)
            .collect(Collectors.toSet());
    int deleted =
        purge(
            pendingEmbeddingDeletePort.findExpiredProcessIds(),
            indexPending,
            pendingEmbeddingDeletePort::deleteExpired);
    deleted +=
        purge(
            documentBlobPort.findExpiredProcessIds(),
            indexPending,
            documentBlobPort::deleteExpired);
    return deleted;
  }

  private int purge(
      Iterable<String> expiredProcessIds,
      Set<String> indexPending,
      Consumer<String> deleteExpired) {
    int deleted = 0;
    for (String processId : expiredProcessIds) {
      if (indexPending.contains(processId)) {
        continue;
      }
      deleteExpired.accept(processId);
      auditPort.append(AuditEvent.ttlCleanup());
      deleted++;
    }
    return deleted;
  }
}
