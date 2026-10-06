package ch.adeon.apps.docextract.content.port;

import ch.adeon.apps.docextract.content.domain.ExtractedText;
import ch.adeon.apps.docextract.ingest.domain.MediaType;

/**
 * Outbound port for FR-3's text extraction (ADR-012): a document reduces to plain text of the first
 * {@code maxWords} words, no structure/table reconstruction. Reference adapter: Apache PDFBox
 * (in-process). Every adapter must be classified {@code local}/{@code external} and pass this
 * port's contract test suite (NfA-8, C-8).
 */
public interface TextExtractionPort {

  ExtractedText extract(byte[] content, MediaType mediaType, int maxWords);

  boolean supports(MediaType mediaType);
}
