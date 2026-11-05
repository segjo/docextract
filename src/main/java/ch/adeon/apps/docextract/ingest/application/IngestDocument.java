package ch.adeon.apps.docextract.ingest.application;


public interface IngestDocument {
    IngestedDocument ingest(IngestCommand command);
}
