package ch.adeon.apps.docextract.retrieval.adapter.out.dms;

/** Thrown when the d.velop DMS valuelist webhook cannot be reached or errors. */
public class ValueListWebhookException extends RuntimeException {

  public ValueListWebhookException(String message, Throwable cause) {
    super(message, cause);
  }
}
