package ch.adeon.apps.docextract.ingest.application;

/** Preview bytes and the full source size needed for an HTTP range response. */
@SuppressWarnings("java:S6218")
public record PreviewChunk(byte[] content, long totalSize) {}
