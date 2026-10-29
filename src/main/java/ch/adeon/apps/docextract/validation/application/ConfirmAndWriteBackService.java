package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort.StoredDocument;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;
import ch.adeon.apps.docextract.validation.port.CorpusPromotionPort;
import ch.adeon.apps.docextract.validation.port.DmsWritePort;
import ch.adeon.apps.docextract.validation.port.DmsWriteRejectedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Consent-gated finalization saga (ADR-003/-006, C-2/C-7): DMS write happens first, then the
 * process's staged {@code PENDING} embedding is promoted to {@code APPROVED} without recomputation.
 * The DMS category is taken from the command; the configured default is only a fallback.
 */
@Service
public class ConfirmAndWriteBackService implements ConfirmAndWriteBack {

  private final DmsWritePort dmsWritePort;
  private final CorpusPromotionPort corpusPromotionPort;
  private final OutboundCredentialPort outboundCredentialPort;
  private final AuditPort auditPort;
  private final ProcessRepositoryPort processRepositoryPort;
  private final ProcessDocumentPort processDocumentPort;
  private final String defaultRepositoryId;

  public ConfirmAndWriteBackService(
      DmsWritePort dmsWritePort,
      CorpusPromotionPort corpusPromotionPort,
      OutboundCredentialPort outboundCredentialPort,
      AppConfigPort appConfigPort,
      AuditPort auditPort,
      ProcessRepositoryPort processRepositoryPort,
      ProcessDocumentPort processDocumentPort,
      @Value("${docextract.ingest.dms.repository-id}") String defaultRepositoryId) {
    this.dmsWritePort = dmsWritePort;
    this.corpusPromotionPort = corpusPromotionPort;
    this.outboundCredentialPort = outboundCredentialPort;
    this.auditPort = auditPort;
    this.processRepositoryPort = processRepositoryPort;
    this.processDocumentPort = processDocumentPort;
    this.defaultRepositoryId = defaultRepositoryId;
  }

  @Override
  public ConfirmationResult confirm(ConfirmationCommand command) {
    StoredDocument staged =
        processDocumentPort
            .find(command.processId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No stored DMS location for process " + command.processId()));
    String repositoryId = processRepositoryPort.retrieve(command.processId()).orElse(defaultRepositoryId);

    String dmsDocumentId;
    try {
      dmsDocumentId =
          dmsWritePort.writeAttributes(
              repositoryId,
              staged.dmsLocation(),
              command.attributes(),
              staged.filename(),
              command.dmsSourceCategory(),
              outboundCredentialPort.current());
    } catch (DmsWriteRejectedException ex) {
      auditPort.append(new AuditEvent("validation.writeback-rejected", "", 0));
      throw new InvalidAttributesException(command.processId(), ex.getMessage());
    }
    if (dmsDocumentId == null) {
      // A null dmsDocumentId means the DMS rejected the write-back due to invalid attributes.
      // Abort the saga before any audit/promotion so the client can repeat the validation step
      // with corrected attributes instead of silently landing in an inconsistent state.
      auditPort.append(new AuditEvent("validation.writeback-rejected", "", 0));
      throw new InvalidAttributesException(command.processId());
    }

    auditPort.append(new AuditEvent("validation.writeback", "", 0));
    // Saga step 2: promotion is idempotent, so a retry after a crash here is safe (ADR-006
    // FINALIZED_INDEX_PENDING recovery — the full lease/retry mechanics are handled by the
    // process module, not repeated here).
    corpusPromotionPort.promote(command.processId(), repositoryId, dmsDocumentId);
    return new ConfirmationResult(repositoryId, dmsDocumentId, true);
  }
}
