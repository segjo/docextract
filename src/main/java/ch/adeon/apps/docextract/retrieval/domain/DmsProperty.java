package ch.adeon.apps.docextract.retrieval.domain;

import java.util.List;

/**
 * One DMS property *value*, sourced from {@code previewState.previewData.documentProperties} —
 * {@code id} references the matching {@link DmsPropertyDefinition} in the owning {@link
 * DmsDocumentType#properties()} (name/dataType/... are looked up there, not duplicated here).
 * Exactly one of {@code value}/{@code values} is set — {@code values} (the full, untruncated list)
 * for multi-attribute properties (see {@link DmsPropertyDefinition#isMultiAttribute()}), taken from
 * the top-level {@code multivalueProperties[].values} map since {@code documentProperties}' own
 * value is truncated there ({@code hasMoreValues}); {@code value} otherwise. Values are kept even
 * when blank/empty — callers decide whether an empty value is relevant, this model doesn't discard
 * it. Empty (no values at all) when sourced from {@code /objdef} — that endpoint has no document
 * instance to read values from, only the type's property definitions.
 */
public record DmsProperty(String id, String value, List<String> values) {}
