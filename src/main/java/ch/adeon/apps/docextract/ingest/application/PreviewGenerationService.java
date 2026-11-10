package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.port.BlobRef;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.ingest.port.PageRange;
import ch.adeon.apps.docextract.ingest.port.PreviewRenderPort;
import ch.adeon.apps.docextract.process.application.ProcessEvent;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessStep;
import ch.adeon.apps.docextract.process.application.StepStatus;
import ch.adeon.apps.docextract.shared.media.MediaType;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs off the request thread so upload latency isn't affected by Gotenberg rendering (ADR-008).
 * Native PDFs need no rendering: the ORIGINAL blob already is the preview source, so {@code PREVIEW
 * ready} is reported immediately with the original blob id.
 */
@Service
public class PreviewGenerationService implements GeneratePreview {

  private static final Logger log = LoggerFactory.getLogger(PreviewGenerationService.class);

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
    log.info("preview started processId={} originalBlobId={}", processId, originalBlobId);
    processEventPort.publish(
        tenantId, new ProcessEvent(processId, ProcessStep.PREVIEW, StepStatus.STARTED, null));
    if (mediaType.isPdf()) {
      log.info("preview skipped (native pdf) processId={}", processId);
      processEventPort.publish(
          tenantId,
          new ProcessEvent(
              processId, ProcessStep.PREVIEW, StepStatus.COMPLETED, originalBlobId.toString()));
      return CompletableFuture.completedFuture(Optional.empty());
    }
    try {
      BlobRef previewBlob = renderAndStorePreview(originalBlobId, processId, tenantId, userId);
      log.info("preview completed processId={} previewBlobId={}", processId, previewBlob.blobId());
      processEventPort.publish(
          tenantId,
          new ProcessEvent(
              processId,
              ProcessStep.PREVIEW,
              StepStatus.COMPLETED,
              previewBlob.blobId().toString()));
      return CompletableFuture.completedFuture(Optional.of(previewBlob.blobId()));
    } catch (PreviewRenderingException ex) {
      log.warn("preview failed processId={}", processId, ex);
      processEventPort.publish(
          tenantId, new ProcessEvent(processId, ProcessStep.PREVIEW, StepStatus.FAILED, null));
      return CompletableFuture.completedFuture(Optional.empty());
    }
  }

  private BlobRef renderAndStorePreview(
      UUID originalBlobId, String processId, String tenantId, String userId) {
    BlobRef original = documentBlobPort.describe(tenantId, originalBlobId);
    byte[] originalBytes =
        documentBlobPort.readRange(tenantId, originalBlobId, PageRange.full(original.sizeBytes()));
    byte[] pdf =
        previewRenderPort.renderPreview(
            originalBlobId.toString(), original.mediaType(), originalBytes);
    return documentBlobPort.store(
        BlobKind.PREVIEW, MediaType.PDF, pdf, processId, tenantId, userId);
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return mediaType.isPdf() || previewRenderPort.supports(mediaType);
  }

  @Override
  public java.util.Set<String> supportedFileExtensions() {
    java.util.Set<String> extensions = new java.util.LinkedHashSet<>();
    extensions.add(".pdf");
    extensions.addAll(previewRenderPort.supportedExtensions());
    return java.util.Set.copyOf(extensions);
  }
}
