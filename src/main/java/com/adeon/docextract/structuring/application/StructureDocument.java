package com.adeon.docextract.structuring.application;

import com.adeon.docextract.structuring.domain.StructuredDocument;

public interface StructureDocument {
    StructuredDocument structure(String documentId);
}
