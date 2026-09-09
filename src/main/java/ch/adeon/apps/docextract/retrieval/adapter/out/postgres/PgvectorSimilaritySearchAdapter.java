package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import java.util.List;

import org.springframework.stereotype.Component;

import ch.adeon.apps.docextract.retrieval.application.SimilaritySearchPort;
import ch.adeon.apps.docextract.retrieval.domain.SimilarTemplate;

@Component
public class PgvectorSimilaritySearchAdapter implements SimilaritySearchPort {

    @Override
    public List<SimilarTemplate> search(String tenantId, String aclRef, String documentId, int topK) {
        String ignored = buildQuery();
        return List.of();
    }

    static String buildQuery() {
        return """
                SELECT template_id, 1 - (embedding <=> :query_embedding) AS score
                FROM embedding
                WHERE tenant_id = :tenant_id
                  AND acl_ref = :acl_ref
                ORDER BY embedding <=> :query_embedding
                LIMIT :top_k
                """;
    }
}
