package ch.adeon.apps.docextract.ingest.domain;

/** Outcome of validating a {@link DocumentUpload} against {@link Limits}. */
public record ValidationResult(boolean valid, String reason) {

  public static ValidationResult ok() {
    return new ValidationResult(true, null);
  }

  public static ValidationResult rejected(String reason) {
    return new ValidationResult(false, reason);
  }
}
