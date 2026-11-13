package ch.adeon.apps.docextract.validation.adapter.out.dms;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import ch.adeon.apps.docextract.validation.adapter.out.dms.DmsPropertyPayloadMapper.Prepared;
import ch.adeon.apps.docextract.validation.adapter.out.dms.DmsPropertyPayloadMapper.SourceProperty;
import ch.adeon.apps.docextract.validation.adapter.out.dms.DmsPropertyPayloadMapper.StoreValidatePayload;
import ch.adeon.apps.docextract.validation.port.PropertyValue;
import ch.adeon.apps.docextract.validation.port.StoreValidationRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DmsPropertyPayloadMapperTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final DmsPropertyPayloadMapper mapper = new DmsPropertyPayloadMapper("Europe/Zurich");

  private static DmsPropertyDefinition definition(
      String id, DmsPropertyDataType type, boolean multi) {
    return new DmsPropertyDefinition(
        id, id, type, false, false, false, false, true, false, multi, false, 0, 0, List.of(),
        false);
  }

  private static PropertyValue single(String id, String value) {
    return new PropertyValue(id, value, null);
  }

  private Prepared prepare(
      String filename, List<PropertyValue> properties, DmsPropertyDefinition... definitions) {
    Map<String, DmsPropertyDefinition> byId = new LinkedHashMap<>();
    for (DmsPropertyDefinition definition : definitions) {
      byId.put(definition.id(), definition);
    }
    return mapper.toStorePayload(
        new StoreValidationRequest("repo", "DKOR", filename, properties, byId));
  }

  private static JsonNode fixture(String name) throws IOException {
    try (InputStream in =
        DmsPropertyPayloadMapperTest.class.getResourceAsStream("/dms/storevalidate/" + name)) {
      return JSON.readTree(in);
    }
  }

  private String sent(DmsPropertyDataType type, String value) {
    Prepared prepared = prepare("a.pdf", List.of(single("p", value)), definition("p", type, false));
    return prepared.payload().extendedProperties().get("p");
  }

  @Test
  void builds_exactly_the_documented_request() throws IOException {
    Prepared prepared =
        prepare(
            "jstore-sidecar.pdf",
            List.of(
                single("22", "test"),
                single("23", "test"),
                single("25", "2026-11-10"),
                single("27", "Finanzen"),
                new PropertyValue("28", null, List.of("test a", "test b"))),
            definition("22", DmsPropertyDataType.ALPHANUMERIC, false),
            definition("23", DmsPropertyDataType.ALPHANUMERIC, false),
            definition("25", DmsPropertyDataType.DATE, false),
            definition("27", DmsPropertyDataType.ALPHANUMERIC, false),
            definition("28", DmsPropertyDataType.ALPHANUMERIC, true));

    JsonNode actual = JSON.valueToTree(prepared.payload());
    assertThat(actual).isEqualTo(fixture("request.json"));
  }

  @Test
  void single_values_go_to_extended_and_multi_values_to_multivalue_with_indices_from_1() {
    Prepared prepared =
        prepare(
            "a.pdf",
            List.of(single("1", "x"), new PropertyValue("2", null, List.of("a", "b", "c"))),
            definition("1", DmsPropertyDataType.ALPHANUMERIC, false),
            definition("2", DmsPropertyDataType.ALPHANUMERIC, true));

    assertThat(prepared.payload().extendedProperties()).containsExactly(Map.entry("1", "x"));
    assertThat(prepared.payload().multivalueExtendedProperties().get("2"))
        .containsExactly(Map.entry("1", "a"), Map.entry("2", "b"), Map.entry("3", "c"));
  }

  @Test
  void a_multi_property_with_one_value_stays_multivalue() {
    Prepared prepared =
        prepare(
            "a.pdf",
            List.of(single("2", "only")),
            definition("2", DmsPropertyDataType.ALPHANUMERIC, true));

    assertThat(prepared.payload().extendedProperties()).isEmpty();
    assertThat(prepared.payload().multivalueExtendedProperties().get("2"))
        .containsExactly(Map.entry("1", "only"));
  }

  @Test
  void unknown_and_empty_values_are_left_out() {
    Prepared prepared =
        prepare(
            "a.pdf",
            List.of(
                single("1", "unbekannt"),
                single("2", "  "),
                single("3", null),
                single("4", "Unbekannt"),
                new PropertyValue("5", null, List.of("", "unbekannt")),
                new PropertyValue("6", null, List.of("a", "", "b")),
                single("7", "kept")),
            definition("1", DmsPropertyDataType.ALPHANUMERIC, false),
            definition("2", DmsPropertyDataType.ALPHANUMERIC, false),
            definition("3", DmsPropertyDataType.ALPHANUMERIC, false),
            definition("4", DmsPropertyDataType.ALPHANUMERIC, false),
            definition("5", DmsPropertyDataType.ALPHANUMERIC, true),
            definition("6", DmsPropertyDataType.ALPHANUMERIC, true),
            definition("7", DmsPropertyDataType.ALPHANUMERIC, false));

    assertThat(prepared.payload().extendedProperties()).containsExactly(Map.entry("7", "kept"));
    // blank entries drop out, the indices stay consecutive
    assertThat(prepared.payload().multivalueExtendedProperties())
        .containsOnlyKeys("6")
        .extractingByKey("6")
        .isEqualTo(Map.of("1", "a", "2", "b"));
    assertThat(prepared.submitted())
        .extracting(PropertyValue::propertyId)
        .containsExactly("6", "7");
  }

  @Test
  void type_is_1_remarks_are_empty_and_file_name_and_type_come_from_the_process_document() {
    Prepared prepared = prepare("Rechnung 2026.final.PDF", List.of());

    assertThat(prepared.payload().type()).isEqualTo(1);
    assertThat(prepared.payload().remarks()).isEmpty();
    assertThat(prepared.payload().objectDefinitionId()).isEqualTo("DKOR");
    assertThat(prepared.payload().systemProperties())
        .containsEntry("property_filename", "Rechnung 2026.final.PDF")
        .containsEntry("property_filetype", "PDF")
        .hasSize(2);
  }

  @Test
  void without_a_file_name_or_extension_no_system_properties_are_sent() {
    assertThat(prepare(null, List.of()).payload().systemProperties()).isEmpty();
    assertThat(prepare("README", List.of()).payload().systemProperties())
        .containsOnlyKeys("property_filename");
  }

  @Test
  void date_is_sent_as_yyyy_MM_dd() {
    assertThat(sent(DmsPropertyDataType.DATE, "2026-11-10")).isEqualTo("2026-11-10");
    assertThat(sent(DmsPropertyDataType.DATE, "10.11.2026")).isEqualTo("2026-11-10");
    assertThat(sent(DmsPropertyDataType.DATE, "2026-11-10T08:02:09+01:00")).isEqualTo("2026-11-10");
    // not parsable: sent as it is, the DMS names the problem
    assertThat(sent(DmsPropertyDataType.DATE, "morgen")).isEqualTo("morgen");
  }

  @Test
  void datetime_has_a_numeric_offset_never_Z_and_no_fraction_of_a_second() {
    assertThat(sent(DmsPropertyDataType.DATETIME, "2026-11-11T08:02:09Z"))
        .isEqualTo("2026-11-11T08:02:09+00:00");
    assertThat(sent(DmsPropertyDataType.DATETIME, "2026-11-11T08:02:09.123456+00:00"))
        .isEqualTo("2026-11-11T08:02:09+00:00");
    assertThat(sent(DmsPropertyDataType.DATETIME, "2026-11-11T08:02:09+05:30"))
        .isEqualTo("2026-11-11T08:02:09+05:30");
    assertThat(sent(DmsPropertyDataType.DATETIME, "2026-11-11T08:02:09Z")).doesNotContain("Z");
  }

  @Test
  void datetime_without_a_zone_is_taken_as_europe_zurich() {
    assertThat(sent(DmsPropertyDataType.DATETIME, "2026-11-11T08:02:09"))
        .isEqualTo("2026-11-11T08:02:09+01:00");
    assertThat(sent(DmsPropertyDataType.DATETIME, "2026-07-01T10:00:00"))
        .isEqualTo("2026-07-01T10:00:00+02:00");
    assertThat(sent(DmsPropertyDataType.DATETIME, "11.11.2026 08:02"))
        .isEqualTo("2026-11-11T08:02:00+01:00");
    assertThat(sent(DmsPropertyDataType.DATETIME, "2026-11-11"))
        .isEqualTo("2026-11-11T00:00:00+01:00");
  }

  @Test
  void numeric_and_currency_use_a_decimal_point_and_no_thousands_separator() {
    for (DmsPropertyDataType type :
        List.of(DmsPropertyDataType.NUMERIC, DmsPropertyDataType.MONEY)) {
      assertThat(sent(type, "12,5")).as(type + " comma").isEqualTo("12.5");
      assertThat(sent(type, "1'234,50")).as(type + " apostrophe").isEqualTo("1234.50");
      assertThat(sent(type, "1.234,50")).as(type + " german").isEqualTo("1234.50");
      assertThat(sent(type, "1,234.50")).as(type + " english").isEqualTo("1234.50");
      assertThat(sent(type, "1,234,567")).as(type + " repeated").isEqualTo("1234567");
      assertThat(sent(type, "12.00")).as(type + " unchanged").isEqualTo("12.00");
      assertThat(sent(type, "CHF 99,95")).as(type + " currency").isEqualTo("99.95");
    }
  }

  @Test
  void strings_are_sent_unchanged() {
    assertThat(sent(DmsPropertyDataType.ALPHANUMERIC, " 12,5 CHF ")).isEqualTo(" 12,5 CHF ");
  }

  @Test
  void the_submitted_values_are_the_normalised_ones() {
    Prepared prepared =
        prepare(
            "a.pdf",
            List.of(single("p", "12,5")),
            definition("p", DmsPropertyDataType.NUMERIC, false));

    assertThat(prepared.submitted()).containsExactly(single("p", "12.5"));
  }

  @Test
  void write_mapping_is_the_one_moved_out_of_the_write_adapter() {
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("1", "x");
    attributes.put("2", List.of("a", "b"));
    attributes.put("3", 5);

    assertThat(mapper.toSourceProperties(attributes))
        .containsExactly(
            new SourceProperty("1", List.of("x")),
            new SourceProperty("2", List.of("a", "b")),
            new SourceProperty("3", List.of("5")));
  }

  @Test
  void reads_only_allowlisted_non_empty_extended_properties_from_the_response() throws IOException {
    StoreValidatePayload response =
        JSON.treeToValue(fixture("response-200.json"), StoreValidatePayload.class);

    List<PropertyValue> properties = mapper.fromResponse(response, Set.of("11", "4", "13", "14"));

    // "15" is not in the allowlist, "14" is empty, system properties and remarks are ignored
    assertThat(properties)
        .containsExactly(single("11", "dawd"), single("4", "2026-11-11"), single("13", "12.00"));
  }

  @Test
  void reads_multi_values_in_index_order_and_skips_empty_ones() {
    Map<String, String> indexed = new LinkedHashMap<>();
    indexed.put("10", "tenth");
    indexed.put("2", "second");
    indexed.put("1", "first");
    indexed.put("3", "");
    StoreValidatePayload response =
        new StoreValidatePayload(
            1, "DKOR", Map.of(), Map.of(), Map.of("28", indexed, "29", Map.of("1", "x")), Map.of());

    assertThat(mapper.fromResponse(response, Set.of("28")))
        .containsExactly(new PropertyValue("28", null, List.of("first", "second", "tenth")));
  }

  @Test
  void a_response_without_properties_yields_nothing() {
    assertThat(
            mapper.fromResponse(
                new StoreValidatePayload(0, null, null, null, null, null), Set.of("1")))
        .isEmpty();
  }
}
