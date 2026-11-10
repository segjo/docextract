package ch.adeon.apps.docextract.retrieval.domain;

import java.util.List;

/**
 * Result of one {@link ch.adeon.apps.docextract.retrieval.port.EmbeddingPort#embed} call: {@code
 * vectors} are in the same order as the input texts; {@code tokenCount}, {@code provider} and
 * {@code modelId} are carried through for the audit trail (C-4, NfA-7).
 */
public record EmbeddingBatchResult(
    List<EmbeddingVector> vectors, int tokenCount, String provider, String modelId) {}
