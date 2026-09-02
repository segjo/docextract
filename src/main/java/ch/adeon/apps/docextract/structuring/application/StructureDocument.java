package ch.adeon.apps.docextract.structuring.application;

import ch.adeon.apps.docextract.structuring.domain.StructuredDocument;

public interface StructureDocument {
    StructuredDocument structure(String documentId);
}
