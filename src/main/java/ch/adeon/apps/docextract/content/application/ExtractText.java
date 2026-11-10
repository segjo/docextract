package ch.adeon.apps.docextract.content.application;

import ch.adeon.apps.docextract.content.port.ExtractedText;
import ch.adeon.apps.docextract.shared.media.MediaType;

/** Inbound port: extracts the first-N-words plain text of a document for FR-3 (ADR-012). */
public interface ExtractText {

  ExtractedText extractText(ContentCommand command);

  /**
   * Whether the configured {@link ch.adeon.apps.docextract.content.port.TextExtractionPort} can
   * convert the given media type.
   */
  boolean supports(MediaType mediaType);
}
