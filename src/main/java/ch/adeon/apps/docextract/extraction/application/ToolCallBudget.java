package ch.adeon.apps.docextract.extraction.application;

/**
 * The {@code docextract.extraction.max-tool-calls} limit of one extraction, shared by all tools
 * (ADR-014/-016). A model that keeps calling tools only wastes its own calls.
 */
final class ToolCallBudget {

  private final int max;
  private int used;

  ToolCallBudget(int max) {
    this.max = max;
  }

  int max() {
    return max;
  }

  /** Takes one call; {@code false} once the limit is reached. */
  boolean tryAcquire() {
    if (used >= max) {
      return false;
    }
    used++;
    return true;
  }
}
