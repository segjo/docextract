package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.shared.error.DocextractException;
import ch.adeon.apps.docextract.shared.error.ProblemCode;
/** The process's quarantined embedding has passed its TTL; confirm is no longer allowed (C-7). */
public class ProcessExpiredException extends DocextractException {

  public ProcessExpiredException(String processId) {
    super(
        ProblemCode.PROCESS_EXPIRED,
        "Process " + processId + " has expired",
        "Process " + processId + " has expired; upload the document again",
        processId,
        null);
  }
}
