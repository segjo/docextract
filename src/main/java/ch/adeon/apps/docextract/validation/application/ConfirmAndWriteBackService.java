package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort.StoredDocument;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import ch.adeon.apps.docextract.validation.domain.PendingEmbeddingCheck;
import ch.adeon.apps.docextract.validation.port.CorpusPromotionPort;
import ch.adeon.apps.docextract.validation.port.DmsWritePort;
import ch.adeon.apps.docextract.validation.port.DmsWriteRejectedException;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingCheckPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Consent-gated finalization saga (ADR-003/-006/-015, C-2/C-7): DMS write happens first, then the
 * process's staged {@code PENDING} embedding is promoted to {@code APPROVED} without recomputation.
 * A failed promotion leaves {@code FINALIZED_INDEX_PENDING}; retries only repeat the promotion. The
 * DMS category is taken from the command; the configured default is only a fallback.
 */
@Service
public class ConfirmAndWriteBackService implements ConfirmAndWriteBack, RetryCorpusPromotion {

  private static final Logger log = LoggerFactory.getLogger(ConfirmAndWriteBackService.class);

  private final DmsWritePort dmsWritePort;
  private final CorpusPromotionPort corpusPromotionPort;
  private final OutboundCredentialPort outboundCredentialPort;
  private final AuthContextPort authContextPort;
  private final AuditPort auditPort;
  private final ProcessRepositoryPort processRepositoryPort;
  private final ProcessDocumentPort processDocumentPort;
  private final FinalizationStatePort finalizationStatePort;
  private final PendingEmbeddingCheckPort pendingEmbeddingCheckPort;
  private final DocumentBlobPort documentBlobPort;
  private final String defaultRepositoryId;
  private final Clock clock;
  private final Duration promotionRetryLimit;
  private final Duration promotionAlertAfter;

  public ConfirmAndWriteBackService(
      DmsWritePort dmsWritePort,
      CorpusPromotionPort corpusPromotionPort,
      OutboundCredentialPort outboundCredentialPort,
      AuthContextPort authContextPort,
      AuditPort auditPort,
      ProcessRepositoryPort processRepositoryPort,
      ProcessDocumentPort processDocumentPort,
      FinalizationStatePort finalizationStatePort,
      PendingEmbeddingCheckPort pendingEmbeddingCheckPort,
      DocumentBlobPort documentBlobPort,
      @Value("${docextract.ingest.dms.repository-id}") String defaultRepositoryId,
      Clock clock,
      @Value("${docextract.validation.promotion-retry-limit}") Duration promotionRetryLimit,
      @Value("${docextract.validation.promotion-alert-after}") Duration promotionAlertAfter) {
    this.dmsWritePort = dmsWritePort;
    this.corpusPromotionPort = corpusPromotionPort;
    this.outboundCredentialPort = outboundCredentialPort;
    this.authContextPort = authContextPort;
    this.auditPort = auditPort;
    this.processRepositoryPort = processRepositoryPort;
    this.processDocumentPort = processDocumentPort;
    this.finalizationStatePort = finalizationStatePort;
    this.pendingEmbeddingCheckPort = pendingEmbeddingCheckPort;
    this.documentBlobPort = documentBlobPort;
    this.defaultRepositoryId = defaultRepositoryId;
    this.clock = clock;
    this.promotionRetryLimit = promotionRetryLimit;
    this.promotionAlertAfter = promotionAlertAfter;
  }

  @Override
  public ConfirmationResult confirm(ConfirmationCommand command) {
    String processId = command.processId();
    String tenantId = authContextPort.current().tenantId();

    // Re-entry (client retry, other instance): the DMS write already happened, never repeat it.
    Optional<Finalization> existing = finalizationStatePort.find(processId);
    if (existing.isPresent() && !tenantId.equals(existing.get().tenantId())) {
      throw new ProcessNotFoundException(processId);
    }
    boolean earlierWriteOutcomeUnknown =
        existing.map(f -> f.status() == FinalizationStatus.DMS_WRITE_STARTED).orElse(false);
    if (existing.isPresent() && !earlierWriteOutcomeUnknown) {
      Finalization finalization = existing.get();
      FinalizationStatus status = finalization.status();
      if (status == FinalizationStatus.FINALIZED_INDEX_PENDING) {
        status = promote(finalization);
      }
      return new ConfirmationResult(
          finalization.repositoryId(), finalization.dmsDocumentId(), true, status);
    }

    // § 6.2 pre-check: process exists, belongs to the caller's tenant, quarantine still valid.
    StoredDocument staged =
        processDocumentPort
            .find(processId)
            .filter(document -> tenantId.equals(document.tenantId()))
            .orElseThrow(() -> new ProcessNotFoundException(processId));
    Optional<PendingEmbeddingCheck> pending = pendingEmbeddingCheckPort.find(processId);
    if (pending.isPresent() && !tenantId.equals(pending.get().tenantId())) {
      throw new ProcessNotFoundException(processId);
    }
    if (pending.isPresent() && pending.get().expired()) {
      throw new ProcessExpiredException(processId);
    }
    String repositoryId = processRepositoryPort.retrieve(processId).orElse(defaultRepositoryId);

    // Saga step 0 (ADR-015): marker before the non-idempotent DMS write, so a later DMS 400 after
    // an unknown outcome (crash, timeout) is reported as "possibly finalized", not as invalid
    // input.
    finalizationStatePort.save(
        new Finalization(
            processId, tenantId, FinalizationStatus.DMS_WRITE_STARTED, repositoryId, null));
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
      throw rejected(processId, earlierWriteOutcomeUnknown, ex.getMessage());
    }
    if (dmsDocumentId == null) {
      // A null dmsDocumentId means the DMS rejected the write-back due to invalid attributes.
      // Abort the saga before any audit/promotion so the client can repeat the validation step
      // with corrected attributes instead of silently landing in an inconsistent state.
      throw rejected(processId, earlierWriteOutcomeUnknown, null);
    }

