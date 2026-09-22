package ch.adeon.apps.docextract.retrieval.domain;

/** Wraps failures in the embed → stage-as-PENDING → similarity-search pipeline (FR-3, NfA-3). */
public class RetrievalException extends RuntimeException {

  public RetrievalException(String message) {
    super(message);
  }

  public RetrievalException(String message, Throwable cause) {
    super(message, cause);
  }
}
