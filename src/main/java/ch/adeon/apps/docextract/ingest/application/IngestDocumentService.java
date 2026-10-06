package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.content.application.ExtractText;
import ch.adeon.apps.docextract.content.application.ProvideContent;
import ch.adeon.apps.docextract.content.domain.ContentCommand;
import ch.adeon.apps.docextract.content.domain.ContentException;
import ch.adeon.apps.docextract.extraction.application.ExtractAttributes;
import ch.adeon.apps.docextract.extraction.domain.ExtractionException;
import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.DmsLocation;
import ch.adeon.apps.docextract.ingest.domain.DocumentUpload;
import ch.adeon.apps.docextract.ingest.domain.IngestCommand;
import ch.adeon.apps.docextract.ingest.domain.IngestedDocument;
import ch.adeon.apps.docextract.ingest.domain.Limits;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.ValidationResult;
import ch.adeon.apps.docextract.ingest.port.DmsChunkUploadPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import ch.adeon.apps.docextract.retrieval.application.FindSimilar;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalException;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import ch.adeon.apps.docextract.shared.hash.Sha256;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Synchronous handover point before the caller receives {@code 202 Accepted + processId} (FR-1):
 * the raw bytes are written to the transient Postgres blobstore as the ORIGINAL blob, and
 * concurrently the original is chunk-uploaded to the DMS (write-only) to obtain the later
 * finalization {@code Location} (ADR-008). Preview rendering (see {@link GeneratePreview}) is
 * chained off the blob store and likewise runs off the request thread, with progress reported per
 * step over SSE; {@link StreamPreview} remains the on-demand fallback if a client asks before the
 * async render finished. If the DMS upload fails, both the already-stored ORIGINAL blob and any
 * PREVIEW blob produced meanwhile are deleted so no orphaned bytes are left behind (C-7).
 */
@Service
public class IngestDocumentService implements IngestDocument {

  private static final Logger log = LoggerFactory.getLogger(IngestDocumentService.class);

  private final DocumentBlobPort documentBlobPort;
  private final DmsChunkUploadPort dmsChunkUploadPort;
  private final AuthContextPort authContextPort;
  private final OutboundCredentialPort outboundCredentialPort;
  private final AuditPort auditPort;
  private final ProcessEventPort processEventPort;
  private final GeneratePreview generatePreview;
  private final ExtractText extractText;
  private final ProvideContent provideContent;
  private final FindSimilar findSimilar;
  private final ExtractAttributes extractAttributes;
  private final DocumentHashStagingPort documentHashStagingPort;
  private final int retrievalTopK;
  private final Limits limits;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  public IngestDocumentService(
      DocumentBlobPort documentBlobPort,
      DmsChunkUploadPort dmsChunkUploadPort,
      AuthContextPort authContextPort,
      OutboundCredentialPort outboundCredentialPort,
      AuditPort auditPort,
      ProcessEventPort processEventPort,
      GeneratePreview generatePreview,
      ExtractText extractText,
      ProvideContent provideContent,
      FindSimilar findSimilar,
      ExtractAttributes extractAttributes,
      DocumentHashStagingPort documentHashStagingPort,
      @Value("${docextract.ingest.max-file-size-bytes:26214400}") long maxFileSizeBytes,
      @Value("${docextract.retrieval.top-k:5}") int retrievalTopK) {
    this.documentBlobPort = documentBlobPort;
    this.dmsChunkUploadPort = dmsChunkUploadPort;
    this.authContextPort = authContextPort;
    this.outboundCredentialPort = outboundCredentialPort;
    this.auditPort = auditPort;
    this.processEventPort = processEventPort;
    this.generatePreview = generatePreview;
    this.extractText = extractText;
    this.provideContent = provideContent;
    this.findSimilar = findSimilar;
    this.extractAttributes = extractAttributes;
    this.documentHashStagingPort = documentHashStagingPort;
    this.retrievalTopK = retrievalTopK;
    this.limits = new Limits(maxFileSizeBytes);
  }

