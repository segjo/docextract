package ch.adeon.apps.docextract.structuring.application;

import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.structuring.domain.StructuredContent;

/**
 * Outbound port converting raw document bytes into normalized, chunkable text (FR-2). The docling
 * adapter is the primary implementation (ADR-007); a PDFBox-based fast-track adapter is kept
 * available behind the same port for local development/testing without the docling container.
 */
public interface StructuringPort {

  StructuredContent convert(byte[] content, MediaType mediaType);

  /**
   * Whether this adapter can convert the given media type; unsupported types must be rejected
   * upstream or rerouted to the PDF preview instead of being passed to {@link #convert}.
   */
  default boolean supports(MediaType mediaType) {
    return true;
  }
}
