package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs off the request thread so upload latency isn't affected by Gotenberg rendering (ADR-008).
 * Native PDFs need no rendering: the ORIGINAL blob already is the preview source, so {@code PREVIEW
 * ready} is reported immediately with the original blob id.
 */
@Service
public class PreviewGenerationService implements GeneratePreview {

  private final DocumentBlobPort documentBlobPort;
  private final PreviewRenderPort previewRenderPort;
  private final ProcessEventPort processEventPort;

  public PreviewGenerationService(
      DocumentBlobPort documentBlobPort,
      PreviewRenderPort previewRenderPort,
      ProcessEventPort processEventPort) {
    this.documentBlobPort = documentBlobPort;
    this.previewRenderPort = previewRenderPort;
    this.processEventPort = processEventPort;
  }

  @Async
  @Override
  public CompletableFuture<Optional<UUID>> generate(
      String processId, UUID originalBlobId, MediaType mediaType, String tenantId, String userId) {
    processEventPort.publish(
        new ProcessEvent(processId, ProcessStep.PREVIEW, StepStatus.STARTED, null));
    if (mediaType.isPdf()) {
      processEventPort.publish(
          new ProcessEvent(
              processId, ProcessStep.PREVIEW, StepStatus.COMPLETED, originalBlobId.toString()));
      return CompletableFuture.completedFuture(Optional.empty());
    }
    try {
      BlobRef previewBlob = renderAndStorePreview(originalBlobId, processId, tenantId, userId);
      processEventPort.publish(
          new ProcessEvent(
              processId,
              ProcessStep.PREVIEW,
              StepStatus.COMPLETED,
              previewBlob.blobId().toString()));
      return CompletableFuture.completedFuture(Optional.of(previewBlob.blobId()));
    } catch (PreviewRenderingException ex) {
      processEventPort.publish(
          new ProcessEvent(processId, ProcessStep.PREVIEW, StepStatus.FAILED, null));
      return CompletableFuture.completedFuture(Optional.empty());
    }
  }

  private BlobRef renderAndStorePreview(
      UUID originalBlobId, String processId, String tenantId, String userId) {
    BlobRef original = documentBlobPort.describe(originalBlobId);
    byte[] originalBytes =
        documentBlobPort.readRange(originalBlobId, PageRange.full(original.sizeBytes()));
    byte[] pdf =
        previewRenderPort.renderPreview(
            originalBlobId.toString(), original.mediaType(), originalBytes);
    return documentBlobPort.store(
        BlobKind.PREVIEW, MediaType.PDF, pdf, processId, tenantId, userId);
  }
}
