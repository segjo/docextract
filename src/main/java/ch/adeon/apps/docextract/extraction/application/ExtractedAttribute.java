package ch.adeon.apps.docextract.extraction.application;

import ch.adeon.apps.docextract.validation.application.AttributeOrigin;
import java.util.List;

/**
 * One value for a writable {@code DmsPropertyDefinition}, referenced by {@code propertyId} (FR-4).
 * Exactly one of {@code value}/{@code values} is populated, mirroring {@code DmsProperty}'s
 * single/multi-attribute split. {@code value}/{@code values} are {@code null} when a {@code
 * hasValueList} property's suggestion could not be confirmed against the DMS value list (E-5:
 * reject rather than hallucinate) — {@code confidence} is {@code 0.0} in that case. {@code
 * confidence} is on a 0–1 scale and {@code null} for {@code origin = DMS}. {@code sourceExcerpt} is
 * the short document passage the suggestion was grounded in, kept for the validation UI (T-1:
 * never itself treated as an instruction).
 *
 * <p>ADR-016: {@code origin} says where the value comes from ({@code DMS} = set by a hook or
 * default of the DMS, no excerpt), {@code changedByDms} that the DMS set or changed it, {@code
 * correctedAfterValidation} that the LLM changed it after the DMS rejected an earlier value (its
 * confidence is capped). Results stored before ADR-016 read as {@code origin = LLM}, both flags
 * {@code false}.
 */
public record ExtractedAttribute(
    String propertyId,
    String value,
    List<String> values,
    Double confidence,
    String sourceExcerpt,
    AttributeOrigin origin,
    boolean changedByDms,
    boolean correctedAfterValidation) {

  public ExtractedAttribute {
    origin = origin == null ? AttributeOrigin.LLM : origin;
  }

  /** A suggestion of the LLM that the DMS did not touch. */
  public ExtractedAttribute(
      String propertyId,
      String value,
      List<String> values,
      double confidence,
      String sourceExcerpt) {
    this(propertyId, value, values, confidence, sourceExcerpt, AttributeOrigin.LLM, false, false);
  }
}
