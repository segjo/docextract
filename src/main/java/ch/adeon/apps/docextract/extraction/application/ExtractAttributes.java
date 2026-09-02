package ch.adeon.apps.docextract.extraction.application;

import ch.adeon.apps.docextract.extraction.domain.AttributeSuggestion;

public interface ExtractAttributes {
    AttributeSuggestion extract(String documentId);
}