  @PreDestroy
  void shutdown() {
    executor.shutdown();
  }

  @Override
  public IngestedDocument ingest(IngestCommand command) {
    DocumentUpload upload =
        new DocumentUpload(command.filename(), command.mediaType(), command.content());

    ValidationResult validationResult = limits.validate(upload);
    if (!validationResult.valid()) {
      throw new UploadRejectedException(validationResult.reason());
    }
    // Media-type acceptance isn't a fixed domain list: it's whatever the configured preview
    // adapter can actually turn into a PDF (PDF itself needs no conversion). Each adapter owns its
    // own supported-type set (T-4 attack-surface minimization).
    if (!generatePreview.supports(upload.mediaType())) {
      throw new UploadRejectedException("Unsupported media type: " + upload.mediaType().value());
    }

    String processId = UUID.randomUUID().toString();
    AuthContext auth = authContextPort.current();
    log.info(
        "ingest started processId={} filename={} mediaType={} tenant={}",
        processId,
        upload.filename(),
        upload.mediaType().value(),
        auth.tenantId());
    // Captured on the request thread: the executor's virtual threads don't inherit the
    // ThreadLocal SecurityContext set by DvelopAuthenticationFilter.
    DvelopCredential credential = outboundCredentialPort.current();
    // Hashes the raw upload bytes (not a conversion/preview derivative) so retrieval can recognize
    // a byte-identical re-upload and skip re-embedding it (CHANGES.md duplicate-detection step).
    documentHashStagingPort.stage(processId, Sha256.hex(upload.content()));

    CompletableFuture<BlobRef> blobFuture =
        CompletableFuture.supplyAsync(
            () ->
                documentBlobPort.store(
                    BlobKind.ORIGINAL,
                    upload.mediaType(),
                    upload.content(),
                    processId,
                    auth.tenantId(),
                    auth.userId()),
            executor);

    CompletableFuture<DmsLocation> dmsFuture =
        CompletableFuture.supplyAsync(
            () -> dmsChunkUploadPort.upload(upload.content(), upload.mediaType(), credential),
            executor);

    CompletableFuture<Optional<UUID>> previewFuture =
        blobFuture.thenCompose(
            blob -> {
              log.info("blob stored processId={} blobId={}", processId, blob.blobId());
              processEventPort.publish(
                  new ProcessEvent(
                      processId,
                      ProcessStep.BLOB_STORED,
                      StepStatus.COMPLETED,
                      blob.blobId().toString()));
              return generatePreview.generate(
                  processId, blob.blobId(), upload.mediaType(), auth.tenantId(), auth.userId());
            });

    // Content extraction normally runs off the ORIGINAL blob, independently of preview rendering
    // and the DMS upload, so a slow/failed conversion doesn't delay the 202 response
    // (ADR-004/-008). Each port (text extraction, document content) falls back to the rendered PDF
    // preview independently whenever it doesn't accept the upload's media type (every adapter
    // accepts PDF, per GeneratePreview's contract). Failures are already reported via the
    // TEXT_EXTRACTED/FAILED process event.
    blobFuture
        .thenCompose(
            blob ->
                resolveContentSource(blob, upload.mediaType(), extractText::supports, previewFuture)
                    .thenCombine(
                        resolveContentSource(
                            blob, upload.mediaType(), provideContent::supports, previewFuture),
                        (textSource, contentSource) ->
                            new ContentSources(textSource, contentSource)))
        .thenAcceptAsync(
            sources -> {
              try {
                extractText.extractText(
                    new ContentCommand(
                        processId,
                        sources.text().blobId(),
                        sources.text().mediaType(),
                        auth.tenantId(),
                        auth.userId()));
                provideContent.provide(
                    new ContentCommand(
                        processId,
                        sources.content().blobId(),
                        sources.content().mediaType(),
                        auth.tenantId(),
                        auth.userId()));
              } catch (ContentException ex) {
                log.warn("content extraction failed processId={}", processId, ex);
                return;
              }
              // Runs right after text extraction, on the same virtual thread, so the text staged
              // for this processId is still in the in-memory job context (ADR-006) when
              // retrieval reads it; failures are already reported via the RETRIEVED/FAILED
              // process event.
              try {
                findSimilar.find(processId, retrievalTopK, credential);
              } catch (RetrievalException ex) {
                log.warn("retrieval failed processId={}", processId, ex);
                return;
              }
              // Runs right after retrieval, on the same virtual thread, while the content staged
              // for this processId is still in the in-memory job context (ADR-006 — discarded
              // only once this step returns); failures are already reported via the
              // EXTRACTED/FAILED process event.
              try {
                extractAttributes.extract(processId, credential);
              } catch (ExtractionException ex) {
                log.warn("extraction failed processId={}", processId, ex);
              }
            },
            executor);

    BlobRef originalBlob;
    try {
      originalBlob = blobFuture.join();
    } catch (CompletionException ex) {
      throw unwrap(ex);
    }

    DmsLocation dmsLocation;
    try {
      dmsLocation = dmsFuture.join();
    } catch (CompletionException ex) {
      RuntimeException cause = unwrap(ex);
      log.warn("dms upload failed processId={}", processId, cause);
      rollback(originalBlob, previewFuture);
      processEventPort.publish(
          new ProcessEvent(processId, ProcessStep.DMS_UPLOADED, StepStatus.FAILED, null));
      throw cause;
    }

    log.info("dms upload completed processId={} dmsLocation={}", processId, dmsLocation.uri());
    processEventPort.publish(
        new ProcessEvent(processId, ProcessStep.DMS_UPLOADED, StepStatus.COMPLETED, null));

    auditPort.append(new AuditEvent("ingest.completed", "", 0));

    return new IngestedDocument(
        processId, dmsLocation.uri(), originalBlob.blobId().toString(), upload.mediaType().value());
  }

