package com.adeon.docextract.retrieval.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.adeon.docextract.security.application.AuthContextPort;
import com.adeon.docextract.security.domain.AuthContext;
import com.adeon.docextract.retrieval.domain.SimilarTemplate;

@Service
public class FindSimilarService implements FindSimilar {

    private final AuthContextPort authContextPort;
    private final SimilaritySearchPort similaritySearchPort;

    public FindSimilarService(AuthContextPort authContextPort, SimilaritySearchPort similaritySearchPort) {
        this.authContextPort = authContextPort;
        this.similaritySearchPort = similaritySearchPort;
    }

    @Override
    public List<SimilarTemplate> find(String documentId, int topK) {
        AuthContext auth = authContextPort.current();
        return similaritySearchPort.search(auth.tenantId(), auth.aclRef(), documentId, topK);
    }
}
