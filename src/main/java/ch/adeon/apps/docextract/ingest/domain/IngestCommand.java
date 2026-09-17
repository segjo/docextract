package ch.adeon.apps.docextract.ingest.domain;

public record IngestCommand(String filename, MediaType mediaType, byte[] content) {

  /** Builds a command, guessing the media type from the filename when no content type is known. */
  public static IngestCommand of(String filename, String contentType, byte[] content) {
    MediaType mediaType =
        (contentType == null || contentType.isBlank())
            ? MediaType.guessFromFilename(filename)
            : MediaType.of(contentType);
    return new IngestCommand(filename, mediaType, content);
  }
}
