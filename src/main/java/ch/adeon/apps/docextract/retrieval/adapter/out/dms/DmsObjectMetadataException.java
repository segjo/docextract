package ch.adeon.apps.docextract.retrieval.adapter.out.dms;

import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
/** Thrown when the d.velop DMS object-properties endpoint cannot be reached or errors. */
public class DmsObjectMetadataException extends AdapterUnavailableException {

  public DmsObjectMetadataException(String message, Throwable cause) {
    super(message, cause);
  }
}
