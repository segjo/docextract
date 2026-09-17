package ch.adeon.apps.docextract.ingest.domain;

/**
 * Result of a synchronous ingest: the raw bytes are already written to the transient Postgres
 * blobstore ({@code originalBlobId}) and the original chunk-uploaded to the DMS as the later
 * finalization target ({@code dmsLocation}, write-only, ADR-008). The preview is produced
 * afterwards, on demand, from the ORIGINAL blob — not as part of this synchronous step.
 */
public record IngestedDocument(
    String processId, String dmsLocation, String originalBlobId, String mediaType) {}
