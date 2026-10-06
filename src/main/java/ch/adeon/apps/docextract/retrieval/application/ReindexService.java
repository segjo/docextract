package ch.adeon.apps.docextract.retrieval.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Flags {@code APPROVED} embeddings produced by a superseded {@code embedding_model} as {@code
 * STALE} (ADR-012). Run automatically once at startup, comparing the currently configured model
 * against every distinct model already present in the corpus — anything else is superseded.
 */
@Service
public class ReindexService implements Reindex {

  private static final Logger log = LoggerFactory.getLogger(ReindexService.class);

  private final CorpusMaintenancePort corpusMaintenancePort;
  private final String currentEmbeddingModel;

  public ReindexService(
      CorpusMaintenancePort corpusMaintenancePort,
      @Value("${docextract.retrieval.embedding-model}") String currentEmbeddingModel) {
    this.corpusMaintenancePort = corpusMaintenancePort;
    this.currentEmbeddingModel = currentEmbeddingModel;
  }

  @Override
  public int reindex(String supersededEmbeddingModel) {
    int flagged = corpusMaintenancePort.markStale(supersededEmbeddingModel);
    if (flagged > 0) {
      log.warn(
          "flagged {} APPROVED embeddings as STALE (superseded embedding_model={}); affected"
              + " documents need re-ingestion to rejoin the retrieval corpus",
          flagged,
          supersededEmbeddingModel);
    }
    return flagged;
  }

  @org.springframework.context.event.EventListener(
      org.springframework.boot.context.event.ApplicationReadyEvent.class)
  void reindexOnStartup() {
    corpusMaintenancePort.approvedModels().stream()
        .filter(model -> !model.equals(currentEmbeddingModel))
        .forEach(this::reindex);
  }
}
