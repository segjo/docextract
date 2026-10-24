package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;
import ch.adeon.apps.docextract.validation.port.CorpusPromotionPort;
import ch.adeon.apps.docextract.validation.port.DmsWritePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Consent-gated finalization saga (ADR-003/-006, C-2/C-7): DMS write happens first, then the
 * process's staged {@code PENDING} embedding is promoted to {@code APPROVED} without recomputation.
 * The Vorlagen-Consent (FR-5) is recorded separately from the write-back consent and never implied
 * by it; actual template persistence/curation is out of scope for now (SPEC §2, Self-Learning-Loop
 * left open for the Ausbaustufe) — only the consent decision itself is audited here.
 */
@Service
public class ConfirmAndWriteBackService implements ConfirmAndWriteBack {

  private final DmsWritePort dmsWritePort;
  private final CorpusPromotionPort corpusPromotionPort;
  private final OutboundCredentialPort outboundCredentialPort;
  private final AuditPort auditPort;
  private final String sourceCategory;
  private final String filename;

  public ConfirmAndWriteBackService(
      DmsWritePort dmsWritePort,
      CorpusPromotionPort corpusPromotionPort,
      OutboundCredentialPort outboundCredentialPort,
      AppConfigPort appConfigPort,
      AuditPort auditPort,
      @Value("${docextract.ingest.dms.origin}") String origin,
      @Value("${docextract.validation.dms.source-category}") String sourceCategory,
      @Value("${docextract.validation.dms.filename:document.pdf}") String filename) {
    this.dmsWritePort = dmsWritePort;
    this.corpusPromotionPort = corpusPromotionPort;
    this.outboundCredentialPort = outboundCredentialPort;
    this.auditPort = auditPort;
    this.sourceCategory = sourceCategory;
    this.filename = filename;
  }

  @Override
  public ConfirmationResult confirm(ConfirmationCommand command) {
    if (!command.consentGiven()) {
      throw new ConsentRequiredException();
    }
    String dmsDocumentId =
        dmsWritePort.writeAttributes(
            command.repositoryId(),
            "",
            command.attributes(),
            filename,
            sourceCategory,
            outboundCredentialPort.current());
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
    corpusPromotionPort.promote(command.processId(), command.repositoryId(), dmsDocumentId);
    if (command.templateConsent()) {
      auditPort.append(new AuditEvent("validation.template-consent", "", 0));
    }
    return new ConfirmationResult(command.documentId(), true, command.templateConsent());
  }
}
