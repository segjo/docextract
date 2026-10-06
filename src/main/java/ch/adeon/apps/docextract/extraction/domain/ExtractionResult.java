package ch.adeon.apps.docextract.extraction.domain;

import java.util.List;

/**
 * Outcome of the extraction step (FR-4): the id of the document type the document was classified as
 * (from the process's staged candidates), and the LLM's attribute-value suggestions for that type's
 * writable properties.
 */
public record ExtractionResult(String documentTypeId, List<ExtractedAttribute> attributes) {

  public static final ExtractionResult EMPTY = new ExtractionResult(null, List.of());
}
