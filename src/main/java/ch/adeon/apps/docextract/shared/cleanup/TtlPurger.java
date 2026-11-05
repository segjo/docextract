package ch.adeon.apps.docextract.shared.cleanup;

/**
 * One module's share of the TTL cleanup (ADR-006/-008, C-7): each module deletes only its own
 * expired rows; {@link TtlCleanupJob} runs all of them.
 */
public interface TtlPurger {

  /** Returns the number of deleted entries (rows or processes, purger-specific). */
  int purgeExpired();
}
