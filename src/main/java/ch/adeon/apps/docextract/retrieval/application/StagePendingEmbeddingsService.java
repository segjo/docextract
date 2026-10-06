package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.content.application.TextStagingPort;
import ch.adeon.apps.docextract.content.domain.ExtractedText;
import ch.adeon.apps.docextract.ingest.application.DocumentHashStagingPort;
import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.PendingEmbedding;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalException;
import ch.adeon.apps.docextract.retrieval.port.EmbeddingPort;
import ch.adeon.apps.docextract.retrieval.port.PendingEmbeddingPort;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Embeds the text staged by the content module for a process into a single vector and quarantines
 * it as a non-retrievalfähige {@code PENDING} entry with a TTL (ADR-006, ADR-012, C-7, T-2). The
 * extracted text itself is never persisted — only the embedding, keyed by {@code processId} and
 * tenant scope. If the uploaded bytes match the hash of an already-{@code APPROVED} embedding for
 * the same tenant/model/source, that vector is reused instead of calling the embedding model again
 * (CHANGES.md duplicate detection).
 */
@Service
public class StagePendingEmbeddingsService implements StageEmbeddings {

  private static final Logger log = LoggerFactory.getLogger(StagePendingEmbeddingsService.class);

  private final TextStagingPort textStagingPort;
  private final AuthContextPort authContextPort;
  private final EmbeddingPort embeddingPort;
  private final PendingEmbeddingPort pendingEmbeddingPort;
  private final DocumentHashStagingPort documentHashStagingPort;
  private final String embeddingModel;
  private final String extractionSource;

  public StagePendingEmbeddingsService(
      TextStagingPort textStagingPort,
      AuthContextPort authContextPort,
      EmbeddingPort embeddingPort,
      PendingEmbeddingPort pendingEmbeddingPort,
      DocumentHashStagingPort documentHashStagingPort,
      @Value("${docextract.retrieval.embedding-model}") String embeddingModel,
      @Value("${docextract.adapters.text-extraction}") String extractionSource) {
    this.textStagingPort = textStagingPort;
    this.authContextPort = authContextPort;
    this.embeddingPort = embeddingPort;
    this.pendingEmbeddingPort = pendingEmbeddingPort;
    this.documentHashStagingPort = documentHashStagingPort;
    this.embeddingModel = embeddingModel;
    this.extractionSource = extractionSource;
  }

  @Override
  public Optional<EmbeddingVector> stage(String processId) {
    ExtractedText extractedText =
        textStagingPort
            .retrieve(processId)
            .orElseThrow(
                () -> new RetrievalException("no staged text found for process " + processId));
    if (extractedText.isBlank()) {
      // A blank/empty document is a valid outcome (E-5: "unbekannt" beats a spurious failure) —
      // it simply yields no embedding and, downstream, no similarity results.
      return Optional.empty();
    }

    AuthContext auth = authContextPort.current();
    String documentHash = documentHashStagingPort.retrieve(processId).orElse(null);
    Optional<EmbeddingVector> reused =
        pendingEmbeddingPort.findApprovedByHash(
            auth.tenantId(), documentHash, embeddingModel, extractionSource);

    EmbeddingVector vector;
    if (reused.isPresent()) {
      vector = reused.get();
      log.info(
          "skipping embedding model call, reusing APPROVED embedding for duplicate document,"
              + " processId={} documentHash={}",
          processId,
          documentHash);
    } else {
      List<EmbeddingVector> vectors;
      try {
        vectors = embeddingPort.embed(List.of(extractedText.text()));
      } catch (RuntimeException ex) {
        throw new RetrievalException("embedding text failed for process " + processId, ex);
      }
      if (vectors.size() != 1) {
        throw new RetrievalException(
            "embedding model returned %d vectors for 1 input text".formatted(vectors.size()));
      }
      vector = vectors.get(0);
    }

    pendingEmbeddingPort.stage(
        List.of(
            new PendingEmbedding(
                processId,
                vector,
                auth.tenantId(),
                extractionSource,
                embeddingModel,
                documentHash)));
    log.info("staged PENDING embedding for processId={}", processId);
    return Optional.of(vector);
  }
}
