package ch.adeon.apps.docextract.ingest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.content.application.ExtractText;
import ch.adeon.apps.docextract.content.application.ProvideContent;
import ch.adeon.apps.docextract.extraction.application.ExtractAttributes;
import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.DmsLocation;
import ch.adeon.apps.docextract.ingest.port.BlobRef;
import ch.adeon.apps.docextract.ingest.port.DmsChunkUploadPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessEvent;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.process.application.ProcessStep;
import ch.adeon.apps.docextract.process.application.StepStatus;
import ch.adeon.apps.docextract.retrieval.application.FindSimilar;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.shared.media.MediaType;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class IngestDocumentServiceTest {

  private final DocumentBlobPort documentBlobPort = mock(DocumentBlobPort.class);
  private final DmsChunkUploadPort dmsChunkUploadPort = mock(DmsChunkUploadPort.class);
  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final OutboundCredentialPort outboundCredentialPort = mock(OutboundCredentialPort.class);
  private final AuditPort auditPort = mock(AuditPort.class);
  private final ProcessEventPort processEventPort = mock(ProcessEventPort.class);
  private final GeneratePreview generatePreview = mock(GeneratePreview.class);
  private final ExtractText extractText = mock(ExtractText.class);
  private final ProvideContent provideContent = mock(ProvideContent.class);
  private final FindSimilar findSimilar = mock(FindSimilar.class);
  private final ExtractAttributes extractAttributes = mock(ExtractAttributes.class);
  private final DocumentHashStagingPort documentHashStagingPort =
      mock(DocumentHashStagingPort.class);
  private final ProcessRepositoryPort processRepositoryPort = mock(ProcessRepositoryPort.class);
  private final ProcessDocumentPort processDocumentPort = mock(ProcessDocumentPort.class);
  private final IngestDocumentService service =
      new IngestDocumentService(
          documentBlobPort,
          dmsChunkUploadPort,
          authContextPort,
          outboundCredentialPort,
          auditPort,
          processEventPort,
          generatePreview,
          extractText,
          provideContent,
          findSimilar,
          extractAttributes,
          documentHashStagingPort,
          processRepositoryPort,
          processDocumentPort,
          1024L,
          5);

  @Test
  void writes_original_bytes_to_the_blobstore_tagged_with_the_current_tenant_and_user() {
    UUID blobId = UUID.randomUUID();
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "user-1", "User One"));
    when(generatePreview.supports(MediaType.PDF)).thenReturn(true);
    when(documentBlobPort.store(
            eq(BlobKind.ORIGINAL),
            eq(MediaType.PDF),
            any(),
            anyString(),
            eq("tenant-a"),
            eq("user-1")))
        .thenReturn(
            new BlobRef(blobId, BlobKind.ORIGINAL, MediaType.PDF, 1, "p", "tenant-a", "user-1"));
    when(dmsChunkUploadPort.upload(any(), eq(MediaType.PDF), any(), any()))
        .thenReturn(new DmsLocation("/dms/blob/chunk/1"));
    when(generatePreview.generate(any(), any(), eq(MediaType.PDF), eq("tenant-a"), eq("user-1")))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    IngestedDocument result =
        service.ingest(IngestCommand.of("invoice.pdf", "application/pdf", new byte[] {1}));

    assertThat(result.dmsLocation()).isEqualTo("/dms/blob/chunk/1");
    assertThat(result.originalBlobId()).isEqualTo(blobId.toString());
    assertThat(result.mediaType()).isEqualTo("application/pdf");
  }

  @Test
  void process_exists_for_its_tenant_as_soon_as_ingest_returns() {
    Set<String> published = ConcurrentHashMap.newKeySet();
    doAnswer(
            invocation -> {
              ProcessEvent event = invocation.getArgument(1);
              published.add(invocation.getArgument(0) + "/" + event.processId());
              return null;
            })
        .when(processEventPort)
        .publish(any(), any());
    when(processEventPort.exists(anyString(), anyString()))
        .thenAnswer(invocation ->
            published.contains(invocation.getArgument(0) + "/" + invocation.getArgument(1)));
    UUID blobId = UUID.randomUUID();
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "user-1", "User One"));
    when(generatePreview.supports(MediaType.PDF)).thenReturn(true);
    when(documentBlobPort.store(any(), any(), any(), anyString(), any(), any()))
        .thenReturn(
            new BlobRef(blobId, BlobKind.ORIGINAL, MediaType.PDF, 1, "p", "tenant-a", "user-1"));
    when(dmsChunkUploadPort.upload(any(), eq(MediaType.PDF), any(), any()))
        .thenReturn(new DmsLocation("/dms/blob/chunk/1"));
    // preview never completes: BLOB_STORED/PREVIEW must not be what makes the process known
    when(generatePreview.generate(any(), any(), any(), any(), any()))
        .thenReturn(new CompletableFuture<>());

    IngestedDocument result =
        service.ingest(IngestCommand.of("invoice.pdf", "application/pdf", new byte[] {1}));

    assertThat(processEventPort.exists("tenant-a", result.processId())).isTrue();
    assertThat(processEventPort.exists("tenant-b", result.processId())).isFalse();
    verify(processEventPort)
        .publish(
            eq("tenant-a"),
            eq(
                new ProcessEvent(
                    result.processId(), ProcessStep.DMS_UPLOADED, StepStatus.COMPLETED, null)));
  }

  @Test
  void upload_exceeding_the_limit_is_rejected_before_any_side_effect() {
    IngestCommand command = IngestCommand.of("big.pdf", "application/pdf", new byte[2048]);

    assertThatThrownBy(() -> service.ingest(command)).isInstanceOf(UploadRejectedException.class);

    verify(documentBlobPort, never()).store(any(), any(), any(), any(), any(), any());
    verify(dmsChunkUploadPort, never()).upload(any(), any(), any(), any());
  }

  @Test
  void deletes_the_original_and_preview_blob_when_the_dms_upload_fails() {
    UUID blobId = UUID.randomUUID();
    UUID previewBlobId = UUID.randomUUID();
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "user-1", "User One"));
    when(generatePreview.supports(MediaType.PDF)).thenReturn(true);
    when(documentBlobPort.store(
            eq(BlobKind.ORIGINAL),
            eq(MediaType.PDF),
            any(),
            anyString(),
            eq("tenant-a"),
            eq("user-1")))
        .thenReturn(
            new BlobRef(blobId, BlobKind.ORIGINAL, MediaType.PDF, 1, "p", "tenant-a", "user-1"));
    when(generatePreview.generate(any(), any(), eq(MediaType.PDF), eq("tenant-a"), eq("user-1")))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(previewBlobId)));
    when(dmsChunkUploadPort.upload(any(), eq(MediaType.PDF), any(), any()))
        .thenThrow(new DmsChunkUploadException("dms unreachable"));

    assertThatThrownBy(() ->
            service.ingest(IngestCommand.of("invoice.pdf", "application/pdf", new byte[] {1})))
        .isInstanceOf(DmsChunkUploadException.class);

    verify(documentBlobPort).delete("tenant-a", blobId);
    verify(documentBlobPort).delete("tenant-a", previewBlobId);
  }
}
