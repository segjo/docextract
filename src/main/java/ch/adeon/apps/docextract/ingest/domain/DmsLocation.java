package ch.adeon.apps.docextract.ingest.domain;

/**
 * Location of a chunk in the write-only d.velop DMS-Chunk-Store, as returned via the {@code
 * Location} header of {@code POST /r/{repositoryId}/blob/chunk}. The upload cannot be read back
 * before finalization (ADR-008), so this reference is kept purely as the later finalization target,
 * never as a source to read the bytes back from.
 */
public record DmsLocation(String uri) {

  public DmsLocation {
    if (uri == null || uri.isBlank()) {
      throw new IllegalArgumentException("location uri must not be blank");
    }
  }
}
