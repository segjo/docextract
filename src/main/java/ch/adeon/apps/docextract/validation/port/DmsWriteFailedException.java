package ch.adeon.apps.docextract.validation.port;

import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
/** Server error, timeout or network failure: the DMS write may or may not have happened. */
public class DmsWriteFailedException extends AdapterUnavailableException {

  public DmsWriteFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
