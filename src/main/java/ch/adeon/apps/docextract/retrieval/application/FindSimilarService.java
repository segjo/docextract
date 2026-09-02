package ch.adeon.apps.docextract.retrieval.application;

import java.util.List;

import org.springframework.stereotype.Service;

import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.retrieval.domain.SimilarTemplate;

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
