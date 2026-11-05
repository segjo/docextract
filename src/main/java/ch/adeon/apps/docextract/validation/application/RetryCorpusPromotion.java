package ch.adeon.apps.docextract.validation.application;

/**
 * Inbound port: retries the idempotent corpus promotion for every process in {@code
 * FINALIZED_INDEX_PENDING} (ARCHITECTURE § 6.2, ADR-015) — never repeats the DMS write.
 */
public interface RetryCorpusPromotion {

  /** Returns the number of processes successfully promoted in this run. */
  int retryPendingPromotions();
}
