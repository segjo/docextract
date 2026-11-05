package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.PendingEmbeddingCheck;
import ch.adeon.apps.docextract.validation.domain.RejectionCommand;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingCheckPort;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingDeletePort;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Deletes the process's quarantined {@code PENDING} embedding and its blobs (ADR-006, C-7, T-2, §
 * 6.2) — rejection never touches the DMS and is refused once the DMS write happened (ADR-015).
 */
@Service
public class RejectSuggestionService implements RejectSuggestion {

  private final PendingEmbeddingDeletePort pendingEmbeddingDeletePort;
  private final PendingEmbeddingCheckPort pendingEmbeddingCheckPort;
  private final DocumentBlobPort documentBlobPort;
  private final ProcessDocumentPort processDocumentPort;
  private final FinalizationStatePort finalizationStatePort;
  private final AuthContextPort authContextPort;
  private final AuditPort auditPort;

  public RejectSuggestionService(
      PendingEmbeddingDeletePort pendingEmbeddingDeletePort,
      PendingEmbeddingCheckPort pendingEmbeddingCheckPort,
      DocumentBlobPort documentBlobPort,
      ProcessDocumentPort processDocumentPort,
      FinalizationStatePort finalizationStatePort,
      AuthContextPort authContextPort,
      AuditPort auditPort) {
    this.pendingEmbeddingDeletePort = pendingEmbeddingDeletePort;
    this.pendingEmbeddingCheckPort = pendingEmbeddingCheckPort;
    this.documentBlobPort = documentBlobPort;
    this.processDocumentPort = processDocumentPort;
    this.finalizationStatePort = finalizationStatePort;
    this.authContextPort = authContextPort;
    this.auditPort = auditPort;
  }

  @Override
  public void reject(RejectionCommand command) {
    String processId = command.processId();
    String tenantId = authContextPort.current().tenantId();
    Optional<Finalization> finalization = finalizationStatePort.find(processId);
    boolean foreign =
        Stream.of(
                finalization.map(Finalization::tenantId),
                processDocumentPort.find(processId).map(document -> document.tenantId()),
                pendingEmbeddingCheckPort.find(processId).map(PendingEmbeddingCheck::tenantId))
            .flatMap(Optional::stream)
            .anyMatch(owner -> !owner.equals(tenantId));
    if (foreign) {
      throw new ProcessNotFoundException(processId);
    }
    if (finalization.isPresent()) {
      throw new AlreadyFinalizedException(processId);
    }
    pendingEmbeddingDeletePort.delete(processId);
    documentBlobPort.deleteByProcess(processId);
    auditPort.append(new AuditEvent("validation.reject", "", 0));
  }
}
