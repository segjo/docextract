package ch.adeon.apps.docextract.validation.adapter.out.dms;

import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import ch.adeon.apps.docextract.validation.port.PropertyValue;
import ch.adeon.apps.docextract.validation.port.StoreValidationRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The one place that speaks the DMS property format for {@code DmsWriteAdapter} ({@code o2m}) and
 * {@code DmsStoreValidationAdapter} ({@code storevalidate}), so the check validates the payload
 * that is later written (ADR-016, decision 2). Also reads a {@code storevalidate} response back
 * into properties (decision 6): only allowlisted, non-empty extended properties, never system
 * properties or remarks.
 */
@Component
public class DmsPropertyPayloadMapper {

  static final int TYPE_DOCUMENT = 1;
  static final String UNKNOWN = "unbekannt";

  private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  // xxx, not XXX: a zero offset is "+00:00", never "Z"; no fraction of a second.
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
  private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("d.M.uuuu");
  private static final DateTimeFormatter GERMAN_DATE_TIME =
      DateTimeFormatter.ofPattern("d.M.uuuu H:mm[:ss]");
  private static final Pattern NUMBER = Pattern.compile("-?\\d(?:[\\d.,'’\\u00a0 ]*\\d)?");

  private final ZoneId defaultZone;

  public DmsPropertyPayloadMapper(
      @Value("${docextract.validation.default-zone:Europe/Zurich}") String defaultZone) {
    this.defaultZone = ZoneId.of(defaultZone);
  }

  /** Write ({@code o2m}) mapping, moved unchanged out of {@code DmsWriteAdapter}. */
  public List<SourceProperty> toSourceProperties(Map<String, Object> attributes) {
    List<SourceProperty> properties = new ArrayList<>();
    attributes.forEach((key, value) -> properties.add(new SourceProperty(key, toValues(value))));
    return properties;
  }

  private static List<String> toValues(Object value) {
    if (value instanceof List<?> list) {
      return list.stream().map(String::valueOf).toList();
    }
    return List.of(String.valueOf(value));
  }

  /** Request body of {@code storevalidate} plus the values as actually sent. */
  public Prepared toStorePayload(StoreValidationRequest request) {
    Map<String, String> extended = new LinkedHashMap<>();
    Map<String, Map<String, String>> multivalue = new LinkedHashMap<>();
    Map<String, PropertyValue> submitted = new LinkedHashMap<>();
    for (PropertyValue property : request.properties()) {
      DmsPropertyDefinition definition = request.definitions().get(property.propertyId());
      DmsPropertyDataType dataType =
          definition == null ? DmsPropertyDataType.ALPHANUMERIC : definition.dataType();
      boolean multiValue =
          definition == null ? property.values() != null : definition.isMultiAttribute();
      List<String> values =
          property.asList().stream()
              .filter(value -> !isUnset(value))
              .map(value -> format(value, dataType))
              .toList();
      if (values.isEmpty()) {
        continue;
      }
      if (multiValue) {
        multivalue.put(property.propertyId(), indexed(values));
        submitted.put(
            property.propertyId(), new PropertyValue(property.propertyId(), null, values));
      } else {
        extended.put(property.propertyId(), values.get(0));
        submitted.put(
            property.propertyId(), new PropertyValue(property.propertyId(), values.get(0), null));
      }
    }
    StoreValidatePayload payload =
        new StoreValidatePayload(
            TYPE_DOCUMENT,
            request.objectDefinitionId(),
            systemProperties(request.filename()),
            Map.of(),
            multivalue,
            extended);
    return new Prepared(payload, List.copyOf(submitted.values()));
  }

  /** Reads the properties the DMS would store out of a {@code storevalidate} 200 response. */
  public List<PropertyValue> fromResponse(StoreValidatePayload response, Set<String> allowlist) {
    List<PropertyValue> properties = new ArrayList<>();
    if (response.extendedProperties() != null) {
      response
          .extendedProperties()
          .forEach((id, value) -> {
            if (allowlist.contains(id) && !isEmpty(value)) {
              properties.add(new PropertyValue(id, value, null));
            }
          });
    }
    if (response.multivalueExtendedProperties() != null) {
      response
          .multivalueExtendedProperties()
          .forEach((id, indexed) -> {
            if (!allowlist.contains(id) || indexed == null) {
              return;
            }
            List<String> values =
                indexed.entrySet().stream()
                    .sorted(Comparator.comparingInt(entry -> index(entry.getKey())))
                    .map(Map.Entry::getValue)
                    .filter(value -> !isEmpty(value))
                    .toList();
            if (!values.isEmpty()) {
              properties.add(new PropertyValue(id, null, values));
            }
          });
    }
    return properties;
  }

