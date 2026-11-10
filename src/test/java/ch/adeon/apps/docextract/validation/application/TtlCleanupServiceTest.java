package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingDeletePort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TtlCleanupServiceTest {

  private final PendingEmbeddingDeletePort embeddingPort = mock(PendingEmbeddingDeletePort.class);
  private final DocumentBlobPort blobPort = mock(DocumentBlobPort.class);
  private final FinalizationStatePort finalizationStatePort = mock(FinalizationStatePort.class);
  private final TtlCleanupService service =
      new TtlCleanupService(embeddingPort, blobPort, finalizationStatePort, mock(AuditPort.class));

  @BeforeEach
  void setUp() {
    when(finalizationStatePort.findByStatus(FinalizationStatus.FINALIZED_INDEX_PENDING))
        .thenReturn(
            List.of(
                new Finalization(
                    "p-pending",
                    "tenant-a",
                    FinalizationStatus.FINALIZED_INDEX_PENDING,
                    "repo-1",
                    "doc-1")));
  }

  // (c) TTL-Cleanup überspringt FINALIZED_INDEX_PENDING.
  @Test
  void skips_embeddings_of_processes_in_finalized_index_pending() {
    when(embeddingPort.findExpiredProcessIds()).thenReturn(List.of("p-pending", "p-orphan"));

    int deleted = service.purgeExpired();

    assertThat(deleted).isEqualTo(1);
    verify(embeddingPort, never()).deleteExpired("p-pending");
    verify(embeddingPort).deleteExpired("p-orphan");
  }

  @Test
  void cleans_up_processes_whose_promotion_was_abandoned() {
    // INDEX_FAILED is not FINALIZED_INDEX_PENDING, so it is not returned as protected
    when(embeddingPort.findExpiredProcessIds()).thenReturn(List.of("p-failed"));

    service.purgeExpired();

    verify(embeddingPort).deleteExpired("p-failed");
  }

  @Test
  void skips_blobs_of_processes_in_finalized_index_pending() {
    when(blobPort.findExpiredProcessIds()).thenReturn(List.of("p-pending", "p-orphan"));

    int deleted = service.purgeExpired();

    assertThat(deleted).isEqualTo(1);
    verify(blobPort, never()).deleteExpired("p-pending");
    verify(blobPort).deleteExpired("p-orphan");
  }
}
