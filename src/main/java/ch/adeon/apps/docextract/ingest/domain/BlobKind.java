package ch.adeon.apps.docextract.ingest.domain;

/** Distinguishes the two blob roles held in the transient Postgres blobstore (ADR-008). */
public enum BlobKind {
  ORIGINAL,
  PREVIEW
}
