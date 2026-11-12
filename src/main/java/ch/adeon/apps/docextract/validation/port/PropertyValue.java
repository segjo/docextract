package ch.adeon.apps.docextract.validation.port;

import java.util.List;

/**
 * The value of one DMS property. Exactly one of {@code value} (single-value property) and {@code
 * values} (multi-value property, order significant) carries content.
 */
public record PropertyValue(String propertyId, String value, List<String> values) {

  /** The value(s) as one ordered list, empty if the property carries none. */
  public List<String> asList() {
    if (values != null) {
      return values;
    }
    return value == null ? List.of() : List.of(value);
  }
}
