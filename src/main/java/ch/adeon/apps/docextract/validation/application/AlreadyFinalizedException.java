package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.shared.error.ConflictStateException;
/** Reject after the DMS write is not possible: the document is already final (ADR-015). */
public class AlreadyFinalizedException extends ConflictStateException {

  public AlreadyFinalizedException(String processId) {
    super("Process " + processId + " is already finalized in the DMS", processId);
  }
}
