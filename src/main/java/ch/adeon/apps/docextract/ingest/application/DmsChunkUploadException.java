package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
/** Thrown when the d.velop DMS-Chunk-Store cannot be reached or rejects a chunk upload/read. */
public class DmsChunkUploadException extends AdapterUnavailableException {

  public DmsChunkUploadException(String message) {
    super(message);
  }

  public DmsChunkUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}
