package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.shared.error.InvalidRequestException;
/** Thrown when an upload violates {@link ch.adeon.apps.docextract.ingest.domain.Limits}. */
public class UploadRejectedException extends InvalidRequestException {

  public UploadRejectedException(String reason) {
    super(reason);
  }
}
