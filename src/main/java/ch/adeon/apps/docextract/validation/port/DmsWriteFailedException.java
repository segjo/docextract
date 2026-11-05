package ch.adeon.apps.docextract.validation.port;

/** Server error, timeout or network failure: the DMS write may or may not have happened. */
public class DmsWriteFailedException extends RuntimeException {

  public DmsWriteFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
