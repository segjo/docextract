package ch.adeon.apps.docextract.ingest.domain;

/**
 * Inclusive byte range for Range-fähiges reading from a blob, so the whole object never needs to be
 * materialized (ADR-008).
 */
public record PageRange(long start, long endInclusive) {

  public PageRange {
    if (start < 0 || endInclusive < start) {
      throw new IllegalArgumentException("invalid range [%d, %d]".formatted(start, endInclusive));
    }
  }

  public static PageRange full(long sizeBytes) {
    return new PageRange(0, Math.max(sizeBytes - 1, 0));
  }
}
