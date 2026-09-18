package ch.adeon.apps.docextract.structuring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.ingest.application.DocumentBlobPort;
import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import ch.adeon.apps.docextract.structuring.domain.StructuredContent;
import ch.adeon.apps.docextract.structuring.domain.StructuredDocument;
import ch.adeon.apps.docextract.structuring.domain.StructuringCommand;
import ch.adeon.apps.docextract.structuring.domain.StructuringException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StructureDocumentServiceTest {

  private final DocumentBlobPort documentBlobPort = mock(DocumentBlobPort.class);
  private final StructuringPort structuringPort = mock(StructuringPort.class);
  private final ChunkStagingPort chunkStagingPort = mock(ChunkStagingPort.class);
  private final ProcessEventPort processEventPort = mock(ProcessEventPort.class);
  private final StructureDocumentService service =
      new StructureDocumentService(
          documentBlobPort, structuringPort, chunkStagingPort, processEventPort, 512);

  @Test
  void reads_the_original_blob_chunks_it_and_stages_the_chunks_for_downstream_steps() {
    UUID blobId = UUID.randomUUID();
    when(documentBlobPort.describe(blobId))
        .thenReturn(new BlobRef(blobId, BlobKind.ORIGINAL, MediaType.PDF, 10, "p1", "t", "u"));
    when(documentBlobPort.readRange(eq(blobId), any())).thenReturn(new byte[10]);
    when(structuringPort.convert(any(), eq(MediaType.PDF)))
        .thenReturn(new StructuredContent("# Heading\n\nSome text."));

    StructuredDocument result =
        service.structure(new StructuringCommand("p1", blobId, MediaType.PDF, "t", "u"));

    assertThat(result.processId()).isEqualTo("p1");
    assertThat(result.chunks()).hasSize(1);
    assertThat(result.chunks().get(0).headingPath()).containsExactly("Heading");

    verify(chunkStagingPort).stage(eq("p1"), eq(result.chunks()));
    verify(processEventPort)
        .publish(new ProcessEvent("p1", ProcessStep.STRUCTURED, StepStatus.STARTED, null));
    verify(processEventPort)
        .publish(new ProcessEvent("p1", ProcessStep.STRUCTURED, StepStatus.COMPLETED, null));
  }

  @Test
  void publishes_a_failed_event_and_wraps_the_error_when_conversion_fails() {
    UUID blobId = UUID.randomUUID();
    when(documentBlobPort.describe(blobId))
        .thenReturn(new BlobRef(blobId, BlobKind.ORIGINAL, MediaType.PDF, 10, "p1", "t", "u"));
    when(documentBlobPort.readRange(eq(blobId), any())).thenReturn(new byte[10]);
    when(structuringPort.convert(any(), eq(MediaType.PDF)))
        .thenThrow(new StructuringException("boom"));

    assertThatThrownBy(
            () -> service.structure(new StructuringCommand("p1", blobId, MediaType.PDF, "t", "u")))
        .isInstanceOf(StructuringException.class);

    verify(processEventPort)
        .publish(new ProcessEvent("p1", ProcessStep.STRUCTURED, StepStatus.FAILED, null));
    verify(chunkStagingPort, org.mockito.Mockito.never()).stage(any(), any());
  }
}
