package ch.adeon.apps.docextract.ingest.application;

/** Thrown when the d.velop DMS-Chunk-Store cannot be reached or rejects a chunk upload/read. */
public class DmsChunkUploadException extends RuntimeException {

  public DmsChunkUploadException(String message) {
    super(message);
  }

  public DmsChunkUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}
