package com.adeon.docextract.ingest.domain;

public record IngestCommand(String filename, byte[] content) {
}
