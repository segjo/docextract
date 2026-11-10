package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.port.BlobRef;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.ingest.port.PageRange;
import ch.adeon.apps.docextract.ingest.port.PreviewRenderPort;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.shared.media.MediaType;
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
    AuthContext auth = authContextPort.current();
    BlobRef original = documentBlobPort.describe(auth.tenantId(), originalBlobId);
    // The port filters by tenant; a blob additionally belongs to the user who uploaded it.
    if (!Objects.equals(auth.userId(), original.userId())) {
      throw new BlobNotFoundException(originalBlobId);
    }

    BlobRef source = original.mediaType().isPdf() ? original : renderPreview(original);

    PageRange effectiveRange = range != null ? range : PageRange.full(source.sizeBytes());
    byte[] content = documentBlobPort.readRange(auth.tenantId(), source.blobId(), effectiveRange);
    return new PreviewChunk(content, source.sizeBytes());
  }

  private BlobRef renderPreview(BlobRef original) {
    byte[] originalBytes =
        documentBlobPort.readRange(
            original.tenantId(), original.blobId(), PageRange.full(original.sizeBytes()));
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
