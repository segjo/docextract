package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.shared.error.DocextractException;
import ch.adeon.apps.docextract.shared.error.ProblemCode;
/**
 * Thrown when the DMS rejects the write-back because the submitted attributes are invalid (DMS
 * write-back yields no {@code dmsDocumentId}). The saga must not proceed to corpus promotion in
 * this case — the client has to repeat the validation step with corrected attributes.
 */
public class InvalidAttributesException extends DocextractException {

  // The DMS' own reason may echo attribute values (PII), so it stays in the message (log only).
  private static final String DETAIL =
      "The DMS rejected the submitted attributes; repeat the validation step";

  public InvalidAttributesException(String processId) {
    this(processId, "DMS rejected attributes for process " + processId);
  }

  public InvalidAttributesException(String processId, String reason) {
    super(ProblemCode.INVALID_ATTRIBUTES, reason, DETAIL, processId, null);
  }
}
