package ch.adeon.apps.docextract.extraction.domain;

/**
 * Wraps failures in the classify → suggest-attributes → valuelist-resolution pipeline (FR-4,
 * NfA-3).
 */
public class ExtractionException extends RuntimeException {

  public ExtractionException(String message) {
    super(message);
  }

  public ExtractionException(String message, Throwable cause) {
    super(message, cause);
  }
}
