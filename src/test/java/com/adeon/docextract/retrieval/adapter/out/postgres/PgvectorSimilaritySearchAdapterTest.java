package com.adeon.docextract.retrieval.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PgvectorSimilaritySearchAdapterTest {

    @Test
    void query_contains_acl_prefilter_columns() {
        String sql = PgvectorSimilaritySearchAdapter.buildQuery();

        assertThat(sql).contains("WHERE tenant_id = :tenant_id");
        assertThat(sql).contains("AND acl_ref = :acl_ref");
    }
}
