package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.ActorKind;
import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import ch.adeon.apps.docextract.validation.domain.RejectionCommand;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingDeletePort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RejectSuggestionServiceTest {

  private final PendingEmbeddingDeletePort deletePort = mock(PendingEmbeddingDeletePort.class);
  private final DocumentBlobPort blobPort = mock(DocumentBlobPort.class);
  private final FinalizationStatePort finalizationStatePort = mock(FinalizationStatePort.class);
  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final AuditPort auditPort = mock(AuditPort.class);
  private final ProcessEventPort processEventPort = mock(ProcessEventPort.class);
  private final RejectSuggestionService service =
      new RejectSuggestionService(
          deletePort,
          blobPort,
          finalizationStatePort,
          authContextPort,
          auditPort,
          processEventPort);

  @BeforeEach
  void setUp() {
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "user-1", "User"));
    when(finalizationStatePort.find("tenant-a", "p1")).thenReturn(Optional.empty());
    when(processEventPort.exists("tenant-a", "p1")).thenReturn(true);
    when(processEventPort.exists("tenant-b", "p1")).thenReturn(true);
  }

  @Test
  void unknown_or_foreign_process_is_not_found_and_nothing_is_deleted() {
    when(processEventPort.exists("tenant-a", "p1")).thenReturn(false);

    assertThatThrownBy(() -> service.reject(new RejectionCommand("p1")))
        .isInstanceOf(ProcessNotFoundException.class);

    verify(deletePort, never()).delete(any(), any());
    verify(blobPort, never()).deleteByProcess(any(), any());
    verify(auditPort, never()).append(any());
  }

  @Test
  void audits_the_rejection_of_a_person_as_ui_human() {
    service.reject(new RejectionCommand("p1"));

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPort).append(captor.capture());
    assertThat(captor.getValue().channel()).isEqualTo(AuditChannel.UI);
    assertThat(captor.getValue().actorKind()).isEqualTo(ActorKind.HUMAN);
    assertThat(captor.getValue().tenantId()).isEqualTo("tenant-a");
    assertThat(captor.getValue().eventType()).isEqualTo("validation.reject");
  }

  @Test
  void deletes_pending_embedding_and_blobs_of_the_callers_tenant() {
    service.reject(new RejectionCommand("p1"));

    verify(deletePort).delete("tenant-a", "p1");
    verify(blobPort).deleteByProcess("tenant-a", "p1");
  }

  @Test
  void refuses_reject_in_finalized_index_pending() {
    when(finalizationStatePort.find("tenant-a", "p1"))
        .thenReturn(
            Optional.of(
                new Finalization(
                    "p1", "tenant-a", FinalizationStatus.FINALIZED_INDEX_PENDING, "r", "d")));

    assertThatThrownBy(() -> service.reject(new RejectionCommand("p1")))
        .isInstanceOf(AlreadyFinalizedException.class);

    verify(deletePort, never()).delete(any(), any());
    verify(blobPort, never()).deleteByProcess(any(), any());
  }

  // NfA-4: the ports are only ever asked for the caller's tenant, so another tenant's data is
  // unreachable (PgPendingEmbeddingDeleteAdapterTest/PgDocumentBlobAdapterTest prove the filter).
  @Test
  void reject_never_addresses_another_tenant() {
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-b", "user-9", "Other"));

    service.reject(new RejectionCommand("p1"));

    verify(deletePort).delete("tenant-b", "p1");
    verify(blobPort).deleteByProcess("tenant-b", "p1");
    verify(deletePort, never()).delete(org.mockito.ArgumentMatchers.eq("tenant-a"), any());
    verify(blobPort, never()).deleteByProcess(org.mockito.ArgumentMatchers.eq("tenant-a"), any());
  }
}
