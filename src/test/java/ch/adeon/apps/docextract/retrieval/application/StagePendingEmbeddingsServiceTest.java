package ch.adeon.apps.docextract.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.content.application.TextStagingPort;
import ch.adeon.apps.docextract.content.port.ExtractedText;
import ch.adeon.apps.docextract.ingest.application.DocumentHashStagingPort;
import ch.adeon.apps.docextract.retrieval.domain.EmbeddingBatchResult;
import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.PendingEmbedding;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalException;
import ch.adeon.apps.docextract.retrieval.port.EmbeddingPort;
import ch.adeon.apps.docextract.retrieval.port.PendingEmbeddingPort;
import ch.adeon.apps.docextract.shared.hash.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StagePendingEmbeddingsServiceTest {

  private final TextStagingPort textStagingPort = mock(TextStagingPort.class);
  private final EmbeddingPort embeddingPort = mock(EmbeddingPort.class);
  private final PendingEmbeddingPort pendingEmbeddingPort = mock(PendingEmbeddingPort.class);
  private final DocumentHashStagingPort documentHashStagingPort =
      mock(DocumentHashStagingPort.class);
  private final AuditPort auditPort = mock(AuditPort.class);
  private final StagePendingEmbeddingsService service =
      new StagePendingEmbeddingsService(
          textStagingPort,
          embeddingPort,
          pendingEmbeddingPort,
          documentHashStagingPort,
          auditPort,
          "qwen3-embedding:0.6b",
          "pdfbox");

  @Test
  void embeds_the_staged_text_and_stages_it_as_pending_with_the_tenant() {
    ExtractedText text = new ExtractedText("first second", 2);
    when(textStagingPort.retrieve("p1")).thenReturn(Optional.of(text));
    when(documentHashStagingPort.retrieve("p1")).thenReturn(Optional.of("hash-1"));
    EmbeddingVector v0 = new EmbeddingVector(new float[] {1f, 0f});
    when(embeddingPort.embed(List.of("first second")))
        .thenReturn(new EmbeddingBatchResult(List.of(v0), 7, "openai-compatible", "qwen3-embedding:0.6b"));
    when(pendingEmbeddingPort.findApprovedByHash(
            "tenant-a", "hash-1", "qwen3-embedding:0.6b", "pdfbox"))
        .thenReturn(Optional.empty());

    Optional<EmbeddingVector> result = service.stage("tenant-a", "p1");

    assertThat(result).contains(v0);
    verify(pendingEmbeddingPort)
        .stage(
            List.of(
                new PendingEmbedding(
                    "p1", v0, "tenant-a", "pdfbox", "qwen3-embedding:0.6b", "hash-1")));
    verify(auditPort)
        .append(
            AuditEvent.llm(
                "tenant-a",
                "retrieval.embed",
                Sha256.hex("first second".getBytes(StandardCharsets.UTF_8)),
                7,
                "openai-compatible",
                "qwen3-embedding:0.6b"));
  }

  @Test
  void reuses_an_already_approved_embedding_for_a_byte_identical_duplicate() {
    ExtractedText text = new ExtractedText("first second", 2);
    when(textStagingPort.retrieve("p1")).thenReturn(Optional.of(text));
    when(documentHashStagingPort.retrieve("p1")).thenReturn(Optional.of("hash-1"));
    EmbeddingVector existing = new EmbeddingVector(new float[] {0.5f, 0.5f});
    when(pendingEmbeddingPort.findApprovedByHash(
            "tenant-a", "hash-1", "qwen3-embedding:0.6b", "pdfbox"))
        .thenReturn(Optional.of(existing));

    Optional<EmbeddingVector> result = service.stage("tenant-a", "p1");

    assertThat(result).contains(existing);
    verify(embeddingPort, never()).embed(anyList());
    verify(pendingEmbeddingPort)
        .stage(
            List.of(
                new PendingEmbedding(
                    "p1", existing, "tenant-a", "pdfbox", "qwen3-embedding:0.6b", "hash-1")));
    // A reused, already-audited embedding is not re-embedded, so no new audit entry is needed.
    verifyNoInteractions(auditPort);
  }

  @Test
  void returns_no_embedding_for_a_blank_document_without_calling_the_embedding_model() {
    when(textStagingPort.retrieve("p1")).thenReturn(Optional.of(ExtractedText.empty()));

    Optional<EmbeddingVector> result = service.stage("tenant-a", "p1");

    assertThat(result).isEmpty();
    verify(embeddingPort, never()).embed(anyList());
    verify(pendingEmbeddingPort, never()).stage(anyList());
  }

  @Test
  void wraps_a_missing_text_staging_entry_in_a_retrieval_exception() {
    when(textStagingPort.retrieve("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.stage("tenant-a", "missing"))
        .isInstanceOf(RetrievalException.class);
  }

  @Test
  void wraps_an_embedding_model_failure_in_a_retrieval_exception() {
    when(textStagingPort.retrieve("p1")).thenReturn(Optional.of(new ExtractedText("text", 1)));
    when(documentHashStagingPort.retrieve("p1")).thenReturn(Optional.empty());
    when(pendingEmbeddingPort.findApprovedByHash("t", null, "qwen3-embedding:0.6b", "pdfbox"))
        .thenReturn(Optional.empty());
    when(embeddingPort.embed(anyList())).thenThrow(new EmbeddingException("boom"));

    assertThatThrownBy(() -> service.stage("t", "p1")).isInstanceOf(RetrievalException.class);
    verify(pendingEmbeddingPort, never()).stage(anyList());
  }
}
