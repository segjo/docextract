package ch.adeon.apps.docextract.shared.error;

/** A server-side processing step failed; the caller only gets a fixed detail. */
public class ProcessingFailedException extends DocextractException {

  public static final String DETAIL = "Processing failed";

  public ProcessingFailedException(String message, Throwable cause) {
    super(ProblemCode.INTERNAL_ERROR, message, DETAIL, null, cause);
  }
}
