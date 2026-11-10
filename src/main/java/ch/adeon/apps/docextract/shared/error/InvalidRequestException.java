package ch.adeon.apps.docextract.shared.error;

/** The request itself is unacceptable; {@code reason} is shown to the caller and must be PII-free. */
public class InvalidRequestException extends DocextractException {

  public InvalidRequestException(String reason) {
    super(ProblemCode.INVALID_REQUEST, reason, reason, null, null);
  }
}
