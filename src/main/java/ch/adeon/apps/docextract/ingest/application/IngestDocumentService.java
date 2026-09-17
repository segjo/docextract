package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.DmsLocation;
import ch.adeon.apps.docextract.ingest.domain.DocumentUpload;
import ch.adeon.apps.docextract.ingest.domain.IngestCommand;
import ch.adeon.apps.docextract.ingest.domain.IngestedDocument;
import ch.adeon.apps.docextract.ingest.domain.Limits;
import ch.adeon.apps.docextract.ingest.domain.ValidationResult;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

  private final DocumentBlobPort documentBlobPort;
  private final DmsChunkUploadPort dmsChunkUploadPort;
  private final AuthContextPort authContextPort;
  private final OutboundCredentialPort outboundCredentialPort;
  private final AuditPort auditPort;
  private final ProcessEventPort processEventPort;
  private final GeneratePreview generatePreview;
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
      @Value("${docextract.ingest.max-file-size-bytes:26214400}") long maxFileSizeBytes) {
    this.documentBlobPort = documentBlobPort;
    this.dmsChunkUploadPort = dmsChunkUploadPort;
    this.authContextPort = authContextPort;
    this.outboundCredentialPort = outboundCredentialPort;
    this.auditPort = auditPort;
    this.processEventPort = processEventPort;
    this.generatePreview = generatePreview;
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

    String processId = UUID.randomUUID().toString();
    AuthContext auth = authContextPort.current();
    // Captured on the request thread: the executor's virtual threads don't inherit the
    // ThreadLocal SecurityContext set by DvelopAuthenticationFilter.
    DvelopCredential credential = outboundCredentialPort.current();

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
              processEventPort.publish(
                  new ProcessEvent(
                      processId,
                      ProcessStep.BLOB_STORED,
                      StepStatus.COMPLETED,
                      blob.blobId().toString()));
              return generatePreview.generate(
                  processId, blob.blobId(), upload.mediaType(), auth.tenantId(), auth.userId());
            });

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
      rollback(originalBlob, previewFuture);
      processEventPort.publish(
          new ProcessEvent(processId, ProcessStep.DMS_UPLOADED, StepStatus.FAILED, null));
      throw unwrap(ex);
    }

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

  private static RuntimeException unwrap(CompletionException ex) {
    Throwable cause = ex.getCause();
    return cause instanceof RuntimeException runtimeException ? runtimeException : ex;
  }
}
