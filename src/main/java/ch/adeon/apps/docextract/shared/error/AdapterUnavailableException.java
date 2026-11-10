package ch.adeon.apps.docextract.shared.error;

/**
 * An external system (DMS, renderer, webhook) is unreachable or failed. The message may carry
 * upstream details for the log; the caller only gets a fixed detail.
 */
public class AdapterUnavailableException extends DocextractException {

  public static final String DETAIL = "An upstream service failed; try again later";

  public AdapterUnavailableException(String message) {
    this(message, null);
  }

  public AdapterUnavailableException(String message, Throwable cause) {
    super(ProblemCode.ADAPTER_UNAVAILABLE, message, DETAIL, null, cause);
  }
}
