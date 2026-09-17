package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.PageRange;
import java.util.UUID;

/**
 * Inbound use case for the preview endpoint: stream a PDF, Range-fähig, out of the ORIGINAL blob
 * (native PDF) or a lazily rendered PREVIEW blob (non-PDF), per ADR-008. A {@code null} range means
 * the whole blob.
 */
public interface StreamPreview {

  PreviewChunk stream(UUID originalBlobId, PageRange range);
}
