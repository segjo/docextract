package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort.StoredDocument;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import ch.adeon.apps.docextract.validation.domain.PendingEmbeddingCheck;
import ch.adeon.apps.docextract.validation.domain.RejectionCommand;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingCheckPort;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingDeletePort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RejectSuggestionServiceTest {

  private final PendingEmbeddingDeletePort deletePort = mock(PendingEmbeddingDeletePort.class);
  private final PendingEmbeddingCheckPort checkPort = mock(PendingEmbeddingCheckPort.class);
  private final DocumentBlobPort blobPort = mock(DocumentBlobPort.class);
  private final ProcessDocumentPort processDocumentPort = mock(ProcessDocumentPort.class);
  private final FinalizationStatePort finalizationStatePort = mock(FinalizationStatePort.class);
  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final RejectSuggestionService service =
      new RejectSuggestionService(
          deletePort,
          checkPort,
          blobPort,
          processDocumentPort,
          finalizationStatePort,
          authContextPort,
          mock(AuditPort.class));

  @BeforeEach
  void setUp() {
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "user-1", "User"));
    when(finalizationStatePort.find("p1")).thenReturn(Optional.empty());
    when(processDocumentPort.find("p1"))
        .thenReturn(Optional.of(new StoredDocument("/blob/1", "a.pdf", "tenant-a")));
    when(checkPort.find("p1"))
        .thenReturn(Optional.of(new PendingEmbeddingCheck("tenant-a", false)));
  }

  @Test
  void deletes_pending_embedding_and_blobs() {
    service.reject(new RejectionCommand("p1"));

    verify(deletePort).delete("p1");
    verify(blobPort).deleteByProcess("p1");
  }

  @Test
  void refuses_reject_in_finalized_index_pending() {
    when(finalizationStatePort.find("p1"))
        .thenReturn(
            Optional.of(
                new Finalization(
                    "p1", "tenant-a", FinalizationStatus.FINALIZED_INDEX_PENDING, "r", "d")));

    assertThatThrownBy(() -> service.reject(new RejectionCommand("p1")))
        .isInstanceOf(AlreadyFinalizedException.class);

    verify(deletePort, never()).delete(any());
    verify(blobPort, never()).deleteByProcess(any());
  }

  @Test
  void reject_of_another_tenant_s_process_is_not_found() {
    when(checkPort.find("p1"))
        .thenReturn(Optional.of(new PendingEmbeddingCheck("tenant-b", false)));

    assertThatThrownBy(() -> service.reject(new RejectionCommand("p1")))
        .isInstanceOf(ProcessNotFoundException.class);

    verify(deletePort, never()).delete(any());
  }
}
