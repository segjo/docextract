package ch.adeon.apps.docextract.validation.domain;

/** Saga state after the DMS write (ARCHITECTURE § 6.2, ADR-015). */
public enum FinalizationStatus {
  /** Marker before the non-idempotent DMS write; outcome unknown until the write returns. */
  DMS_WRITE_STARTED,
  /** DMS finalized, corpus promotion still outstanding; protected from TTL cleanup. */
  FINALIZED_INDEX_PENDING,
  COMPLETED
}
