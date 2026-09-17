package ch.adeon.apps.docextract.process.domain;

/** Pipeline steps reported over SSE for a {@code processId} (ADR-004). */
public enum ProcessStep {
  BLOB_STORED,
  DMS_UPLOADED,
  PREVIEW
}
