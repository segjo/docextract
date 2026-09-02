package com.adeon.docextract.retrieval.application;

import java.util.List;

import com.adeon.docextract.retrieval.domain.SimilarTemplate;

public interface FindSimilar {
    List<SimilarTemplate> find(String documentId, int topK);
}
