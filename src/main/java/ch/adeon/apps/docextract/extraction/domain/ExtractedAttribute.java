package ch.adeon.apps.docextract.extraction.domain;

import java.util.List;

/**
 * One LLM-suggested value for a writable {@link
 * ch.adeon.apps.docextract.retrieval.domain.DmsPropertyDefinition}, referenced by {@code
 * propertyId} (FR-4). Exactly one of {@code value}/{@code values} is populated, mirroring {@link
 * ch.adeon.apps.docextract.retrieval.domain.DmsProperty}'s single/multi-attribute split. {@code
 * value}/{@code values} are {@code null} when a {@code hasValueList} property's suggestion could
 * not be confirmed against the DMS value list (E-5: reject rather than hallucinate) — {@code
 * confidence} is {@code 0.0} in that case. {@code sourceExcerpt} is the short document passage the
 * suggestion was grounded in, kept for the validation UI (T-1: never itself treated as an
 * instruction).
 */
public record ExtractedAttribute(
    String propertyId,
    String value,
    List<String> values,
    double confidence,
    String sourceExcerpt) {}
