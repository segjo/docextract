package ch.adeon.apps.docextract.validation.adapter.out.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
import ch.adeon.apps.docextract.validation.port.PropertyValue;
import ch.adeon.apps.docextract.validation.port.StoreValidationError;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult.Invalid;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class StoreValidateResponseParserTest {

  private static final Set<String> ALLOWLIST = Set.of("11", "4", "13", "14", "15");
  private static final List<PropertyValue> SUBMITTED =
      List.of(new PropertyValue("11", "dawd", null));

  private final JsonMapper json = JsonMapper.builder().build();
  private final StoreValidateResponseParser parser =
      new StoreValidateResponseParser(json, new DmsPropertyPayloadMapper("Europe/Zurich"));

  private static String fixture(String name) throws IOException {
    try (InputStream in =
        StoreValidateResponseParserTest.class.getResourceAsStream("/dms/storevalidate/" + name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private StoreValidationResult parse(int status, String body) {
    return parser.parse(status, body, ALLOWLIST, SUBMITTED);
  }

  @Test
  void ok_is_valid_and_reads_the_properties_through_the_mapper() throws IOException {
    StoreValidationResult result = parse(200, fixture("response-200.json"));

    assertThat(result).isInstanceOf(Valid.class);
    Valid valid = (Valid) result;
    assertThat(valid.submitted()).isEqualTo(SUBMITTED);
    assertThat(valid.returned())
        .containsExactly(
            new PropertyValue("11", "dawd", null),
            new PropertyValue("4", "2026-11-11", null),
            new PropertyValue("13", "12.00", null),
            new PropertyValue("15", "In Prüfung", null));
  }

  @Test
  void ok_with_an_unreadable_body_is_valid_without_adopting_anything() {
    StoreValidationResult result = parse(200, "<html>not json</html>");

    assertThat(result).isEqualTo(new Valid(SUBMITTED, List.of()));
  }

  @Test
  void ok_with_an_empty_body_is_valid_without_adopting_anything() {
    assertThat(parse(200, "")).isEqualTo(new Valid(SUBMITTED, List.of()));
    assertThat(parse(200, null)).isEqualTo(new Valid(SUBMITTED, List.of()));
  }

  @Test
  void bad_request_is_exactly_one_error_with_property_code_and_message() throws IOException {
    StoreValidationResult result = parse(400, fixture("response-400.json"));

    assertThat(result)
        .isEqualTo(
            new Invalid(
                new StoreValidationError(
                    "4",
                    "0010019",
                    "Für mindestens ein Pflichtfeld wurde kein Wert eingegeben. [0010019]"
                        + "  (Rechnungsdatum)")));
  }

  @Test
  void the_property_is_only_reported_if_it_is_on_the_allowlist() {
    Invalid invalid =
        (Invalid)
            parse(
                400,
                "{\"message\":\"Ungültig [0000042]\",\"propertyFieldId\":\"property_filename\"}");

    assertThat(invalid.error())
        .isEqualTo(new StoreValidationError(null, "0000042", "Ungültig [0000042]"));
  }

  @Test
  void the_dms_code_is_the_first_number_in_square_brackets_and_never_mapped() {
    Invalid invalid =
        (Invalid)
            parse(400, "{\"message\":\"Fehler [0099999] siehe [1]\",\"propertyFieldId\":\"11\"}");

    assertThat(invalid.error().dmsCode()).isEqualTo("0099999");
    assertThat(invalid.error().message()).isEqualTo("Fehler [0099999] siehe [1]");
  }

  @Test
  void without_a_code_in_the_message_there_is_no_dms_code() {
    Invalid invalid =
        (Invalid) parse(400, "{\"message\":\"Fehler [abc]\",\"propertyFieldId\":\"11\"}");

    assertThat(invalid.error()).isEqualTo(new StoreValidationError("11", null, "Fehler [abc]"));
  }

  @Test
  void a_missing_property_field_id_gives_no_property() {
    Invalid invalid = (Invalid) parse(400, "{\"message\":\"Fehler [0001]\"}");

    assertThat(invalid.error()).isEqualTo(new StoreValidationError(null, "0001", "Fehler [0001]"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "not json", "[1,2]", "\"text\""})
  void an_unreadable_bad_request_body_is_an_error_without_details(String body) {
    assertThat(parse(400, body)).isEqualTo(new Invalid(new StoreValidationError(null, null, null)));
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403, 404, 409, 422, 500, 502, 503})
  void every_other_status_is_not_a_validation_result(int status) {
    assertThatThrownBy(() -> parse(status, "{\"message\":\"x\"}"))
        .isInstanceOf(AdapterUnavailableException.class)
        .hasMessageContaining(String.valueOf(status))
        .satisfies(
            ex -> assertThat(((AdapterUnavailableException) ex).detail()).doesNotContain("x\""));
  }
}
