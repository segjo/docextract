package ch.adeon.apps.docextract.shared.error;

/** The operation is not allowed in the current state of the resource (e.g. already finalized). */
public class ConflictStateException extends DocextractException {

  public ConflictStateException(String message, String processId) {
    super(ProblemCode.CONFLICT_STATE, message, message, processId, null);
  }
}
