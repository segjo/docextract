package ch.adeon.apps.docextract.retrieval.adapter.out.dms;

/** Thrown when the d.velop DMS object-properties endpoint cannot be reached or errors. */
public class DmsObjectMetadataException extends RuntimeException {

  public DmsObjectMetadataException(String message, Throwable cause) {
    super(message, cause);
  }
}
