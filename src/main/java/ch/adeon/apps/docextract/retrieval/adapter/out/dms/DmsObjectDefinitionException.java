package ch.adeon.apps.docextract.retrieval.adapter.out.dms;

/** Thrown when the d.velop DMS object-definitions endpoint cannot be reached or errors. */
public class DmsObjectDefinitionException extends RuntimeException {

  public DmsObjectDefinitionException(String message, Throwable cause) {
    super(message, cause);
  }
}
