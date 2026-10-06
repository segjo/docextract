package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.validation.domain.RejectionCommand;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingDeletePort;
import org.springframework.stereotype.Service;

/**
 * Deletes the process's quarantined {@code PENDING} embedding (ADR-006, C-7, T-2) — rejection never
 * touches the DMS.
 */
@Service
public class RejectSuggestionService implements RejectSuggestion {

  private final PendingEmbeddingDeletePort pendingEmbeddingDeletePort;
  private final AuditPort auditPort;

  public RejectSuggestionService(
      PendingEmbeddingDeletePort pendingEmbeddingDeletePort, AuditPort auditPort) {
    this.pendingEmbeddingDeletePort = pendingEmbeddingDeletePort;
    this.auditPort = auditPort;
  }

  @Override
  public void reject(RejectionCommand command) {
    pendingEmbeddingDeletePort.delete(command.processId());
    auditPort.append(new AuditEvent("validation.reject", "", 0));
  }
}
