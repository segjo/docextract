package ch.adeon.apps.docextract.validation.application;

/**
 * Thrown when the DMS rejects the write-back because the submitted attributes are invalid (DMS
 * write-back yields no {@code dmsDocumentId}). The saga must not proceed to corpus promotion in
 * this case — the client has to repeat the validation step with corrected attributes.
 */
public class InvalidAttributesException extends RuntimeException {

  public InvalidAttributesException(String processId) {
    super("DMS rejected attributes for process " + processId + " — repeat the validation step");
  }

  /** {@code reason} is the DMS' own message and becomes the exception message shown to the user. */
  public InvalidAttributesException(String processId, String reason) {
    super(reason);
  }
}
