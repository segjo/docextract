package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.validation.domain.RejectionCommand;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingDeletePort;
import org.springframework.stereotype.Service;

/**
 * Deletes the caller's process's quarantined {@code PENDING} embedding and its blobs (ADR-006, C-7,
 * T-2, § 6.2) — rejection never touches the DMS and is refused once the DMS write happened
 * (ADR-015). The ports only see the caller's tenant, so another tenant's process is the same as an
 * unknown one: 404.
 */
@Service
public class RejectSuggestionService implements RejectSuggestion {

  private final PendingEmbeddingDeletePort pendingEmbeddingDeletePort;
  private final DocumentBlobPort documentBlobPort;
  private final FinalizationStatePort finalizationStatePort;
  private final AuthContextPort authContextPort;
  private final AuditPort auditPort;
  private final ProcessEventPort processEventPort;

  public RejectSuggestionService(
      PendingEmbeddingDeletePort pendingEmbeddingDeletePort,
      DocumentBlobPort documentBlobPort,
      FinalizationStatePort finalizationStatePort,
      AuthContextPort authContextPort,
      AuditPort auditPort,
      ProcessEventPort processEventPort) {
    this.pendingEmbeddingDeletePort = pendingEmbeddingDeletePort;
    this.documentBlobPort = documentBlobPort;
    this.finalizationStatePort = finalizationStatePort;
    this.authContextPort = authContextPort;
    this.auditPort = auditPort;
    this.processEventPort = processEventPort;
  }

  @Override
  public void reject(RejectionCommand command) {
    String processId = command.processId();
    AuthContext caller = authContextPort.current();
    String tenantId = caller.tenantId();
    if (finalizationStatePort.find(tenantId, processId).isPresent()) {
      throw new AlreadyFinalizedException(processId);
    }
    if (!processEventPort.exists(tenantId, processId)) {
      throw new ProcessNotFoundException(processId);
    }
    pendingEmbeddingDeletePort.delete(tenantId, processId);
    documentBlobPort.deleteByProcess(tenantId, processId);
    auditPort.append(
        AuditEvent.byUser(
            tenantId,
            AuditChannel.UI,
            caller.agent(),
            caller.actorHash(),
            "validation.reject",
            null));
  }
}
