package ch.adeon.apps.docextract.retrieval.adapter.out.dms;

import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
/** Thrown when the d.velop DMS object-definitions endpoint cannot be reached or errors. */
public class DmsObjectDefinitionException extends AdapterUnavailableException {

  public DmsObjectDefinitionException(String message, Throwable cause) {
    super(message, cause);
  }
}
