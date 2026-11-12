package ch.adeon.apps.docextract.extraction.application;

import java.util.List;

/**
 * Outcome of the extraction step (FR-4): the id of the document type the document was classified as
 * (from the process's staged candidates), the attribute values for that type's writable properties,
 * and the result of the final DMS check (ADR-016). Results stored before ADR-016 have no {@code
 * validation} and read as {@code NOT_CHECKED}.
 */
public record ExtractionResult(
    String documentTypeId, List<ExtractedAttribute> attributes, ValidationOutcome validation) {

  public static final ExtractionResult EMPTY = new ExtractionResult(null, List.of());

  public ExtractionResult {
    validation = validation == null ? ValidationOutcome.NOT_CHECKED : validation;
  }

  public ExtractionResult(String documentTypeId, List<ExtractedAttribute> attributes) {
    this(documentTypeId, attributes, ValidationOutcome.NOT_CHECKED);
  }
}
