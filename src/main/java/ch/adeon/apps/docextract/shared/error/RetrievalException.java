package ch.adeon.apps.docextract.shared.error;

/** Wraps failures in the embed → stage-as-PENDING → similarity-search pipeline (FR-3, NfA-3). */
public class RetrievalException extends ProcessingFailedException {

  public RetrievalException(String message) {
    super(message, null);
  }

  public RetrievalException(String message, Throwable cause) {
    super(message, cause);
  }
}
