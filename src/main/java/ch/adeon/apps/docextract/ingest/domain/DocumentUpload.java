package ch.adeon.apps.docextract.ingest.domain;

/** Raw upload as received from the caller (UI iframe or MCP tool), before any DMS interaction. */
public record DocumentUpload(String filename, MediaType mediaType, byte[] content) {

  public long sizeInBytes() {
    return content == null ? 0 : content.length;
  }
}
