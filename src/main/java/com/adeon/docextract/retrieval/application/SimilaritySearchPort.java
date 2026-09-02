package com.adeon.docextract.retrieval.application;

import java.util.List;

import com.adeon.docextract.retrieval.domain.SimilarTemplate;

public interface SimilaritySearchPort {
    List<SimilarTemplate> search(String tenantId, String aclRef, String documentId, int topK);
}
