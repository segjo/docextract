package ch.adeon.apps.docextract.ingest.domain;

public record IngestCommand(String filename, byte[] content) {
}
