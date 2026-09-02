package com.adeon.docextract.ingest.application;

import com.adeon.docextract.ingest.domain.IngestCommand;
import com.adeon.docextract.ingest.domain.IngestedDocument;

public interface IngestDocument {
    IngestedDocument ingest(IngestCommand command);
}
