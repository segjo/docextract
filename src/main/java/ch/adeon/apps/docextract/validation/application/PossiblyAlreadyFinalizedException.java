package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.shared.error.ConflictStateException;
/**
 * The DMS refused the write after an earlier attempt with unknown outcome; the document is most
 * likely already stored (ADR-015). Must be checked in the DMS, not re-validated.
 */
public class PossiblyAlreadyFinalizedException extends ConflictStateException {

  public PossiblyAlreadyFinalizedException(String processId) {
    super(
        "Process "
            + processId
            + " was most likely already stored in the DMS by an earlier attempt; check the DMS",
        processId);
  }
}
