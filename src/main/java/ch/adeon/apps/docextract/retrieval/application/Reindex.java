package ch.adeon.apps.docextract.retrieval.application;

/**
 * Inbound port (ADR-012 §8.5, "Ein Wechsel des Embedding-Modells löst eine Re-Indexierung aus"):
 * flags {@code APPROVED} embeddings produced by a now-superseded {@code embedding_model} as {@code
 * STALE}. Raw document text is never persisted (ADR-006), so an automatic re-embedding of
 * historical documents is not possible from within this system alone — {@code STALE} rows are
 * excluded from retrieval (the mandatory {@code status = 'APPROVED'} predicate already covers this)
 * and surfaced so an operator can trigger re-ingestion of the affected documents.
 */
public interface Reindex {

  /**
   * Marks stale rows for the given, now-superseded model id; returns the number of rows flagged.
   */
  int reindex(String supersededEmbeddingModel);
}
