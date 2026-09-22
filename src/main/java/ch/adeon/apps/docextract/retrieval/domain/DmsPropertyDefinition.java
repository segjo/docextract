package ch.adeon.apps.docextract.retrieval.domain;

import java.util.List;

/**
 * The definition of one property of a {@link DmsDocumentType} — id/name plus everything needed to
 * interpret a value for it (mirrors the raw d.velop property {@code definition} 1:1). {@code
 * isMultiAttribute} tells a caller whether the corresponding {@link DmsProperty} for this id
 * carries its value in {@code value} or {@code values}. {@code valueList} is only populated when
 * {@code hasValueList} is true (empty otherwise, and also when the webhook returned no values yet
 * because this property depends on another one not filled in at fetch time — SPEC §3).
 */
public record DmsPropertyDefinition(
    String id,
    String name,
    DmsPropertyDataType dataType,
    boolean mandatory,
    boolean hasValueList,
    boolean isDynamicValueList,
    boolean isHidden,
    boolean isModifiable,
    boolean isSystemProperty,
    boolean isMultiAttribute,
    boolean isDocumentTypeProperty,
    int maxRowCount,
    int maxLength,
    List<String> valueList,
    boolean hasMoreValues) {}
