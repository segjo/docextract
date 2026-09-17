package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Preview-Branch (ADR-008): native PDFs are streamed directly from the ORIGINAL blob; non-PDFs are
 * rendered once via Gotenberg into a PREVIEW blob and streamed from there. No DMS read-back is
 * involved, since the DMS chunk upload is write-only. Access is limited to the tenant and user who
 * own the blob (NfA-4); a mismatch is reported as {@link BlobNotFoundException} to avoid revealing
 * that a blob exists for a different tenant/user.
 */
@Service
public class PreviewStreamService implements StreamPreview {

  private final DocumentBlobPort documentBlobPort;
  private final PreviewRenderPort previewRenderPort;
  private final AuthContextPort authContextPort;

  public PreviewStreamService(
      DocumentBlobPort documentBlobPort,
      PreviewRenderPort previewRenderPort,
      AuthContextPort authContextPort) {
    this.documentBlobPort = documentBlobPort;
    this.previewRenderPort = previewRenderPort;
    this.authContextPort = authContextPort;
  }

  @Override
  public PreviewChunk stream(UUID originalBlobId, PageRange range) {
    BlobRef original = documentBlobPort.describe(originalBlobId);
    requireOwnership(originalBlobId, original);

    BlobRef source = original.mediaType().isPdf() ? original : renderPreview(original);

    PageRange effectiveRange = range != null ? range : PageRange.full(source.sizeBytes());
    byte[] content = documentBlobPort.readRange(source.blobId(), effectiveRange);
    return new PreviewChunk(content, source.sizeBytes());
  }

  private void requireOwnership(UUID blobId, BlobRef blob) {
    AuthContext auth = authContextPort.current();
    if (!Objects.equals(auth.tenantId(), blob.tenantId())
        || !Objects.equals(auth.userId(), blob.userId())) {
      throw new BlobNotFoundException(blobId);
    }
  }

  private BlobRef renderPreview(BlobRef original) {
    byte[] originalBytes =
        documentBlobPort.readRange(original.blobId(), PageRange.full(original.sizeBytes()));
    byte[] pdf =
        previewRenderPort.renderPreview(
            original.blobId().toString(), original.mediaType(), originalBytes);
    return documentBlobPort.store(
        BlobKind.PREVIEW,
        MediaType.PDF,
        pdf,
        original.processId(),
        original.tenantId(),
        original.userId());
  }
}
