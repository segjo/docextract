package ch.adeon.apps.docextract.validation.adapter.in.scheduler;

import ch.adeon.apps.docextract.validation.application.RetryCorpusPromotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic promotion retry for processes stuck in {@code FINALIZED_INDEX_PENDING} (ADR-015). The
 * promotion is idempotent, so concurrent runs on several cluster nodes are harmless.
 */
@Component
public class FinalizationRecoveryScheduler {

  private static final Logger log = LoggerFactory.getLogger(FinalizationRecoveryScheduler.class);

  private final RetryCorpusPromotion retryCorpusPromotion;

  public FinalizationRecoveryScheduler(RetryCorpusPromotion retryCorpusPromotion) {
    this.retryCorpusPromotion = retryCorpusPromotion;
  }

  @Scheduled(
      fixedDelayString = "${docextract.validation.recovery-interval}",
      initialDelayString = "${docextract.validation.recovery-interval}")
  public void run() {
    int promoted = retryCorpusPromotion.retryPendingPromotions();
    if (promoted > 0) {
      log.info("Promotion retry: {} process(es) promoted", promoted);
    }
  }
}
