package ch.adeon.apps.docextract.retrieval.domain;

import java.util.Arrays;

/**
 * d.velop's numeric property data type, named so an LLM sees e.g. {@code "DATE"} instead of an
 * opaque {@code 4} when this ends up in its context (T-1, extraction prompt quality). {@code code}
 * is the raw d.velop value, kept for round-tripping to/from the wire. {@code 0}-{@code 5} are
 * documented in dvelop-dmsapp.yaml's ObjDef schema ({@code propertyFields[].dataType}); {@code 7}
 * ({@code property_colorcode}) and {@code 8} ({@code property_doc_type_type}) aren't documented
 * there but were confirmed against a live {@code /o2/{dmsObjectId}/} response. Any other,
 * completely unseen code maps to {@link #UNKNOWN} rather than failing deserialization.
 */
public enum DmsPropertyDataType {
  ALPHANUMERIC(0),
  NUMERIC(1),
  MONEY(2),
  DATETIME(3),
  DATE(4),
  KEY_VALUE_LIST(5),
  COLOR_CODE(7),
  DOCUMENT_TYPE(8),
  UNKNOWN(-1);

  private final int code;

  DmsPropertyDataType(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }

  public static DmsPropertyDataType fromCode(int code) {
    return Arrays.stream(values()).filter(type -> type.code == code).findFirst().orElse(UNKNOWN);
  }
}
