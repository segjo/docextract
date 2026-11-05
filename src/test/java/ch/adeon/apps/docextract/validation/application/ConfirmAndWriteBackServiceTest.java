package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort.StoredDocument;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import ch.adeon.apps.docextract.validation.domain.PendingEmbeddingCheck;
import ch.adeon.apps.docextract.validation.port.CorpusPromotionPort;
import ch.adeon.apps.docextract.validation.port.DmsWriteFailedException;
import ch.adeon.apps.docextract.validation.port.DmsWritePort;
import ch.adeon.apps.docextract.validation.port.DmsWriteRejectedException;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingCheckPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ConfirmAndWriteBackServiceTest {

  private static final Finalization INDEX_PENDING =
      new Finalization(
          "p1", "tenant-a", FinalizationStatus.FINALIZED_INDEX_PENDING, "repo-1", "doc-1");
  private static final Finalization WRITE_STARTED =
      new Finalization("p1", "tenant-a", FinalizationStatus.DMS_WRITE_STARTED, "repo-1", null);

  private DmsWritePort dmsWritePort;
  private CorpusPromotionPort corpusPromotionPort;
  private FinalizationStatePort finalizationStatePort;
  private ProcessDocumentPort processDocumentPort;
  private PendingEmbeddingCheckPort pendingEmbeddingCheckPort;
  private DocumentBlobPort documentBlobPort;
  private ConfirmAndWriteBackService service;

  @BeforeEach
  void setUp() {
    dmsWritePort = mock(DmsWritePort.class);
    corpusPromotionPort = mock(CorpusPromotionPort.class);
    finalizationStatePort = mock(FinalizationStatePort.class);
    processDocumentPort = mock(ProcessDocumentPort.class);
    pendingEmbeddingCheckPort = mock(PendingEmbeddingCheckPort.class);
    documentBlobPort = mock(DocumentBlobPort.class);
    OutboundCredentialPort outboundCredentialPort = mock(OutboundCredentialPort.class);
    AuthContextPort authContextPort = mock(AuthContextPort.class);
    ProcessRepositoryPort processRepositoryPort = mock(ProcessRepositoryPort.class);
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "user-1", "User"));
    when(processDocumentPort.find("p1"))
        .thenReturn(Optional.of(new StoredDocument("/blob/1", "a.pdf", "tenant-a")));
    when(pendingEmbeddingCheckPort.find("p1"))
        .thenReturn(Optional.of(new PendingEmbeddingCheck("tenant-a", false)));
    when(processRepositoryPort.retrieve("p1")).thenReturn(Optional.of("repo-1"));
    when(outboundCredentialPort.current())
        .thenReturn(new DvelopCredential("Authorization", "Bearer token", null));
    when(finalizationStatePort.find("p1")).thenReturn(Optional.empty());
    service =
        new ConfirmAndWriteBackService(
            dmsWritePort,
            corpusPromotionPort,
            outboundCredentialPort,
            authContextPort,
            mock(AuditPort.class),
            processRepositoryPort,
            processDocumentPort,
            finalizationStatePort,
            pendingEmbeddingCheckPort,
            documentBlobPort,
            "default-repo");
  }

  @Test
  void must_reject_and_not_promote_when_dms_rejects_attributes() {
    when(dmsWritePort.writeAttributes(
            eq("repo-1"), eq("/blob/1"), eq(Map.of()), eq("a.pdf"), eq("DREC"), any()))
        .thenReturn(null);

    assertThatThrownBy(() -> service.confirm(new ConfirmationCommand("p1", "DREC", Map.of())))
        .isInstanceOf(InvalidAttributesException.class);

    verify(corpusPromotionPort, never()).promote(eq("p1"), any(), any());
    verify(finalizationStatePort, never()).save(INDEX_PENDING);
    verify(finalizationStatePort).discardWriteStarted("p1");
  }

  // Restfenster: früherer Versuch mit unbekanntem Ausgang, DMS weist zweiten Write mit 400 ab.
  @Test
  void dms_rejection_after_unknown_outcome_is_reported_as_possibly_finalized() {
    when(finalizationStatePort.find("p1")).thenReturn(Optional.of(WRITE_STARTED));
    when(dmsWritePort.writeAttributes(any(), any(), any(), any(), any(), any()))
        .thenThrow(new DmsWriteRejectedException("already stored"));

    assertThatThrownBy(() -> service.confirm(new ConfirmationCommand("p1", "DREC", Map.of())))
        .isInstanceOf(PossiblyAlreadyFinalizedException.class);

    verify(finalizationStatePort, never()).discardWriteStarted(any());
    verify(corpusPromotionPort, never()).promote(any(), any(), any());
  }

  @Test
  void earlier_unknown_outcome_with_successful_retry_completes_the_saga() {
    when(finalizationStatePort.find("p1")).thenReturn(Optional.of(WRITE_STARTED));
    stubDmsWriteReturns("doc-1");

    ConfirmationResult result = service.confirm(new ConfirmationCommand("p1", "DREC", Map.of()));

    assertThat(result.indexStatus()).isEqualTo(FinalizationStatus.COMPLETED);
  }

  @Test
  void dms_failure_with_unknown_outcome_keeps_the_marker() {
    when(dmsWritePort.writeAttributes(any(), any(), any(), any(), any(), any()))
        .thenThrow(new DmsWriteFailedException("timeout", null));

    assertThatThrownBy(() -> service.confirm(new ConfirmationCommand("p1", "DREC", Map.of())))
        .isInstanceOf(DmsWriteFailedException.class);

    verify(finalizationStatePort).save(WRITE_STARTED);
    verify(finalizationStatePort, never()).discardWriteStarted(any());
  }

  @Test
  void writes_dms_before_promotion_and_completes_the_saga() {
    stubDmsWriteReturns("doc-1");

    ConfirmationResult result = service.confirm(new ConfirmationCommand("p1", "DREC", Map.of()));

    assertThat(result)
        .isEqualTo(new ConfirmationResult("repo-1", "doc-1", true, FinalizationStatus.COMPLETED));
    InOrder order =
        inOrder(dmsWritePort, finalizationStatePort, corpusPromotionPort, documentBlobPort);
    order.verify(finalizationStatePort).save(WRITE_STARTED);
    order.verify(dmsWritePort).writeAttributes(any(), any(), any(), any(), any(), any());
    order.verify(finalizationStatePort).save(INDEX_PENDING);
    order.verify(corpusPromotionPort).promote("p1", "repo-1", "doc-1");
    order
        .verify(finalizationStatePort)
        .save(INDEX_PENDING.withStatus(FinalizationStatus.COMPLETED));
    order.verify(documentBlobPort).deleteByProcess("p1");
  }

  // (a) Promotion-Fehler → FINALIZED_INDEX_PENDING, kein zweiter DMS-Write.
  @Test
  void promotion_failure_leaves_finalized_index_pending_without_second_dms_write() {
    stubDmsWriteReturns("doc-1");
    doThrow(new IllegalStateException("db down"))
        .when(corpusPromotionPort)
        .promote(anyString(), anyString(), anyString());

    ConfirmationResult result = service.confirm(new ConfirmationCommand("p1", "DREC", Map.of()));

    assertThat(result.writtenBack()).isTrue();
    assertThat(result.indexStatus()).isEqualTo(FinalizationStatus.FINALIZED_INDEX_PENDING);
    verify(finalizationStatePort).save(INDEX_PENDING);
    verify(finalizationStatePort, never())
        .save(INDEX_PENDING.withStatus(FinalizationStatus.COMPLETED));
    verify(dmsWritePort).writeAttributes(any(), any(), any(), any(), any(), any());
  }

  // (b) Retry im Zustand FINALIZED_INDEX_PENDING → nur Promotion, kein DMS-Write.
  @Test
  void repeated_confirm_in_finalized_index_pending_only_promotes() {
    when(finalizationStatePort.find("p1")).thenReturn(Optional.of(INDEX_PENDING));

    ConfirmationResult result = service.confirm(new ConfirmationCommand("p1", "DREC", Map.of()));

    assertThat(result)
        .isEqualTo(new ConfirmationResult("repo-1", "doc-1", true, FinalizationStatus.COMPLETED));
    verify(dmsWritePort, never()).writeAttributes(any(), any(), any(), any(), any(), any());
    verify(corpusPromotionPort).promote("p1", "repo-1", "doc-1");
    verify(finalizationStatePort).save(INDEX_PENDING.withStatus(FinalizationStatus.COMPLETED));
  }

  @Test
  void repeated_confirm_by_another_tenant_is_not_found() {
    when(finalizationStatePort.find("p1"))
        .thenReturn(
            Optional.of(
                new Finalization(
                    "p1", "tenant-b", FinalizationStatus.FINALIZED_INDEX_PENDING, "r", "d")));

    assertThatThrownBy(() -> service.confirm(new ConfirmationCommand("p1", "DREC", Map.of())))
        .isInstanceOf(ProcessNotFoundException.class);

    verify(corpusPromotionPort, never()).promote(any(), any(), any());
  }

  // § 6.2 pre-check: gleicher Tenant (NfA-4).
  @Test
  void confirm_of_another_tenant_s_process_never_writes_dms() {
    when(processDocumentPort.find("p1"))
        .thenReturn(Optional.of(new StoredDocument("/blob/1", "a.pdf", "tenant-b")));

    assertThatThrownBy(() -> service.confirm(new ConfirmationCommand("p1", "DREC", Map.of())))
        .isInstanceOf(ProcessNotFoundException.class);

    verify(dmsWritePort, never()).writeAttributes(any(), any(), any(), any(), any(), any());
  }

  @Test
  void pending_embedding_of_another_tenant_blocks_the_dms_write() {
    when(pendingEmbeddingCheckPort.find("p1"))
        .thenReturn(Optional.of(new PendingEmbeddingCheck("tenant-b", false)));

    assertThatThrownBy(() -> service.confirm(new ConfirmationCommand("p1", "DREC", Map.of())))
        .isInstanceOf(ProcessNotFoundException.class);

    verify(dmsWritePort, never()).writeAttributes(any(), any(), any(), any(), any(), any());
  }

  // § 6.2 pre-check: PENDING gültig (C-7).
  @Test
  void expired_pending_embedding_blocks_the_dms_write() {
    when(pendingEmbeddingCheckPort.find("p1"))
        .thenReturn(Optional.of(new PendingEmbeddingCheck("tenant-a", true)));

    assertThatThrownBy(() -> service.confirm(new ConfirmationCommand("p1", "DREC", Map.of())))
        .isInstanceOf(ProcessExpiredException.class);

    verify(dmsWritePort, never()).writeAttributes(any(), any(), any(), any(), any(), any());
  }

  // § 6.5: Retrieval übersprungen → kein PENDING, Freigabe bleibt möglich.
  @Test
  void confirm_without_pending_embedding_still_writes_dms() {
    when(pendingEmbeddingCheckPort.find("p1")).thenReturn(Optional.empty());
    stubDmsWriteReturns("doc-1");

    ConfirmationResult result = service.confirm(new ConfirmationCommand("p1", "DREC", Map.of()));

    assertThat(result.documentId()).isEqualTo("doc-1");
  }

  // (b) Hintergrund-Retry nach Instanzausfall: nur Promotion, kein DMS-Write.
  @Test
  void background_retry_only_promotes_and_never_writes_dms() {
    when(finalizationStatePort.findByStatus(FinalizationStatus.FINALIZED_INDEX_PENDING))
        .thenReturn(List.of(INDEX_PENDING));

    int promoted = service.retryPendingPromotions();

    assertThat(promoted).isEqualTo(1);
    verify(dmsWritePort, never()).writeAttributes(any(), any(), any(), any(), any(), any());
    verify(corpusPromotionPort).promote("p1", "repo-1", "doc-1");
    verify(finalizationStatePort).save(INDEX_PENDING.withStatus(FinalizationStatus.COMPLETED));
    verify(documentBlobPort).deleteByProcess("p1");
  }

  @Test
  void background_retry_keeps_finalized_index_pending_when_promotion_fails_again() {
    when(finalizationStatePort.findByStatus(FinalizationStatus.FINALIZED_INDEX_PENDING))
        .thenReturn(List.of(INDEX_PENDING));
    doThrow(new IllegalStateException("db down"))
        .when(corpusPromotionPort)
        .promote(anyString(), anyString(), anyString());

    int promoted = service.retryPendingPromotions();

    assertThat(promoted).isZero();
    verify(finalizationStatePort, never()).save(any());
    verify(dmsWritePort, never()).writeAttributes(any(), any(), any(), any(), any(), any());
  }

  @Test
  void repeated_confirm_after_completion_neither_writes_nor_promotes() {
    when(finalizationStatePort.find("p1"))
        .thenReturn(Optional.of(INDEX_PENDING.withStatus(FinalizationStatus.COMPLETED)));

    service.confirm(new ConfirmationCommand("p1", "DREC", Map.of()));

    verify(dmsWritePort, never()).writeAttributes(any(), any(), any(), any(), any(), any());
    verify(corpusPromotionPort, never()).promote(any(), any(), any());
  }

  private void stubDmsWriteReturns(String dmsDocumentId) {
    when(dmsWritePort.writeAttributes(
            eq("repo-1"), eq("/blob/1"), eq(Map.of()), eq("a.pdf"), eq("DREC"), any()))
        .thenReturn(dmsDocumentId);
  }
}
