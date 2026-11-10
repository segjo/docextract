package ch.adeon.apps.docextract.retrieval.adapter.out.dms;

import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
/** Thrown when the d.velop DMS valuelist webhook cannot be reached or errors. */
public class ValueListWebhookException extends AdapterUnavailableException {

  public ValueListWebhookException(String message, Throwable cause) {
    super(message, cause);
  }
}
