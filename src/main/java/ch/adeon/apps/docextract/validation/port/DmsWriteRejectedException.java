package ch.adeon.apps.docextract.validation.port;

/** The DMS rejected the write-back with a client error; {@code reason} is the DMS' own message. */
public class DmsWriteRejectedException extends RuntimeException {

  public DmsWriteRejectedException(String reason) {
    super(reason);
  }
}