    auditPort.append(new AuditEvent("validation.writeback", "", 0));
    // Saga step 2 (ADR-015): persist FINALIZED_INDEX_PENDING before promoting, so a crash or a
    // promotion failure leaves a state from which only the idempotent promotion is retried.
    Finalization finalization =
        new Finalization(
            processId,
            tenantId,
            FinalizationStatus.FINALIZED_INDEX_PENDING,
            repositoryId,
            dmsDocumentId);
    finalizationStatePort.save(finalization);
    FinalizationStatus status = promote(finalization);
    // The bytes now live in the DMS; the retry only needs the ids from the saga state.
    deleteBlobs(processId);
    return new ConfirmationResult(repositoryId, dmsDocumentId, true, status);
  }

  @Override
  public int retryPendingPromotions() {
    Instant now = clock.instant();
    int promoted = 0;
    int overdue = 0;
    for (Finalization finalization :
        finalizationStatePort.findByStatus(FinalizationStatus.FINALIZED_INDEX_PENDING)) {
      if (promote(finalization) == FinalizationStatus.COMPLETED) {
        deleteBlobs(finalization.processId());
        promoted++;
        continue;
      }
      Instant firstFailedAt =
          finalization.firstFailedAt() != null ? finalization.firstFailedAt() : now;
      Duration age = Duration.between(firstFailedAt, now);
      if (age.compareTo(promotionRetryLimit) >= 0) {
        giveUp(finalization.withFirstFailedAt(firstFailedAt));
      } else if (age.compareTo(promotionAlertAfter) >= 0) {
        overdue++;
      }
    }
    if (overdue > 0) {
      log.error(
          "{} corpus promotion(s) pending longer than {} (FINALIZED_INDEX_PENDING)",
          overdue,
          promotionAlertAfter);
    }
    return promoted;
  }

  /** Final failure (C-7): TTL cleanup may now delete embedding and blobs; the DMS keeps the doc. */
  private void giveUp(Finalization finalization) {
    finalizationStatePort.save(finalization.withStatus(FinalizationStatus.INDEX_FAILED));
    auditPort.append(new AuditEvent("validation.promotion-abandoned", "", 0));
    log.error(
        "Corpus promotion for process {} abandoned after {}, now INDEX_FAILED",
        finalization.processId(),
        promotionRetryLimit);
  }

  /** Returns the resulting status; FINALIZED_INDEX_PENDING stays if the promotion fails. */
  private FinalizationStatus promote(Finalization finalization) {
    try {
      corpusPromotionPort.promote(
          finalization.processId(), finalization.repositoryId(), finalization.dmsDocumentId());
    } catch (RuntimeException ex) {
      log.warn(
          "Corpus promotion failed for process {}, stays FINALIZED_INDEX_PENDING: {}",
          finalization.processId(),
          ex.getClass().getSimpleName());
      auditPort.append(new AuditEvent("validation.promotion-failed", "", 0));
      if (finalization.firstFailedAt() == null) {
        finalizationStatePort.save(finalization.withFirstFailedAt(clock.instant()));
      }
      return FinalizationStatus.FINALIZED_INDEX_PENDING;
    }
    finalizationStatePort.save(finalization.withStatus(FinalizationStatus.COMPLETED));
    auditPort.append(new AuditEvent("validation.promotion", "", 0));
    return FinalizationStatus.COMPLETED;
  }

  /**
   * After an earlier attempt with unknown outcome, a DMS rejection most likely means the document
   * is already stored (the DMS refuses a second write with 400) — keep the marker in that case.
   */
  private RuntimeException rejected(
      String processId, boolean earlierWriteOutcomeUnknown, String reason) {
    auditPort.append(new AuditEvent("validation.writeback-rejected", "", 0));
    if (earlierWriteOutcomeUnknown) {
      return new PossiblyAlreadyFinalizedException(processId);
    }
    finalizationStatePort.discardWriteStarted(processId);
    return reason == null
        ? new InvalidAttributesException(processId)
        : new InvalidAttributesException(processId, reason);
  }

  private void deleteBlobs(String processId) {
    try {
      documentBlobPort.deleteByProcess(processId);
    } catch (RuntimeException ex) {
      // Best effort: the TTL cleanup removes them later.
      log.warn("Deleting blobs of process {} failed: {}", processId, ex.getClass().getSimpleName());
    }
  }
}
