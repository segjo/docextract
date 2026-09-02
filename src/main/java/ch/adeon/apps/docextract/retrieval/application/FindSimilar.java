package ch.adeon.apps.docextract.retrieval.application;

import java.util.List;

import ch.adeon.apps.docextract.retrieval.domain.SimilarTemplate;

public interface FindSimilar {
    List<SimilarTemplate> find(String documentId, int topK);
}