  /** Best-effort cleanup of already-persisted bytes when the DMS upload fails (C-7). */
  private void rollback(BlobRef originalBlob, CompletableFuture<Optional<UUID>> previewFuture) {
    try {
      previewFuture.join().ifPresent(documentBlobPort::delete);
    } catch (CompletionException ignored) {
      // preview generation failures are already reported via SSE; nothing extra to clean up
    }
    documentBlobPort.delete(originalBlob.blobId());
  }

  /**
   * Resolves the blob/media type a content port should read from: the ORIGINAL blob as-is if the
   * port accepts the upload's media type, otherwise the rendered PDF preview — every content port
   * accepts PDF, per {@link GeneratePreview}'s contract.
   */
  private CompletableFuture<ContentSource> resolveContentSource(
      BlobRef originalBlob,
      MediaType mediaType,
      Predicate<MediaType> supports,
      CompletableFuture<Optional<UUID>> previewFuture) {
    if (supports.test(mediaType)) {
      return CompletableFuture.completedFuture(new ContentSource(originalBlob.blobId(), mediaType));
    }
    return previewFuture.thenApply(
        preview ->
            preview
                .map(previewBlobId -> new ContentSource(previewBlobId, MediaType.PDF))
                // preview rendering failed too: surface the original mismatch as a ContentException
                // rather than silently mislabeling the still-unsupported bytes as PDF.
                .orElseGet(() -> new ContentSource(originalBlob.blobId(), mediaType)));
  }

  private record ContentSource(UUID blobId, MediaType mediaType) {}

  private record ContentSources(ContentSource text, ContentSource content) {}

  private static RuntimeException unwrap(CompletionException ex) {
    Throwable cause = ex.getCause();
    return cause instanceof RuntimeException runtimeException ? runtimeException : ex;
  }
}
