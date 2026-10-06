package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import java.util.Optional;

/**
 * Inbound port: embeds the text staged by the content module for a process and quarantines the
 * resulting vector as {@code PENDING} with a TTL (FR-3, ADR-006, ADR-012, C-7). Returns the vector
 * so callers (e.g. {@link FindSimilar}) can reuse it in the same run without re-embedding. Empty
 * when the staged text was blank (E-5).
 */
public interface StageEmbeddings {

  Optional<EmbeddingVector> stage(String processId);
}
