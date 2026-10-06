package ch.adeon.apps.docextract.content.application;

import ch.adeon.apps.docextract.content.domain.ContentCommand;
import ch.adeon.apps.docextract.content.domain.DocumentContent;
import ch.adeon.apps.docextract.ingest.domain.MediaType;

/** Inbound port: provides the document representation used for FR-4 attribute suggestions. */
public interface ProvideContent {

  DocumentContent provide(ContentCommand command);

  /**
   * Whether the configured {@link ch.adeon.apps.docextract.content.port.DocumentContentPort} can
   * convert the given media type.
   */
  boolean supports(MediaType mediaType);
}
