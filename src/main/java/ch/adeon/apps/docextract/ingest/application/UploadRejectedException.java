package ch.adeon.apps.docextract.ingest.application;

/** Thrown when an upload violates {@link ch.adeon.apps.docextract.ingest.domain.Limits}. */
public class UploadRejectedException extends RuntimeException {

  public UploadRejectedException(String reason) {
    super(reason);
  }
}