  private static Map<String, Object> systemProperties(String filename) {
    Map<String, Object> system = new LinkedHashMap<>();
    if (filename != null && !filename.isBlank()) {
      system.put("property_filename", filename);
      int dot = filename.lastIndexOf('.');
      if (dot >= 0 && dot < filename.length() - 1) {
        system.put("property_filetype", filename.substring(dot + 1));
      }
    }
    return system;
  }

  // 1-based indices as strings, order kept.
  private static Map<String, String> indexed(List<String> values) {
    Map<String, String> indexed = new LinkedHashMap<>();
    for (int i = 0; i < values.size(); i++) {
      indexed.put(String.valueOf(i + 1), values.get(i));
    }
    return indexed;
  }

  private static int index(String key) {
    try {
      return Integer.parseInt(key);
    } catch (NumberFormatException ex) {
      return Integer.MAX_VALUE;
    }
  }

  private static boolean isUnset(String value) {
    return isEmpty(value) || UNKNOWN.equalsIgnoreCase(value.trim());
  }

  private static boolean isEmpty(String value) {
    return value == null || value.isBlank();
  }

  private String format(String value, DmsPropertyDataType dataType) {
    return switch (dataType) {
      case DATE -> formatDate(value);
      case DATETIME -> formatDateTime(value);
      case NUMERIC, MONEY -> formatNumber(value);
      default -> value;
    };
  }

  // Unparsable values are sent as they are: the DMS then names the problem (400).
  private static String formatDate(String value) {
    String text = value.trim();
    try {
      return LocalDate.parse(text, DATE).format(DATE);
    } catch (DateTimeParseException ignored) {
      // next format
    }
    try {
      return LocalDate.parse(text, GERMAN_DATE).format(DATE);
    } catch (DateTimeParseException ignored) {
      // next format
    }
    try {
      return OffsetDateTime.parse(text).toLocalDate().format(DATE);
    } catch (DateTimeParseException ignored) {
      // next format
    }
    try {
      return LocalDateTime.parse(text).toLocalDate().format(DATE);
    } catch (DateTimeParseException ignored) {
      return value;
    }
  }

  private String formatDateTime(String value) {
    String text = value.trim();
    try {
      return OffsetDateTime.parse(text).format(DATE_TIME);
    } catch (DateTimeParseException ignored) {
      // next format
    }
    try {
      return ZonedDateTime.parse(text).toOffsetDateTime().format(DATE_TIME);
    } catch (DateTimeParseException ignored) {
      // next format
    }
    // No zone given: extracted values are taken as local time of the default zone.
    try {
      return LocalDateTime.parse(text).atZone(defaultZone).toOffsetDateTime().format(DATE_TIME);
    } catch (DateTimeParseException ignored) {
      // next format
    }
    try {
      return LocalDateTime.parse(text, GERMAN_DATE_TIME)
          .atZone(defaultZone)
          .toOffsetDateTime()
          .format(DATE_TIME);
    } catch (DateTimeParseException ignored) {
      // next format
    }
    try {
      return LocalDate.parse(text, DATE)
          .atStartOfDay(defaultZone)
          .toOffsetDateTime()
          .format(DATE_TIME);
    } catch (DateTimeParseException ignored) {
      return value;
    }
  }

  /** Decimal point, no thousands separator, no currency or unit. */
  private static String formatNumber(String value) {
    Matcher matcher = NUMBER.matcher(value);
    return matcher.find() ? canonicalNumber(matcher.group()) : value;
  }

  /** "8,00" -> "8.00", "1'000.20" -> "1000.20", "1.000,20" -> "1000.20". */
  private static String canonicalNumber(String raw) {
    String number = raw.replaceAll("['’\\u00a0 ]", "");
    int lastComma = number.lastIndexOf(',');
    int lastDot = number.lastIndexOf('.');
    int decimalAt = Math.max(lastComma, lastDot);
    if (decimalAt < 0) {
      return number;
    }
    boolean otherSeparatorBefore =
        number.substring(0, decimalAt).contains(lastComma > lastDot ? "." : ",");
    boolean repeated = number.indexOf(number.charAt(decimalAt)) != decimalAt;
    if (repeated && !otherSeparatorBefore) {
      return number.replaceAll("[.,]", "");
    }
    return number.substring(0, decimalAt).replaceAll("[.,]", "")
        + "."
        + number.substring(decimalAt + 1);
  }

  /** Property of the {@code o2m} write payload. */
  public record SourceProperty(String key, List<String> values) {}

  /** Wire format of {@code storevalidate}; the 200 response has no {@code type}, hence boxed. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StoreValidatePayload(
      Integer type,
      String objectDefinitionId,
      Map<String, Object> systemProperties,
      Map<String, Object> remarks,
      Map<String, Map<String, String>> multivalueExtendedProperties,
      Map<String, String> extendedProperties) {}

  /** The request body and the values it contains after normalisation. */
  public record Prepared(StoreValidatePayload payload, List<PropertyValue> submitted) {}
}
