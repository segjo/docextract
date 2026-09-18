package ch.adeon.apps.docextract.structuring.application;

import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.structuring.domain.StructuredDocument;
import ch.adeon.apps.docextract.structuring.domain.StructuringCommand;

/** Inbound port: converts + chunks a document already held in the ingest blobstore (FR-2). */
public interface StructureDocument {

  StructuredDocument structure(StructuringCommand command);

  /** Whether the configured {@link StructuringPort} can convert the given media type. */
  boolean supports(MediaType mediaType);
}
