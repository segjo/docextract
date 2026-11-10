package ch.adeon.apps.docextract.ingest.domain;

/** Outcome of validating a {@link DocumentUpload} against {@link Limits}. */
public record ValidationResult(boolean valid, String reason, boolean sizeExceeded) {

  public static ValidationResult ok() {
    return new ValidationResult(true, null, false);
  }

  public static ValidationResult rejected(String reason) {
    return new ValidationResult(false, reason, false);
  }

  public static ValidationResult tooLarge(String reason) {
    return new ValidationResult(false, reason, true);
  }
}
