package ch.adeon.apps.docextract.shared.error;

/**
 * Wraps failures in the classify → suggest-attributes → valuelist-resolution pipeline (FR-4,
 * NfA-3).
 */
public class ExtractionException extends ProcessingFailedException {

  public ExtractionException(String message) {
    super(message, null);
  }

  public ExtractionException(String message, Throwable cause) {
    super(message, cause);
  }
}
