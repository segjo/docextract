package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.MediaType;

/**
 * Outbound port rendering a non-PDF upload (read from the ORIGINAL blob) into a PDF preview
 * (Gotenberg/LibreOffice), stored as the PREVIEW blob (ADR-008).
 */
public interface PreviewRenderPort {

  byte[] renderPreview(String filename, MediaType mediaType, byte[] content);

  /** Whether this adapter can render a PDF preview for the given media type. */
  default boolean supports(MediaType mediaType) {
    return true;
  }
}
