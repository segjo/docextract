package ch.adeon.apps.docextract.retrieval.application;

/**
 * Outbound port for {@link Reindex}: the only writer allowed to move an {@code APPROVED} embedding
 * out of the active corpus without a validation-module consent decision — this is a maintenance
 * bookkeeping action (embedding model retired), not a rejection or promotion (ADR-012).
 */
public interface CorpusMaintenancePort {

  /** Flags every currently APPROVED row with the given {@code embedding_model} as STALE. */
  int markStale(String embeddingModel);

  /** Distinct {@code embedding_model} values currently present among APPROVED rows. */
  java.util.List<String> approvedModels();
}
