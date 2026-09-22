package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.PendingEmbedding;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalException;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.structuring.application.ChunkStagingPort;
import ch.adeon.apps.docextract.structuring.domain.DocChunk;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Embeds the chunks staged by structuring for a process and quarantines the resulting vectors as
 * non-retrievalfähige {@code PENDING} entries with a TTL (ADR-006, C-7, T-2). Chunk text itself is
 * never persisted — only the embeddings, keyed by {@code processId} and tenant/ACL scope.
 */
@Service
public class StagePendingEmbeddingsService implements StageEmbeddings {

  private static final Logger log = LoggerFactory.getLogger(StagePendingEmbeddingsService.class);

  private final ChunkStagingPort chunkStagingPort;
  private final AuthContextPort authContextPort;
  private final EmbeddingPort embeddingPort;
  private final PendingEmbeddingPort pendingEmbeddingPort;
  private final String embeddingModel;
  private final String extractionSource;

  public StagePendingEmbeddingsService(
      ChunkStagingPort chunkStagingPort,
      AuthContextPort authContextPort,
      EmbeddingPort embeddingPort,
      PendingEmbeddingPort pendingEmbeddingPort,
      @Value("${docextract.retrieval.embedding-model}") String embeddingModel,
      @Value("${docextract.structuring.mode}") String extractionSource) {
    this.chunkStagingPort = chunkStagingPort;
    this.authContextPort = authContextPort;
    this.embeddingPort = embeddingPort;
    this.pendingEmbeddingPort = pendingEmbeddingPort;
    this.embeddingModel = embeddingModel;
    this.extractionSource = extractionSource;
  }

  @Override
  public List<EmbeddingVector> stage(String processId) {
    List<DocChunk> chunks =
        chunkStagingPort
            .retrieve(processId)
            .orElseThrow(
                () -> new RetrievalException("no staged chunks found for process " + processId));
    if (chunks.isEmpty()) {
      // A blank/empty document is a valid outcome (E-5: "unbekannt" beats a spurious failure) —
      // it simply yields zero embeddings and, downstream, zero similarity results.
      return List.of();
    }

    AuthContext auth = authContextPort.current();
    List<String> texts = chunks.stream().map(DocChunk::text).toList();
    List<EmbeddingVector> vectors;
    try {
      vectors = embeddingPort.embed(texts);
    } catch (RuntimeException ex) {
      throw new RetrievalException("embedding chunks failed for process " + processId, ex);
    }
    if (vectors.size() != chunks.size()) {
      throw new RetrievalException(
          "embedding model returned %d vectors for %d chunks"
              .formatted(vectors.size(), chunks.size()));
    }

    List<PendingEmbedding> pending = new ArrayList<>(chunks.size());
    for (int i = 0; i < chunks.size(); i++) {
      pending.add(
          new PendingEmbedding(
              processId,
              chunks.get(i).index(),
              vectors.get(i),
              auth.tenantId(),
              auth.aclRef(),
              extractionSource,
              embeddingModel));
    }
    pendingEmbeddingPort.stage(pending);
    log.info("staged {} PENDING embeddings for processId={}", pending.size(), processId);
    return vectors;
  }
}
