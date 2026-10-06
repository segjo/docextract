package ch.adeon.apps.docextract.content.domain;

/**
 * Plain-text input for FR-3 (similarity search): the first {@code wordCount} words of the document,
 * with no structure or table reconstruction (ADR-012). Held only in-memory (ADR-006) — never
 * persisted. {@code text} is blank for an empty/too-short source document (E-5: reported, not
 * silently swallowed).
 */
public record ExtractedText(String text, int wordCount) {

  public static ExtractedText empty() {
    return new ExtractedText("", 0);
  }

  public boolean isBlank() {
    return text == null || text.isBlank();
  }
}
