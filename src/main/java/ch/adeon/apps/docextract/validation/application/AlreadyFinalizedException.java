package ch.adeon.apps.docextract.validation.application;

/** Reject after the DMS write is not possible: the document is already final (ADR-015). */
public class AlreadyFinalizedException extends RuntimeException {

  public AlreadyFinalizedException(String processId) {
    super("Process " + processId + " is already finalized in the DMS");
  }
}
