package ch.adeon.apps.docextract.content.port;

import ch.adeon.apps.docextract.content.domain.DocumentContent;
import ch.adeon.apps.docextract.content.domain.Representation;
import ch.adeon.apps.docextract.ingest.domain.MediaType;

/**
 * Outbound port for FR-4's document representation, chosen to match the configured LLM adapter's
 * capabilities (e.g. full text, Markdown, page images). Deliberately separate from {@link
 * TextExtractionPort}: FR-3's similarity search and FR-4's attribute suggestion use independent
 * representations of the same document (SPEC §2).
 */
public interface DocumentContentPort {

  DocumentContent provide(byte[] content, MediaType mediaType);

  boolean supports(MediaType mediaType);

  /** The {@link Representation} this adapter produces, so the configured value can be validated. */
  Representation representation();
}
