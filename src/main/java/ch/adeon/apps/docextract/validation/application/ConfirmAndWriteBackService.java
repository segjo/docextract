package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;
import ch.adeon.apps.docextract.validation.port.CorpusPromotionPort;
import ch.adeon.apps.docextract.validation.port.DmsWritePort;
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
  private final AuditPort auditPort;

  public ConfirmAndWriteBackService(
      DmsWritePort dmsWritePort, CorpusPromotionPort corpusPromotionPort, AuditPort auditPort) {
    this.dmsWritePort = dmsWritePort;
    this.corpusPromotionPort = corpusPromotionPort;
    this.auditPort = auditPort;
  }

  @Override
  public ConfirmationResult confirm(ConfirmationCommand command) {
    if (!command.consentGiven()) {
      throw new ConsentRequiredException();
    }
    dmsWritePort.writeAttributes(command.documentId(), command.attributes());
    auditPort.append(new AuditEvent("validation.writeback", "", 0));
    // Saga step 2: promotion is idempotent, so a retry after a crash here is safe (ADR-006
    // FINALIZED_INDEX_PENDING recovery — the full lease/retry mechanics are handled by the
    // process module, not repeated here).
    corpusPromotionPort.promote(command.processId(), command.repositoryId(), command.documentId());
    if (command.templateConsent()) {
      auditPort.append(new AuditEvent("validation.template-consent", "", 0));
    }
    return new ConfirmationResult(command.documentId(), true, command.templateConsent());
  }
}
