package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.IngestCommand;
import ch.adeon.apps.docextract.ingest.domain.IngestedDocument;

public interface IngestDocument {
    IngestedDocument ingest(IngestCommand command);
}
