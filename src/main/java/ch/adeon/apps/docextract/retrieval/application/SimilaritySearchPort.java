package ch.adeon.apps.docextract.retrieval.application;

import java.util.List;

import ch.adeon.apps.docextract.retrieval.domain.SimilarTemplate;

public interface SimilaritySearchPort {
    List<SimilarTemplate> search(String tenantId, String aclRef, String documentId, int topK);
}
