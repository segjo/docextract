package ch.adeon.apps.docextract.ingest.domain;

/**
 * {@code repositoryId} is the d.velop repository the user picked in the import mask ({@code GET
 * /repo/repositories}, see {@link DmsRepository}) — may be {@code null}/blank for callers that
 * don't offer a choice (e.g. the MCP gateway), in which case ingest falls back to the configured
 * default repository.
 */
public record IngestCommand(String filename, MediaType mediaType, byte[] content, String repositoryId) {

  /** Builds a command, guessing the media type from the filename when no content type is known. */
  public static IngestCommand of(
      String filename, String contentType, byte[] content, String repositoryId) {
    MediaType mediaType =
        (contentType == null || contentType.isBlank())
            ? MediaType.guessFromFilename(filename)
            : MediaType.of(contentType);
    return new IngestCommand(filename, mediaType, content, repositoryId);
  }

  /** Convenience overload for callers that don't offer a repository choice (e.g. MCP gateway). */
  public static IngestCommand of(String filename, String contentType, byte[] content) {
    return of(filename, contentType, content, null);
  }
}
