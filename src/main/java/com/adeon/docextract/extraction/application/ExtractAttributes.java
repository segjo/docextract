package com.adeon.docextract.extraction.application;

import com.adeon.docextract.extraction.domain.AttributeSuggestion;

public interface ExtractAttributes {
    AttributeSuggestion extract(String documentId);
}
