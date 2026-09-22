package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import java.util.List;

/**
 * Inbound port: embeds the chunks staged by structuring for a process and quarantines the resulting
 * vectors as {@code PENDING} with a TTL (FR-3, ADR-006, C-7). Returns the vectors so callers (e.g.
 * {@link FindSimilar}) can reuse them in the same run without re-embedding.
 */
public interface StageEmbeddings {

  List<EmbeddingVector> stage(String processId);
}
