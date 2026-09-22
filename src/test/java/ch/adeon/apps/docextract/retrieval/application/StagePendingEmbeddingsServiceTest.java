package ch.adeon.apps.docextract.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.PendingEmbedding;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalException;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.structuring.application.ChunkStagingPort;
import ch.adeon.apps.docextract.structuring.domain.DocChunk;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StagePendingEmbeddingsServiceTest {

  private final ChunkStagingPort chunkStagingPort = mock(ChunkStagingPort.class);
  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final EmbeddingPort embeddingPort = mock(EmbeddingPort.class);
  private final PendingEmbeddingPort pendingEmbeddingPort = mock(PendingEmbeddingPort.class);
  private final StagePendingEmbeddingsService service =
      new StagePendingEmbeddingsService(
          chunkStagingPort,
          authContextPort,
          embeddingPort,
          pendingEmbeddingPort,
          "qwen3-embedding:0.6b",
          "pdfbox-fast-track");

  @Test
  void embeds_staged_chunks_and_stages_them_as_pending_with_tenant_and_acl() {
    List<DocChunk> chunks =
        List.of(new DocChunk(0, List.of("Heading"), "first"), new DocChunk(1, List.of(), "second"));
    when(chunkStagingPort.retrieve("p1")).thenReturn(Optional.of(chunks));
    when(authContextPort.current())
        .thenReturn(new AuthContext("tenant-a", "acl-a", "user", "User"));
    EmbeddingVector v0 = new EmbeddingVector(new float[] {1f, 0f});
    EmbeddingVector v1 = new EmbeddingVector(new float[] {0f, 1f});
    when(embeddingPort.embed(List.of("first", "second"))).thenReturn(List.of(v0, v1));

    List<EmbeddingVector> result = service.stage("p1");

    assertThat(result).containsExactly(v0, v1);
    verify(pendingEmbeddingPort)
        .stage(
            List.of(
                new PendingEmbedding(
                    "p1", 0, v0, "tenant-a", "acl-a", "pdfbox-fast-track", "qwen3-embedding:0.6b"),
                new PendingEmbedding(
                    "p1",
                    1,
                    v1,
                    "tenant-a",
                    "acl-a",
                    "pdfbox-fast-track",
                    "qwen3-embedding:0.6b")));
  }

  @Test
  void returns_no_embeddings_for_an_empty_document_without_calling_the_embedding_model() {
    when(chunkStagingPort.retrieve("p1")).thenReturn(Optional.of(List.of()));

    List<EmbeddingVector> result = service.stage("p1");

    assertThat(result).isEmpty();
    verify(embeddingPort, never()).embed(anyList());
    verify(pendingEmbeddingPort, never()).stage(anyList());
  }

  @Test
  void wraps_a_missing_chunk_staging_entry_in_a_retrieval_exception() {
    when(chunkStagingPort.retrieve("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.stage("missing")).isInstanceOf(RetrievalException.class);
  }

  @Test
  void wraps_an_embedding_model_failure_in_a_retrieval_exception() {
    List<DocChunk> chunks = List.of(new DocChunk(0, List.of(), "text"));
    when(chunkStagingPort.retrieve("p1")).thenReturn(Optional.of(chunks));
    when(authContextPort.current()).thenReturn(new AuthContext("t", "a", "u", "U"));
    when(embeddingPort.embed(anyList())).thenThrow(new EmbeddingException("boom"));

    assertThatThrownBy(() -> service.stage("p1")).isInstanceOf(RetrievalException.class);
    verify(pendingEmbeddingPort, never()).stage(anyList());
  }
}
